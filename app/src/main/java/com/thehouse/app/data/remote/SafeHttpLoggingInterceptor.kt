package com.thehouse.app.data.remote

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Logging HTTP mínimo y seguro.
 *
 * Registra únicamente método, host, status y duración.
 * Nunca registra headers, bodies, paths, query params, tokens, credenciales ni PII.
 */
class SafeHttpLoggingInterceptor(
    private val logger: (String) -> Unit = { message -> Log.d(TAG, message) },
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val startNanos = System.nanoTime()
        val response = chain.proceed(request)
        val durationMs = (System.nanoTime() - startNanos) / 1_000_000

        logger(
            "${request.method} ${request.url.host} " +
                "-> ${response.code} (${durationMs}ms)",
        )

        return response
    }

    private companion object {
        const val TAG = "TheHouseHttp"
    }
}
