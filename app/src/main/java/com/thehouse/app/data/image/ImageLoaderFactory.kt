package com.thehouse.app.data.image

import android.content.Context
import coil3.ImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import okhttp3.OkHttpClient

/**
 * Fábrica base de Coil.
 *
 * No crea pantallas ni componentes de negocio. La UI futura consumirá un
 * ImageLoader controlado desde el composition root.
 */
object ImageLoaderFactory {
    fun create(
        context: Context,
        okHttpClient: OkHttpClient,
    ): ImageLoader = ImageLoader.Builder(context)
        .components {
            add(OkHttpNetworkFetcherFactory(callFactory = { okHttpClient }))
        }
        .crossfade(true)
        .build()
}
