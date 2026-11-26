package com.thehouse.app.data.remote

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Logging HTTP mínimo y seguro.
 *
 * Registra método, ruta relativa, status, duración y `requestId` cuando el
 * Backend lo devuelve:
 *
 * ```
 * GET /api/v1/properties -> 200 (23020ms) requestId=abc123
 * ```
 *
 * Nunca registra headers de autorización, JWT, cookies, bodies, credenciales,
 * PII ni query params (la consulta se omite por completo: solo se usa
 * `HttpUrl.encodedPath`). Los segmentos de ruta con posible PII (p. ej. un
 * correo) se enmascaran con [sanitizePath].
 *
 * El host se omite a propósito: la app usa un único backend y el formato
 * pedido es `MÉTODO <ruta> -> <status> (<duración>ms)`.
 */
class SafeHttpLoggingInterceptor(
    private val logger: (String) -> Unit = { message -> Log.d(TAG, message) },
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val startNanos = System.nanoTime()
        val response = chain.proceed(request)
        val durationMs = (System.nanoTime() - startNanos) / 1_000_000
        val requestId = response.requestId()

        logger(
            "${request.method} ${sanitizePath(request.url.encodedPath)} " +
                "-> ${response.code} (${durationMs}ms)" +
                (requestId?.let { " requestId=$it" } ?: ""),
        )

        return response
    }

    /** Correlación con el Backend si la envía; opaca, sin PII. */
    private fun Response.requestId(): String? =
        header(HEADER_REQUEST_ID)
            ?: header(HEADER_CORRELATION_ID)
            ?: header(HEADER_REQUEST_ID_ALT)

    companion object {
        const val TAG = "TheHouseHttp"

        private const val HEADER_REQUEST_ID = "x-request-id"
        private const val HEADER_REQUEST_ID_ALT = "request-id"
        private const val HEADER_CORRELATION_ID = "x-correlation-id"
        private const val MASK = "***"

        /**
         * Enmascara segmentos de ruta con posible PII (p. ej. correos en la
         * ruta). No altera rutas opacas de negocio (`/api/v1/properties/{id}`).
         */
        fun sanitizePath(path: String): String =
            path.split('/').joinToString(separator = "/") { segment ->
                if (segment.contains('@')) MASK else segment
            }
    }
}
