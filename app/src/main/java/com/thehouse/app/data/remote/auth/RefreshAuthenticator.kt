package com.thehouse.app.data.remote.auth

import com.thehouse.app.data.auth.session.InMemorySessionStore
import com.thehouse.app.data.auth.storage.SessionTokenStorage
import com.thehouse.app.domain.model.SessionState
import okhttp3.Authenticator
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import java.util.concurrent.TimeUnit

/**
 * Authenticator con refresh single-flight (D3, contrato `POST /auth/refresh`).
 *
 * Ante varios 401 simultáneos SOLO UNA request ejecuta `/auth/refresh`; las
 * demás esperan el mismo resultado (lock + doble verificación de token) y
 * continúan con el token nuevo. Dos refresh simultáneos con el mismo token
 * pueden cerrar la sesión.
 *
 * - 200: actualiza accessToken en memoria, reemplaza refreshToken cifrado,
 *   reintenta la request original UNA vez.
 * - 401: borra accessToken + refreshToken, SessionState Anonymous.
 * - Nunca loopea: requests a `/auth/refresh`/`/auth/login`/`/auth/register`
 *   no se reintentan, y una request ya reintentada (`priorResponse != null`)
 *   no vuelve a refrescar.
 * - Nunca registra tokens ni bodies.
 *
 * D19: la lógica de refresh se delegó a [TokenRefresher], compartida con
 * [BearerTokenInterceptor] para el caso "sesión activa sin access token".
 */
class RefreshAuthenticator(
    baseUrl: String,
    private val sessionStore: InMemorySessionStore,
    refreshTokenStorage: SessionTokenStorage,
    refreshClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .callTimeout(15, TimeUnit.SECONDS)
        .build(),
    tokenRefresher: TokenRefresher? = null,
) : Authenticator {

    private val refresher: TokenRefresher = tokenRefresher ?: TokenRefresher(
        baseUrl = baseUrl,
        sessionStore = sessionStore,
        refreshTokenStorage = refreshTokenStorage,
        refreshClient = refreshClient,
    )

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
        if (sessionStore.state.value is SessionState.Suspended) return null

        val sentToken = response.request.header("Authorization")?.removePrefix("Bearer ")
        val currentToken = sessionStore.accessToken
        if (currentToken.isNullOrBlank()) return null
        // Otro hilo ya refrescó mientras esperábamos: reintentar con el nuevo.
        if (currentToken != sentToken) {
            return response.request.newBuilder()
                .header("Authorization", "Bearer $currentToken")
                .build()
        }
        // Un solo hilo llega aquí → único refresh en vuelo.
        val newToken = refresher.refreshNow() ?: return null
        if (newToken == sentToken) return null
        return response.request.newBuilder()
            .header("Authorization", "Bearer $newToken")
            .build()
    }
}
