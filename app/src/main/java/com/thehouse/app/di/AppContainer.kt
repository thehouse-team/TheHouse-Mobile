package com.thehouse.app.di

import android.content.Context
import coil3.ImageLoader
import com.thehouse.app.data.auth.repository.AuthRepositoryImpl
import com.thehouse.app.data.auth.repository.OnboardingPreferencesRepository
import com.thehouse.app.data.auth.session.InMemorySessionStore
import com.thehouse.app.data.auth.storage.EncryptedRefreshTokenStorage
import com.thehouse.app.data.config.AppConfig
import com.thehouse.app.data.config.AppConfigProvider
import com.thehouse.app.data.dispatcher.DefaultDispatcherProvider
import com.thehouse.app.data.image.ImageLoaderFactory
import com.thehouse.app.data.preferences.DataStorePreferencesStore
import com.thehouse.app.data.preferences.PreferencesStore
import com.thehouse.app.data.preferences.appPreferencesDataStore
import com.thehouse.app.data.remote.HttpClientFactory
import com.thehouse.app.data.remote.RetrofitFactory
import com.thehouse.app.data.remote.api.AuthApi
import com.thehouse.app.data.serialization.JsonProvider
import com.thehouse.app.domain.common.DispatcherProvider
import com.thehouse.app.domain.repository.AuthRepository
import com.thehouse.app.domain.repository.OnboardingRepository
import com.thehouse.app.domain.usecase.LoginUseCase
import com.thehouse.app.domain.usecase.LogoutUseCase
import com.thehouse.app.domain.usecase.ObserveSessionUseCase
import com.thehouse.app.domain.usecase.RegisterUseCase
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
        HttpClientFactory.create(
            context = appContext,
            enableLogging = config.httpLoggingEnabled,
        )
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

    // ===== NV-04 Auth + Onboarding =====

    val authApi: AuthApi by lazy { retrofit.create(AuthApi::class.java) }

    val sessionStore: InMemorySessionStore by lazy { InMemorySessionStore() }

    val refreshTokenStorage: EncryptedRefreshTokenStorage by lazy {
        EncryptedRefreshTokenStorage(preferences)
    }

    val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(
            api = authApi,
            sessionStore = sessionStore,
            refreshTokenStorage = refreshTokenStorage,
        )
    }

    val onboardingRepository: OnboardingRepository by lazy {
        OnboardingPreferencesRepository(preferences)
    }

    val register: RegisterUseCase by lazy { RegisterUseCase(authRepository) }
    val login: LoginUseCase by lazy { LoginUseCase(authRepository) }
    val logout: LogoutUseCase by lazy { LogoutUseCase(authRepository) }
    val observeSession: ObserveSessionUseCase by lazy { ObserveSessionUseCase(authRepository) }
}
