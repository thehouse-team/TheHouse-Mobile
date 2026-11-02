package com.thehouse.app.data.remote

import okhttp3.OkHttpClient

/**
 * Fábrica base de OkHttp.
 *
 * No define endpoints ni ejecuta requests. El logging solo se agrega cuando
 * el build type lo habilita explícitamente.
 */
object HttpClientFactory {
    fun create(
        enableLogging: Boolean,
        timeouts: NetworkTimeouts = NetworkTimeouts(),
    ): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(timeouts.connect)
        .readTimeout(timeouts.read)
        .writeTimeout(timeouts.write)
        .callTimeout(timeouts.call)
        .apply {
            if (enableLogging) {
                addInterceptor(SafeHttpLoggingInterceptor())
            }
        }
        .build()
}
