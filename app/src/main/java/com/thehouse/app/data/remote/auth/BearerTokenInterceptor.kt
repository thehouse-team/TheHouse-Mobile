package com.thehouse.app.data.remote.auth

import com.thehouse.app.data.auth.session.InMemorySessionStore
import com.thehouse.app.data.remote.dto.visits.ProblemDetailsDto
import com.thehouse.app.data.serialization.JsonProvider
import com.thehouse.app.domain.model.SessionState
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Interceptor Bearer: agrega `Authorization: Bearer <accessToken>` en memoria
 * cuando hay sesión y la solicitud aún no lleva header.
 *
 * - Omite endpoints de auth que no usan Bearer (`login`, `register`, `refresh`).
 *   `logout` SÍ lleva Bearer según contrato D3; `GET /users/me` también.
 * - D19: si la sesión está Activa pero el access token aún no se hidrató
 *   (null/expirado), ejecuta un refresh single-flight ANTES de enviar la
 *   request protegida. Si el refresh falla, se envía la request sin token
 *   (Backend 401) y la sesión ya quedó limpia → la UI deriva a Login.
 * - Reintento máximo UNO: el token se refresca una sola vez por request.
 * - Nunca loguea el token (ver [SafeHttpLoggingInterceptor]).
 */
class BearerTokenInterceptor(
    private val sessionStore: InMemorySessionStore,
    private val tokenRefresher: TokenRefresher? = null,
) : Interceptor {

    private val json = JsonProvider.create()

    private val noBearerPaths = listOf(
        "/api/v1/auth/login",
        "/api/v1/auth/register",
        "/api/v1/auth/refresh",
    )

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val path = original.url.encodedPath
        if (noBearerPaths.any { path.endsWith(it) }) {
            return observeSuspension(chain.proceed(original))
        }
        if (sessionStore.state.value is SessionState.Suspended &&
            !path.endsWith("/api/v1/auth/logout")
        ) {
            throw AccountSuspendedIOException()
        }
        if (original.header("Authorization") != null) {
            return observeSuspension(chain.proceed(original))
        }

        var token = sessionStore.accessToken
        if (token.isNullOrBlank() && sessionStore.state.value is SessionState.Active) {
            // D19: hidratación sin red duplicada: el refresher es single-flight
            // y solo actúa cuando NO hay access token en memoria.
            token = tokenRefresher?.refreshIfNeeded()
        }
        if (token.isNullOrBlank()) {
            return observeSuspension(chain.proceed(original))
        }
        val signed = original.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()
        return observeSuspension(chain.proceed(signed))
    }

    private fun observeSuspension(response: Response): Response {
        if (response.code == 403) {
            // peekBody conserva intacto el Problem Details que mapeará el repositorio.
            val code = runCatching {
                json.decodeFromString<ProblemDetailsDto>(response.peekBody(8192).string()).code
            }.getOrNull()
            if (code == "account_suspended") sessionStore.suspendAccount()
        }
        return response
    }
}
