package com.thehouse.app.di

import android.content.Context
import coil3.ImageLoader
import com.thehouse.app.data.config.AppConfig
import com.thehouse.app.data.config.AppConfigProvider
import com.thehouse.app.data.dispatcher.DefaultDispatcherProvider
import com.thehouse.app.data.image.ImageLoaderFactory
import com.thehouse.app.data.preferences.DataStorePreferencesStore
import com.thehouse.app.data.preferences.PreferencesStore
import com.thehouse.app.data.preferences.appPreferencesDataStore
import com.thehouse.app.data.remote.HttpClientFactory
import com.thehouse.app.data.remote.RetrofitFactory
import com.thehouse.app.data.serialization.JsonProvider
import com.thehouse.app.domain.common.DispatcherProvider
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import retrofit2.Retrofit

/**
 * Composition root de Dependency Injection manual.
 *
 * Construye dependencias de infraestructura y las expone de forma lazy.
 * No contiene lógica de negocio ni estado mutable global.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val config: AppConfig by lazy { AppConfigProvider.provide() }
    val json: Json by lazy { JsonProvider.create() }
    val dispatchers: DispatcherProvider by lazy { DefaultDispatcherProvider() }

    val httpClient: OkHttpClient by lazy {
        HttpClientFactory.create(enableLogging = config.httpLoggingEnabled)
    }

    val retrofit: Retrofit by lazy {
        RetrofitFactory.create(
            baseUrl = config.apiBaseUrl,
            client = httpClient,
            json = json,
        )
    }

    val imageLoader: ImageLoader by lazy {
        ImageLoaderFactory.create(
            context = appContext,
            okHttpClient = httpClient,
        )
    }

    val preferences: PreferencesStore by lazy {
        DataStorePreferencesStore(appContext.appPreferencesDataStore)
    }
}
