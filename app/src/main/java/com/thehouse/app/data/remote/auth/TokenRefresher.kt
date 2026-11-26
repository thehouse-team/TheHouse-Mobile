package com.thehouse.app.data.remote.auth

import com.thehouse.app.data.auth.session.InMemorySessionStore
import com.thehouse.app.data.auth.storage.SessionTokenStorage
import com.thehouse.app.data.remote.dto.auth.AuthResponseDto
import com.thehouse.app.data.remote.dto.visits.ProblemDetailsDto
import com.thehouse.app.data.serialization.JsonProvider
import com.thehouse.app.domain.model.SessionState
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * D19: refresh single-flight reutilizable por [RefreshAuthenticator] y por
 * [BearerTokenInterceptor] cuando una sesión activa no tiene access token.
 *
 * - Un solo refresh en vuelo (monitor JVM).
 * - Reintento máximo UNO por request original: el llamador decide.
 * - 200: actualiza access token y rota el refresh token cifrado.
 * - 401: limpia sesión y storage → Anonymous.
 * - 403 `account_suspended`: suspende la cuenta.
 * - Nunca registra tokens, bodies ni mensajes sensibles.
 */
class TokenRefresher(
    private val baseUrl: String,
    private val sessionStore: InMemorySessionStore,
    private val refreshTokenStorage: SessionTokenStorage,
    private val refreshClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .callTimeout(15, TimeUnit.SECONDS)
        .build(),
) {

    private val lock = Any()
    private val json: Json = JsonProvider.create()

    /**
     * Fuerza un refresh (lo usa el Authenticator tras un 401 con token stale).
     * Devuelve el nuevo access token, o null si falló definitivamente.
     */
    fun refreshNow(): String? = synchronized(lock) {
        if (sessionStore.state.value is SessionState.Suspended) return null
        doRefreshBlocking()
    }

    /**
     * Solo refresca si la sesión está activa pero NO hay access token
     * (lo usa el BearerTokenInterceptor antes de enviar la request protegida).
     */
    fun refreshIfNeeded(): String? = synchronized(lock) {
        if (sessionStore.state.value !is SessionState.Active) return null
        if (!sessionStore.accessToken.isNullOrBlank()) return sessionStore.accessToken
        doRefreshBlocking()
    }

    private fun doRefreshBlocking(): String? {
        val refreshToken = runCatching { runBlocking { refreshTokenStorage.readRefreshToken() } }
            .getOrNull()
            ?.toString(Charsets.UTF_8)
        if (refreshToken.isNullOrBlank()) {
            sessionStore.clear()
            runCatching { runBlocking { refreshTokenStorage.clear() } }
            return null
        }
        val normalizedBase = baseUrl.trimEnd('/') + "/"
        val bodyJson = json.encodeToString(
            kotlinx.serialization.serializer(),
            mapOf("refreshToken" to refreshToken),
        )
        val request = Request.Builder()
            .url(normalizedBase + "api/v1/auth/refresh")
            .header("Cache-Control", "no-store, no-cache")
            .post(bodyJson.toRequestBody("application/json".toMediaType()))
            .build()
        return try {
            refreshClient.newCall(request).execute().use { resp ->
                when (resp.code) {
                    200 -> {
                        if (sessionStore.state.value is SessionState.Suspended) return null
                        val dto = json.decodeFromString(
                            AuthResponseDto.serializer(),
                            resp.body?.string().orEmpty(),
                        )
                        val newAccess = dto.accessToken ?: dto.token
                        if (newAccess.isNullOrBlank()) return null
                        sessionStore.updateAccessToken(newAccess)
                        val newRefresh = dto.refreshToken
                        if (newRefresh != null) {
                            runCatching {
                                runBlocking {
                                    refreshTokenStorage.writeRefreshToken(newRefresh.toByteArray(Charsets.UTF_8))
                                }
                            }
                        } else {
                            runCatching { runBlocking { refreshTokenStorage.clear() } }
                        }
                        newAccess
                    }
                    403 -> {
                        val code = runCatching {
                            json.decodeFromString<ProblemDetailsDto>(resp.body?.string().orEmpty()).code
                        }.getOrNull()
                        if (code == "account_suspended") sessionStore.suspendAccount()
                        null
                    }
                    401 -> {
                        if (sessionStore.state.value !is SessionState.Suspended) {
                            sessionStore.clear()
                            runCatching { runBlocking { refreshTokenStorage.clear() } }
                        }
                        null
                    }
                    else -> null
                }
            }
        } catch (_: Exception) {
            null
        }
    }
}
