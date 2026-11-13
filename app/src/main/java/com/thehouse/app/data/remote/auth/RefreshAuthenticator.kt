package com.thehouse.app.data.remote.auth

import com.thehouse.app.data.auth.session.InMemorySessionStore
import com.thehouse.app.data.auth.storage.SessionTokenStorage
import com.thehouse.app.data.remote.dto.auth.AuthResponseDto
import com.thehouse.app.data.remote.dto.visits.ProblemDetailsDto
import com.thehouse.app.data.serialization.JsonProvider
import com.thehouse.app.domain.model.SessionState
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import java.util.concurrent.TimeUnit

/**
 * Authenticator con refresh single-flight (D3, contrato `POST /auth/refresh`).
 *
 * Regla crítica D3: ante varios 401 simultáneos SOLO UNA request ejecuta
 * `/auth/refresh`. Las demás esperan el mismo resultado (lock + doble
 * verificación de token) y continúan con el token nuevo.
 * Dos refresh simultáneos con el mismo token pueden cerrar la sesión.
 *
 * - 200: actualiza accessToken en memoria, reemplaza refreshToken cifrado,
 *   reintenta la request original UNA vez.
 * - 401: borra accessToken + refreshToken, SessionState Anonymous.
 *   (Devuelve null → el caller recibe el 401 original; la navegación
 *   a Login se deriva de SessionState.Anonymous.)
 * - Nunca loopea: requests a `/auth/refresh`/`/auth/login`/`/auth/register`
 *   no se reintentan, y una request ya reintentada (`priorResponse != null`)
 *   no vuelve a refrescar.
 * - Nunca registra tokens ni bodies.
 *
 * Nota de hilos: `Authenticator.authenticate` es bloqueante (hilos OkHttp).
 * El lock es un monitor JVM; la operación de red se ejecuta con un cliente
 * OkHttp aislado (sin este authenticator) para evitar recursión.
 */
class RefreshAuthenticator(
    private val baseUrl: String,
    private val sessionStore: InMemorySessionStore,
    private val refreshTokenStorage: SessionTokenStorage,
    private val refreshClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .callTimeout(15, TimeUnit.SECONDS)
        .build(),
) : Authenticator {

    private val lock = Any()
    private val json: Json = JsonProvider.create()

    private val noRetryPaths = listOf(
        "/api/v1/auth/refresh",
        "/api/v1/auth/login",
        "/api/v1/auth/register",
    )

    override fun authenticate(route: Route?, response: Response): Request? {
        val path = response.request.url.encodedPath
        if (noRetryPaths.any { path.endsWith(it) }) return null
        // Máximo 1 retry por request original: si ya hubo retry, no insistir.
        if (response.priorResponse != null) return null

        synchronized(lock) {
            if (sessionStore.state.value is SessionState.Suspended) return null
            val sentToken = response.request.header("Authorization")?.removePrefix("Bearer ")
            val currentToken = sessionStore.accessToken
            if (currentToken.isNullOrBlank()) return null
            // Otro hilo ya refrescó mientras esperábamos el lock.
            if (currentToken != sentToken) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $currentToken")
                    .build()
            }
            // Un solo hilo llega aquí → único refresh en vuelo.
            val newToken = runBlocking { refreshOnce() } ?: return null
            if (newToken == sentToken) return null
            return response.request.newBuilder()
                .header("Authorization", "Bearer $newToken")
                .build()
        }
    }

    private suspend fun refreshOnce(): String? {
        val refreshToken = refreshTokenStorage.readRefreshToken()
            ?.toString(Charsets.UTF_8)
        if (refreshToken.isNullOrBlank()) {
            sessionStore.clear()
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
                if (resp.code == 200) {
                    if (sessionStore.state.value is SessionState.Suspended) return null
                    val text = resp.body?.string().orEmpty()
                    val dto = json.decodeFromString(AuthResponseDto.serializer(), text)
                    val newAccess = dto.accessToken ?: dto.token
                    val newRefresh = dto.refreshToken
                    if (newAccess.isNullOrBlank()) return null
                    sessionStore.updateAccessToken(newAccess)
                    if (newRefresh != null) {
                        refreshTokenStorage.writeRefreshToken(newRefresh.toByteArray(Charsets.UTF_8))
                    } else {
                        refreshTokenStorage.clear()
                    }
                    newAccess
                } else if (resp.code == 403) {
                    val code = runCatching {
                        json.decodeFromString<ProblemDetailsDto>(resp.body?.string().orEmpty()).code
                    }.getOrNull()
                    if (code == "account_suspended") sessionStore.suspendAccount()
                    null
                } else if (resp.code == 401 && sessionStore.state.value !is SessionState.Suspended) {
                    sessionStore.clear()
                    refreshTokenStorage.clear()
                    null
                } else {
                    null
                }
            }
        } catch (_: Exception) {
            null
        }
    }
}
