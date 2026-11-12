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
import com.thehouse.app.data.remote.api.ProfileApi
import com.thehouse.app.data.remote.api.PreferencesApi
import com.thehouse.app.data.favorites.repository.FavoritesRepositoryImpl
import com.thehouse.app.data.favorites.state.FavoriteIdsStore
import com.thehouse.app.data.remote.api.FavoritesApi
import com.thehouse.app.data.remote.api.PropertyApi
import com.thehouse.app.data.remote.api.UniversityApi
import com.thehouse.app.data.remote.api.VisitsApi
import com.thehouse.app.data.visits.repository.VisitsRepositoryImpl
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

import com.thehouse.app.domain.repository.VisitsRepository
import com.thehouse.app.domain.usecase.AddFavoriteUseCase
import com.thehouse.app.domain.usecase.CancelVisitUseCase
import com.thehouse.app.domain.usecase.CreateVisitUseCase

import com.thehouse.app.domain.usecase.GetFavoritesUseCase
import com.thehouse.app.domain.usecase.GetFavoriteIdsUseCase
import com.thehouse.app.domain.usecase.GetPropertiesUseCase
import com.thehouse.app.domain.usecase.GetVisitUseCase
import com.thehouse.app.domain.usecase.GetVisitsUseCase
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

import com.thehouse.app.domain.usecase.GetProfileUseCase
import com.thehouse.app.domain.usecase.UpdateProfileUseCase
import com.thehouse.app.domain.usecase.SelectUserUniversityUseCase
import com.thehouse.app.domain.usecase.GetPreferencesUseCase
import com.thehouse.app.domain.usecase.SavePreferencesUseCase
import com.thehouse.app.domain.usecase.GetQuickSetupUseCase

import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import retrofit2.Retrofit

/** Composition root de Dependency Injection manual.
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
    val addFavorite: com.thehouse.app.domain.usecase.AddFavoriteUseCase by lazy { com.thehouse.app.domain.usecase.AddFavoriteUseCase(favoritesRepository) }
    val removeFavorite: RemoveFavoriteUseCase by lazy { RemoveFavoriteUseCase(favoritesRepository) }


    // ===== D11 Profile + Preferences + Quick Setup =====

    val profileApi: ProfileApi by lazy { retrofit.create(ProfileApi::class.java) }

    val preferencesApi: PreferencesApi by lazy { retrofit.create(PreferencesApi::class.java) }

    val profileRepository: com.thehouse.app.domain.repository.ProfileRepository by lazy {
        com.thehouse.app.data.repository.ProfileRepositoryImpl(profileApi)
    }

    val preferencesRepository: com.thehouse.app.domain.repository.PreferenceRepository by lazy {
        com.thehouse.app.data.repository.PreferenceRepositoryImpl(preferencesApi)
    }

    val getProfile: GetProfileUseCase by lazy { GetProfileUseCase(profileRepository) }
    val updateProfile: UpdateProfileUseCase by lazy { UpdateProfileUseCase(profileRepository) }
    val selectUserUniversity: SelectUserUniversityUseCase by lazy { SelectUserUniversityUseCase(profileRepository) }
    val getPreferences: GetPreferencesUseCase by lazy { GetPreferencesUseCase(preferencesRepository) }
    val savePreferences: SavePreferencesUseCase by lazy { SavePreferencesUseCase(preferencesRepository) }
    val getQuickSetup: GetQuickSetupUseCase by lazy { GetQuickSetupUseCase(profileRepository) }


    // ===== D8.1 Visitas reales (NV-10) =====

    val visitsApi: VisitsApi by lazy { retrofit.create(VisitsApi::class.java) }

    val visitsRepository: VisitsRepository by lazy {
        VisitsRepositoryImpl(api = visitsApi, json = json)
    }

    val createVisit: CreateVisitUseCase by lazy { CreateVisitUseCase(visitsRepository) }
    val getVisits: GetVisitsUseCase by lazy { GetVisitsUseCase(visitsRepository) }
    val getVisit: GetVisitUseCase by lazy { GetVisitUseCase(visitsRepository) }
    val cancelVisit: CancelVisitUseCase by lazy { CancelVisitUseCase(visitsRepository) }

    val selectedUniversityRepository: SelectedUniversityRepository by lazy {
        SelectedUniversityRepositoryImpl(preferences = preferences)
    }

    val getUniversities: GetUniversitiesUseCase by lazy {
        GetUniversitiesUseCase(universityRepository)
    }

    val getProperties: GetPropertiesUseCase by lazy { GetPropertiesUseCase(propertyRepository) }

    val observeSelectedUniversity: ObserveSelectedUniversityUseCase by lazy {
        ObserveSelectedUniversityUseCase(selectedUniversityRepository)
    }

    val selectUniversity: SelectUniversityUseCase by lazy { SelectUniversityUseCase(selectedUniversityRepository) }

    val searchProperties: SearchPropertiesUseCase by lazy { SearchPropertiesUseCase() }

    val register: RegisterUseCase by lazy { RegisterUseCase(authRepository) }
    val login: LoginUseCase by lazy { LoginUseCase(authRepository) }
    val logout: LogoutUseCase by lazy { LogoutUseCase(authRepository) }
    val observeSession: ObserveSessionUseCase by lazy { ObserveSessionUseCase(authRepository) }
    val restoreSession: RestoreSessionUseCase by lazy { RestoreSessionUseCase(authRepository) }

    // ===== D9 Requests + Real Chat (reutiliza Retrofit/HttpClient D3) =====

    val requestsApi: com.thehouse.app.data.remote.api.RequestsApi by lazy {
        retrofit.create(com.thehouse.app.data.remote.api.RequestsApi::class.java)
    }

    val chatApi: com.thehouse.app.data.remote.api.ChatApi by lazy {
        retrofit.create(com.thehouse.app.data.remote.api.ChatApi::class.java)
    }

    val requestsRepository: com.thehouse.app.domain.repository.RequestsRepository by lazy {
        com.thehouse.app.data.requests.repository.RequestsRepositoryImpl(
            api = requestsApi,
            json = json,
        )
    }

    val conversationRepository: com.thehouse.app.domain.repository.ConversationRepository by lazy {
        com.thehouse.app.data.chat.repository.ConversationsRepositoryImpl(
            api = chatApi,
            json = json,
        )
    }

    val createRequest: com.thehouse.app.domain.usecase.CreateRequestUseCase by lazy {
        com.thehouse.app.domain.usecase.CreateRequestUseCase(requestsRepository)
    }

    val getUserRequests: com.thehouse.app.domain.usecase.GetUserRequestsUseCase by lazy {
        com.thehouse.app.domain.usecase.GetUserRequestsUseCase(requestsRepository)
    }

    val getRequest: com.thehouse.app.domain.usecase.GetRequestUseCase by lazy {
        com.thehouse.app.domain.usecase.GetRequestUseCase(requestsRepository)
    }

    val cancelRequest: com.thehouse.app.domain.usecase.CancelRequestUseCase by lazy {
        com.thehouse.app.domain.usecase.CancelRequestUseCase(requestsRepository)
    }

    val getLandlordRequests: com.thehouse.app.domain.usecase.GetLandlordRequestsUseCase by lazy {
        com.thehouse.app.domain.usecase.GetLandlordRequestsUseCase(requestsRepository)
    }

    val acceptRequest: com.thehouse.app.domain.usecase.AcceptRequestUseCase by lazy {
        com.thehouse.app.domain.usecase.AcceptRequestUseCase(requestsRepository)
    }

    val rejectRequest: com.thehouse.app.domain.usecase.RejectRequestUseCase by lazy {
        com.thehouse.app.domain.usecase.RejectRequestUseCase(requestsRepository)
    }

    val getConversations: com.thehouse.app.domain.usecase.GetConversationsUseCase by lazy {
        com.thehouse.app.domain.usecase.GetConversationsUseCase(conversationRepository)
    }

    val getMessages: com.thehouse.app.domain.usecase.GetMessagesUseCase by lazy {
        com.thehouse.app.domain.usecase.GetMessagesUseCase(conversationRepository)
    }

    val sendMessage: com.thehouse.app.domain.usecase.SendMessageUseCase by lazy {
        com.thehouse.app.domain.usecase.SendMessageUseCase(conversationRepository)
    }

    val markConversationRead: com.thehouse.app.domain.usecase.MarkConversationReadUseCase by lazy {
        com.thehouse.app.domain.usecase.MarkConversationReadUseCase(conversationRepository)
    }
}