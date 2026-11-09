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
import com.thehouse.app.data.catalog.repository.PropertyRepositoryImpl
import com.thehouse.app.data.catalog.repository.SelectedUniversityRepositoryImpl
import com.thehouse.app.data.catalog.repository.UniversityRepositoryImpl
import com.thehouse.app.data.remote.HttpClientFactory
import com.thehouse.app.data.remote.RetrofitFactory
import com.thehouse.app.data.remote.api.AuthApi
import com.thehouse.app.data.favorites.repository.FavoritesRepositoryImpl
import com.thehouse.app.data.favorites.state.FavoriteIdsStore
import com.thehouse.app.data.remote.api.FavoritesApi
import com.thehouse.app.data.remote.api.PropertyApi
import com.thehouse.app.data.remote.api.UniversityApi
import com.thehouse.app.data.remote.auth.BearerTokenInterceptor
import com.thehouse.app.data.remote.auth.RefreshAuthenticator
import com.thehouse.app.data.serialization.JsonProvider
import com.thehouse.app.domain.common.DispatcherProvider
import com.thehouse.app.domain.repository.AuthRepository
import com.thehouse.app.domain.repository.FavoritesRepository
import com.thehouse.app.domain.repository.OnboardingRepository
import com.thehouse.app.domain.repository.PropertyRepository
import com.thehouse.app.domain.repository.SelectedUniversityRepository
import com.thehouse.app.domain.repository.UniversityRepository
import com.thehouse.app.domain.usecase.AddFavoriteUseCase
import com.thehouse.app.domain.usecase.GetFavoritesUseCase
import com.thehouse.app.domain.usecase.GetFavoriteIdsUseCase
import com.thehouse.app.domain.usecase.GetPropertiesUseCase
import com.thehouse.app.domain.usecase.GetUniversitiesUseCase
import com.thehouse.app.domain.usecase.RemoveFavoriteUseCase
import com.thehouse.app.domain.usecase.LoginUseCase
import com.thehouse.app.domain.usecase.LogoutUseCase
import com.thehouse.app.domain.usecase.ObserveSelectedUniversityUseCase
import com.thehouse.app.domain.usecase.ObserveSessionUseCase
import com.thehouse.app.domain.usecase.RegisterUseCase
import com.thehouse.app.domain.usecase.RestoreSessionUseCase
import com.thehouse.app.domain.usecase.SearchPropertiesUseCase
import com.thehouse.app.domain.usecase.SelectUniversityUseCase
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import retrofit2.Retrofit

/**
 * Composition root de Dependency Injection manual.
 *
 * Construye dependencias de infraestructura y las expone de forma lazy.
 * No contiene lógica de negocio ni estado mutable global.
 *
 * Orden de construcción D3: sesión/token-storage (sin red) primero,
 * luego httpClient con Bearer + authenticator single-flight (usa solo
 * sesión/storage/config, sin ciclo con el Retrofit/repositorio).
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val config: AppConfig by lazy { AppConfigProvider.provide() }
    val json: Json by lazy { JsonProvider.create() }
    val dispatchers: DispatcherProvider by lazy { DefaultDispatcherProvider() }

    val preferences: PreferencesStore by lazy {
        DataStorePreferencesStore(appContext.appPreferencesDataStore)
    }

    // ===== NV-04 Auth + Onboarding (D3: sesión persistente) =====

    val sessionStore: InMemorySessionStore by lazy { InMemorySessionStore() }

    val refreshTokenStorage: EncryptedRefreshTokenStorage by lazy {
        EncryptedRefreshTokenStorage(preferences)
    }

    val bearerTokenInterceptor: BearerTokenInterceptor by lazy {
        BearerTokenInterceptor(sessionStore)
    }

    val refreshAuthenticator: RefreshAuthenticator by lazy {
        RefreshAuthenticator(
            baseUrl = config.apiBaseUrl,
            sessionStore = sessionStore,
            refreshTokenStorage = refreshTokenStorage,
        )
    }

    val httpClient: OkHttpClient by lazy {
        HttpClientFactory.create(
            context = appContext,
            enableLogging = config.httpLoggingEnabled,
            bearerInterceptor = bearerTokenInterceptor,
            authenticator = refreshAuthenticator,
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

    val authApi: AuthApi by lazy { retrofit.create(AuthApi::class.java) }

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

    // ===== D5 Catálogo real (NV-06/NV-07, reutiliza Retrofit/HttpClient D3) =====

    val universityApi: UniversityApi by lazy { retrofit.create(UniversityApi::class.java) }

    val propertyApi: PropertyApi by lazy { retrofit.create(PropertyApi::class.java) }

    val universityRepository: UniversityRepository by lazy {
        UniversityRepositoryImpl(api = universityApi)
    }

    val propertyRepository: PropertyRepository by lazy {
        PropertyRepositoryImpl(api = propertyApi)
    }

    val favoritesApi: FavoritesApi by lazy { retrofit.create(FavoritesApi::class.java) }

    val favoritesRepository: FavoritesRepository by lazy {
        FavoritesRepositoryImpl(api = favoritesApi)
    }

    val favoriteIdsStore: FavoriteIdsStore by lazy { FavoriteIdsStore() }

    val getFavorites: GetFavoritesUseCase by lazy { GetFavoritesUseCase(favoritesRepository) }
    val getFavoriteIds: GetFavoriteIdsUseCase by lazy { GetFavoriteIdsUseCase(favoritesRepository) }
    val addFavorite: AddFavoriteUseCase by lazy { AddFavoriteUseCase(favoritesRepository) }
    val removeFavorite: RemoveFavoriteUseCase by lazy { RemoveFavoriteUseCase(favoritesRepository) }

    val selectedUniversityRepository: SelectedUniversityRepository by lazy {
        SelectedUniversityRepositoryImpl(preferences = preferences)
    }

    val getUniversities: GetUniversitiesUseCase by lazy {
        GetUniversitiesUseCase(universityRepository)
    }

    val getProperties: GetPropertiesUseCase by lazy {
        GetPropertiesUseCase(propertyRepository)
    }

    val observeSelectedUniversity: ObserveSelectedUniversityUseCase by lazy {
        ObserveSelectedUniversityUseCase(selectedUniversityRepository)
    }

    val selectUniversity: SelectUniversityUseCase by lazy {
        SelectUniversityUseCase(selectedUniversityRepository)
    }

    val searchProperties: SearchPropertiesUseCase by lazy { SearchPropertiesUseCase() }

    val register: RegisterUseCase by lazy { RegisterUseCase(authRepository) }
    val login: LoginUseCase by lazy { LoginUseCase(authRepository) }
    val logout: LogoutUseCase by lazy { LogoutUseCase(authRepository) }
    val observeSession: ObserveSessionUseCase by lazy { ObserveSessionUseCase(authRepository) }
    val restoreSession: RestoreSessionUseCase by lazy { RestoreSessionUseCase(authRepository) }
}
