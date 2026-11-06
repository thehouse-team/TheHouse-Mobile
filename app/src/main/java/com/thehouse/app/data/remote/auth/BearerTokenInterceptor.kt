package com.thehouse.app.data.remote.auth

import com.thehouse.app.data.auth.session.InMemorySessionStore
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Interceptor Bearer: agrega `Authorization: Bearer <accessToken>` en memoria
 * cuando hay sesión y la solicitud aún no lleva header.
 *
 * - Nunca loguea el token (ver [com.thehouse.app.data.remote.SafeHttpLoggingInterceptor]).
 * - Omite endpoints de auth que no usan Bearer (`login`, `register`, `refresh`).
 *   `logout` SÍ lleva Bearer según contrato D3 (revoca access + familia refresh).
 *   `GET /users/me` SÍ lleva Bearer.
 */
class BearerTokenInterceptor(
    private val sessionStore: InMemorySessionStore,
) : Interceptor {

    private val noBearerPaths = listOf(
        "/api/v1/auth/login",
        "/api/v1/auth/register",
        "/api/v1/auth/refresh",
    )

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val path = original.url.encodedPath
        if (noBearerPaths.any { path.endsWith(it) }) {
            return chain.proceed(original)
        }
        if (original.header("Authorization") != null) {
            return chain.proceed(original)
        }
        val token = sessionStore.accessToken
        if (token.isNullOrBlank()) {
            return chain.proceed(original)
        }
        val signed = original.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()
        return chain.proceed(signed)
    }
}
