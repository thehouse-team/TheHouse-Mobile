package com.thehouse.app.data.remote

import android.content.Context
import java.io.File
import okhttp3.Cache
import okhttp3.OkHttpClient

/**
 * Fábrica base de OkHttp.
 *
 * U2 (NV-04): agrega caché HTTP en disco, respetando `Cache-Control` y `ETag`
 * del servidor. Las respuestas de auth no se cachean: la API de auth envía
 * `Cache-Control: no-store, no-cache` en todas sus llamadas.
 * El logging solo se agrega cuando el build type lo habilita explícitamente.
 */
object HttpClientFactory {

    private const val CACHE_DIR = "thehouse_http_cache"

    fun create(
        context: Context,
        enableLogging: Boolean,
        timeouts: NetworkTimeouts = NetworkTimeouts(),
    ): OkHttpClient = create(
        enableLogging = enableLogging,
        cacheDir = File(context.cacheDir, CACHE_DIR),
        timeouts = timeouts,
    )

    fun create(
        enableLogging: Boolean,
        cacheDir: File?,
        timeouts: NetworkTimeouts = NetworkTimeouts(),
    ): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(timeouts.connect)
        .readTimeout(timeouts.read)
        .writeTimeout(timeouts.write)
        .callTimeout(timeouts.call)
        .apply { cacheDir?.let { cache(Cache(it, CACHE_SIZE_BYTES)) } }
        .apply {
            if (enableLogging) {
                addInterceptor(SafeHttpLoggingInterceptor())
            }
        }
        .build()

    private const val CACHE_SIZE_BYTES: Long = 10L * 1024L * 1024L // 10 MiB
}
