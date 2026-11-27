package com.thehouse.app.di

import android.content.Context
import coil3.ImageLoader
import com.thehouse.app.data.auth.repository.AuthRepositoryImpl
import com.thehouse.app.data.auth.repository.OnboardingPreferencesRepository
import com.thehouse.app.data.auth.session.InMemorySessionStore
import com.thehouse.app.data.auth.storage.EncryptedRefreshTokenStorage
import com.thehouse.app.data.cache.OfflineCacheStore
import com.thehouse.app.data.config.AppConfig
import com.thehouse.app.data.config.AppConfigProvider
import com.thehouse.app.data.connectivity.AndroidConnectivityObserver
import com.thehouse.app.data.dispatcher.DefaultDispatcherProvider
import com.thehouse.app.data.image.ImageHttpClientFactory
import com.thehouse.app.data.image.ImageLoaderFactory
import com.thehouse.app.data.image.ImageUrlResolver
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
import com.thehouse.app.data.remote.api.SmartMatchApi
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
import com.thehouse.app.domain.connectivity.ConnectivityObserver
import com.thehouse.app.domain.model.SessionState
import com.thehouse.app.domain.repository.AuthRepository
import com.thehouse.app.domain.repository.FavoritesRepository
import com.thehouse.app.domain.repository.OnboardingRepository
import com.thehouse.app.domain.repository.PropertyRepository
import com.thehouse.app.domain.repository.SelectedUniversityRepository
import com.thehouse.app.domain.repository.UniversityRepository
import com.thehouse.app.presentation.catalog.CatalogFlowLogger

import com.thehouse.app.domain.repository.VisitsRepository
import com.thehouse.app.data.smartmatch.SmartMatchRepository
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
import com.thehouse.app.domain.usecase.SearchRoomsUseCase
import com.thehouse.app.data.room.RoomSearchRepositoryImpl
import com.thehouse.app.domain.usecase.FavoriteStateStore
import com.thehouse.app.domain.usecase.ToggleFavoriteUseCase
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

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

    val offlineCacheStore: OfflineCacheStore by lazy {
        OfflineCacheStore(preferences = preferences, json = json)
    }

    val connectivityObserver: ConnectivityObserver by lazy {
        AndroidConnectivityObserver(appContext)
    }

    /** D21 adenda: lectura puntual de ubicación, solo tras acción del usuario. */
    val deviceLocationProvider: com.thehouse.app.domain.location.DeviceLocationProvider by lazy {
        com.thehouse.app.data.location.AndroidDeviceLocationProvider(appContext)
    }

    /** D21 cierre: persistencia de la decisión de POST_NOTIFICATIONS. */
    val notificationPermissionStore: com.thehouse.app.domain.repository.NotificationPermissionStore by lazy {
        com.thehouse.app.data.notifications.DataStoreNotificationPermissionStore(preferences)
    }

    private val currentUserIdProvider: () -> String? = {
        (sessionStore.state.value as? SessionState.Active)?.session?.userId
    }

    // ===== NV-04 Auth + Onboarding (D3: sesión persistente) =====

    val sessionStore: InMemorySessionStore by lazy { InMemorySessionStore() }

    val refreshTokenStorage: EncryptedRefreshTokenStorage by lazy {
        EncryptedRefreshTokenStorage(preferences)
    }

    /** D19: único refresher single-flight compartido por interceptor y authenticator. */
    val tokenRefresher: com.thehouse.app.data.remote.auth.TokenRefresher by lazy {
        com.thehouse.app.data.remote.auth.TokenRefresher(
            baseUrl = config.apiBaseUrl,
            sessionStore = sessionStore,
            refreshTokenStorage = refreshTokenStorage,
        )
    }

    val bearerTokenInterceptor: BearerTokenInterceptor by lazy {
        BearerTokenInterceptor(sessionStore, tokenRefresher)
    }

    val refreshAuthenticator: RefreshAuthenticator by lazy {
        RefreshAuthenticator(
            baseUrl = config.apiBaseUrl,
            sessionStore = sessionStore,
            refreshTokenStorage = refreshTokenStorage,
            tokenRefresher = tokenRefresher,
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

    /** D21 hotfix: Coil NO comparte el cliente Bearer de Retrofit. */
    val imageHttpClient: OkHttpClient by lazy { ImageHttpClientFactory.create() }

    val imageLoader: ImageLoader by lazy {
        ImageLoaderFactory.create(
            context = appContext,
            okHttpClient = imageHttpClient,
        )
    }

    /** Resuelve URLs relativas del contrato de medios contra el origen real. */
    fun resolveImageUrl(value: String?): String? =
        ImageUrlResolver.resolve(value, config.apiBaseUrl)

    val authApi: AuthApi by lazy { retrofit.create(AuthApi::class.java) }

    private val authRepositoryImpl: AuthRepositoryImpl by lazy {
        AuthRepositoryImpl(
            api = authApi,
            sessionStore = sessionStore,
            refreshTokenStorage = refreshTokenStorage,
            onLegalStatus = { upToDate -> legalUpToDate.value = upToDate },
        )
    }

    val authRepository: AuthRepository by lazy { authRepositoryImpl }

    /** NV-15: limpieza local de sesión sin red (delete account / logout). */
    val localSessionCleaner: com.thehouse.app.domain.auth.LocalSessionCleaner by lazy {
        authRepositoryImpl
    }

    val onboardingRepository: OnboardingRepository by lazy {
        OnboardingPreferencesRepository(preferences)
    }

    // ===== D5 Catálogo real (NV-06/NV-07, reutiliza Retrofit/HttpClient D3) =====

    val universityApi: UniversityApi by lazy { retrofit.create(UniversityApi::class.java) }

    val propertyApi: PropertyApi by lazy { retrofit.create(PropertyApi::class.java) }

    val universityRepository: UniversityRepository by lazy {
        UniversityRepositoryImpl(api = universityApi, cache = offlineCacheStore)
    }

    val propertyRepository: PropertyRepository by lazy {
        PropertyRepositoryImpl(api = propertyApi, cache = offlineCacheStore)
    }

    val favoritesApi: FavoritesApi by lazy { retrofit.create(FavoritesApi::class.java) }

    val favoritesRepository: FavoritesRepository by lazy {
        FavoritesRepositoryImpl(
            api = favoritesApi,
            cache = offlineCacheStore,
            currentUserId = currentUserIdProvider,
        )
    }

    val favoriteIdsStore: FavoriteIdsStore by lazy { FavoriteIdsStore() }

    /** D20: estado compartido de favoritos por roomId (identidad real). */
    val favoriteStateStore: FavoriteStateStore by lazy { FavoriteStateStore() }
    private val favoriteInFlight = java.util.Collections.newSetFromMap(java.util.concurrent.ConcurrentHashMap<String, Boolean>())

    val getFavorites: GetFavoritesUseCase by lazy { GetFavoritesUseCase(favoritesRepository) }
    val getFavoriteIds: GetFavoriteIdsUseCase by lazy { GetFavoriteIdsUseCase(favoritesRepository) }
    val addFavorite: com.thehouse.app.domain.usecase.AddFavoriteUseCase by lazy { com.thehouse.app.domain.usecase.AddFavoriteUseCase(favoritesRepository) }
    val removeFavorite: RemoveFavoriteUseCase by lazy { RemoveFavoriteUseCase(favoritesRepository) }
    val toggleFavorite: ToggleFavoriteUseCase by lazy {
        ToggleFavoriteUseCase(
            repository = favoritesRepository,
            store = favoriteStateStore,
            isAuthenticated = { sessionStore.state.value is SessionState.Active },
            inFlight = favoriteInFlight,
        )
    }

    /** D20 hotfix: single-flight de sincronía de favoritos por sesión. */
    private var favoritesSyncJob: kotlinx.coroutines.Job? = null
    private var favoritesSyncOwner: String? = null

    fun syncFavorites(force: Boolean = false) {
        val session = sessionStore.state.value
        if (session !is SessionState.Active) {
            favoritesSyncJob?.cancel()
            favoritesSyncJob = null
            favoritesSyncOwner = null
            favoriteStateStore.clear()
            return
        }
        val owner = session.session.userId
        // Evita dos GET /favorites/ids simultáneos por recomposición/LaunchedEffect.
        if (!force && favoritesSyncJob?.isActive == true && favoritesSyncOwner == owner) return
        favoritesSyncJob?.cancel()
        favoritesSyncOwner = owner
        favoritesSyncJob = applicationScope.launch {
            CatalogFlowLogger.home("load_trigger", "event", "favorites_sync", "force", force)
            getFavoriteIds().onSuccess { favoriteStateStore.setAll(it) }
        }
    }


    // ===== D11 Profile + Preferences + Quick Setup =====

    val profileApi: ProfileApi by lazy { retrofit.create(ProfileApi::class.java) }

    val preferencesApi: PreferencesApi by lazy { retrofit.create(PreferencesApi::class.java) }

    val profileRepository: com.thehouse.app.domain.repository.ProfileRepository by lazy {
        com.thehouse.app.data.repository.ProfileRepositoryImpl(
            api = profileApi,
            cache = offlineCacheStore,
            currentUserId = currentUserIdProvider,
        )
    }

    val preferencesRepository: com.thehouse.app.domain.repository.PreferenceRepository by lazy {
        com.thehouse.app.data.repository.PreferenceRepositoryImpl(
            api = preferencesApi,
            cache = offlineCacheStore,
            currentUserId = currentUserIdProvider,
        )
    }

    val roomApi: com.thehouse.app.data.remote.api.RoomApi by lazy { retrofit.create(com.thehouse.app.data.remote.api.RoomApi::class.java) }
    val roomRepository: com.thehouse.app.domain.usecase.RoomDetailRepository by lazy {
        com.thehouse.app.data.room.RoomRepository(roomApi)
    }
    val getRoomDetail: com.thehouse.app.domain.usecase.GetRoomDetailUseCase by lazy {
        com.thehouse.app.domain.usecase.GetRoomDetailUseCase(roomRepository)
    }

    /** D20: búsqueda real de habitaciones (GET /api/v1/rooms/search). */
    val roomSearchRepository: RoomSearchRepositoryImpl by lazy { RoomSearchRepositoryImpl(roomApi) }
    val searchRooms: SearchRoomsUseCase by lazy { SearchRoomsUseCase(roomSearchRepository) }

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
    val createRoomVisit: com.thehouse.app.domain.usecase.CreateRoomVisitUseCase by lazy {
        com.thehouse.app.domain.usecase.CreateRoomVisitUseCase(visitsRepository)
    }
    val getVisits: GetVisitsUseCase by lazy { GetVisitsUseCase(visitsRepository) }
    val getVisit: GetVisitUseCase by lazy { GetVisitUseCase(visitsRepository) }
    val cancelVisit: CancelVisitUseCase by lazy { CancelVisitUseCase(visitsRepository) }

    val selectedUniversityRepository: SelectedUniversityRepository by lazy {
        SelectedUniversityRepositoryImpl(preferences = preferences)
    }

    val getUniversities: GetUniversitiesUseCase by lazy {
        GetUniversitiesUseCase(universityRepository)
    }

    val searchNearbyUniversities: com.thehouse.app.domain.usecase.SearchNearbyUniversitiesUseCase by lazy {
        com.thehouse.app.domain.usecase.SearchNearbyUniversitiesUseCase(universityRepository)
    }

    val getProperties: GetPropertiesUseCase by lazy { GetPropertiesUseCase(propertyRepository) }

    val observeSelectedUniversity: ObserveSelectedUniversityUseCase by lazy {
        ObserveSelectedUniversityUseCase(selectedUniversityRepository)
    }

    val selectUniversity: SelectUniversityUseCase by lazy { SelectUniversityUseCase(selectedUniversityRepository) }

    val searchProperties: SearchPropertiesUseCase by lazy { SearchPropertiesUseCase() }

    val register: RegisterUseCase by lazy { RegisterUseCase(authRepository) }
    val login: LoginUseCase by lazy { LoginUseCase(authRepository) }
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

    // ===== D12 Landlord real (NV-12, reutiliza Retrofit/HttpClient D3) =====

    val landlordApi: com.thehouse.app.data.remote.api.LandlordApi by lazy {
        retrofit.create(com.thehouse.app.data.remote.api.LandlordApi::class.java)
    }

    val landlordRepository: com.thehouse.app.domain.repository.LandlordRepository by lazy {
        com.thehouse.app.data.landlord.repository.LandlordRepositoryImpl(
            api = landlordApi,
            json = json,
        )
    }

    val activateLandlord: com.thehouse.app.domain.usecase.ActivateLandlordUseCase by lazy {
        com.thehouse.app.domain.usecase.ActivateLandlordUseCase(landlordRepository)
    }

    val getLandlordProfile: com.thehouse.app.domain.usecase.GetLandlordProfileUseCase by lazy {
        com.thehouse.app.domain.usecase.GetLandlordProfileUseCase(landlordRepository)
    }

    val getLandlordBuildings: com.thehouse.app.domain.usecase.GetLandlordBuildingsUseCase by lazy {
        com.thehouse.app.domain.usecase.GetLandlordBuildingsUseCase(landlordRepository)
    }

    val getLandlordBuilding: com.thehouse.app.domain.usecase.GetLandlordBuildingUseCase by lazy {
        com.thehouse.app.domain.usecase.GetLandlordBuildingUseCase(landlordRepository)
    }

    val createBuilding: com.thehouse.app.domain.usecase.CreateBuildingUseCase by lazy {
        com.thehouse.app.domain.usecase.CreateBuildingUseCase(landlordRepository)
    }

    val updateBuilding: com.thehouse.app.domain.usecase.UpdateBuildingUseCase by lazy {
        com.thehouse.app.domain.usecase.UpdateBuildingUseCase(landlordRepository)
    }

    val deleteBuilding: com.thehouse.app.domain.usecase.DeleteBuildingUseCase by lazy {
        com.thehouse.app.domain.usecase.DeleteBuildingUseCase(landlordRepository)
    }

    val generateRooms: com.thehouse.app.domain.usecase.GenerateRoomsUseCase by lazy {
        com.thehouse.app.domain.usecase.GenerateRoomsUseCase(landlordRepository)
    }

    val getLandlordRooms: com.thehouse.app.domain.usecase.GetLandlordRoomsUseCase by lazy {
        com.thehouse.app.domain.usecase.GetLandlordRoomsUseCase(landlordRepository)
    }

    val updateRoom: com.thehouse.app.domain.usecase.UpdateRoomUseCase by lazy {
        com.thehouse.app.domain.usecase.UpdateRoomUseCase(landlordRepository)
    }

    val deleteRoom: com.thehouse.app.domain.usecase.DeleteRoomUseCase by lazy {
        com.thehouse.app.domain.usecase.DeleteRoomUseCase(landlordRepository)
    }

    val pauseRoom: com.thehouse.app.domain.usecase.PauseRoomUseCase by lazy {
        com.thehouse.app.domain.usecase.PauseRoomUseCase(landlordRepository)
    }

    val resumeRoom: com.thehouse.app.domain.usecase.ResumeRoomUseCase by lazy {
        com.thehouse.app.domain.usecase.ResumeRoomUseCase(landlordRepository)
    }

    val getPublicationStatus: com.thehouse.app.domain.usecase.GetPublicationStatusUseCase by lazy {
        com.thehouse.app.domain.usecase.GetPublicationStatusUseCase(landlordRepository)
    }

    val publishBuilding: com.thehouse.app.domain.usecase.PublishBuildingUseCase by lazy {
        com.thehouse.app.domain.usecase.PublishBuildingUseCase(landlordRepository)
    }

    val uploadBuildingPhoto: com.thehouse.app.domain.usecase.UploadBuildingPhotoUseCase by lazy {
        com.thehouse.app.domain.usecase.UploadBuildingPhotoUseCase(landlordRepository)
    }

    val deleteBuildingPhoto: com.thehouse.app.domain.usecase.DeleteBuildingPhotoUseCase by lazy {
        com.thehouse.app.domain.usecase.DeleteBuildingPhotoUseCase(landlordRepository)
    }

    val uploadRoomPhoto: com.thehouse.app.domain.usecase.UploadRoomPhotoUseCase by lazy {
        com.thehouse.app.domain.usecase.UploadRoomPhotoUseCase(landlordRepository)
    }

    val deleteRoomPhoto: com.thehouse.app.domain.usecase.DeleteRoomPhotoUseCase by lazy {
        com.thehouse.app.domain.usecase.DeleteRoomPhotoUseCase(landlordRepository)
    }

    val uploadIneDocument: com.thehouse.app.domain.usecase.UploadIneDocumentUseCase by lazy {
        com.thehouse.app.domain.usecase.UploadIneDocumentUseCase(landlordRepository)
    }

    val uploadPropertyProofDocument: com.thehouse.app.domain.usecase.UploadPropertyProofDocumentUseCase by lazy {
        com.thehouse.app.domain.usecase.UploadPropertyProofDocumentUseCase(landlordRepository)
    }

    val getBuildingDocuments: com.thehouse.app.domain.usecase.GetBuildingDocumentsUseCase by lazy {
        com.thehouse.app.domain.usecase.GetBuildingDocumentsUseCase(landlordRepository)
    }

    val getLandlordStatsRooms: com.thehouse.app.domain.usecase.GetLandlordStatsRoomsUseCase by lazy {
        com.thehouse.app.domain.usecase.GetLandlordStatsRoomsUseCase(landlordRepository)
    }

    // ===== D13 Reportes / verificación (NV-21, reutiliza Retrofit/HttpClient D3) =====

    val reportsApi: com.thehouse.app.data.remote.api.ReportsApi by lazy {
        retrofit.create(com.thehouse.app.data.remote.api.ReportsApi::class.java)
    }

    val reportsRepository: com.thehouse.app.domain.repository.ReportsRepository by lazy {
        com.thehouse.app.data.reports.repository.ReportsRepositoryImpl(
            api = reportsApi,
            json = json,
        )
    }

    val createReport: com.thehouse.app.domain.usecase.CreateReportUseCase by lazy {
        com.thehouse.app.domain.usecase.CreateReportUseCase(reportsRepository)
    }

    val getBuildingVerification: com.thehouse.app.domain.usecase.GetBuildingVerificationUseCase by lazy {
        com.thehouse.app.domain.usecase.GetBuildingVerificationUseCase(reportsRepository)
    }

    // ===== D13.1 Agreements + My Rental =====

    val agreementsApi: com.thehouse.app.data.remote.api.AgreementsApi by lazy {
        retrofit.create(com.thehouse.app.data.remote.api.AgreementsApi::class.java)
    }

    val agreementsRepository: com.thehouse.app.domain.repository.AgreementsRepository by lazy {
        com.thehouse.app.data.agreements.repository.AgreementsRepositoryImpl(
            api = agreementsApi,
            json = json,
        )
    }

    val createAgreement: com.thehouse.app.domain.usecase.CreateAgreementUseCase by lazy {
        com.thehouse.app.domain.usecase.CreateAgreementUseCase(agreementsRepository)
    }

    val getLandlordAgreements: com.thehouse.app.domain.usecase.GetLandlordAgreementsUseCase by lazy {
        com.thehouse.app.domain.usecase.GetLandlordAgreementsUseCase(agreementsRepository)
    }

    val getLandlordAgreement: com.thehouse.app.domain.usecase.GetLandlordAgreementUseCase by lazy {
        com.thehouse.app.domain.usecase.GetLandlordAgreementUseCase(agreementsRepository)
    }

    val issueAgreementCode: com.thehouse.app.domain.usecase.IssueAgreementCodeUseCase by lazy {
        com.thehouse.app.domain.usecase.IssueAgreementCodeUseCase(agreementsRepository)
    }

    val cancelAgreement: com.thehouse.app.domain.usecase.CancelAgreementUseCase by lazy {
        com.thehouse.app.domain.usecase.CancelAgreementUseCase(agreementsRepository)
    }

    val endAgreement: com.thehouse.app.domain.usecase.EndAgreementUseCase by lazy {
        com.thehouse.app.domain.usecase.EndAgreementUseCase(agreementsRepository)
    }

    val previewAgreement: com.thehouse.app.domain.usecase.PreviewAgreementUseCase by lazy {
        com.thehouse.app.domain.usecase.PreviewAgreementUseCase(agreementsRepository)
    }

    val acceptAgreement: com.thehouse.app.domain.usecase.AcceptAgreementUseCase by lazy {
        com.thehouse.app.domain.usecase.AcceptAgreementUseCase(agreementsRepository)
    }

    val getMyRental: com.thehouse.app.domain.usecase.GetMyRentalUseCase by lazy {
        com.thehouse.app.domain.usecase.GetMyRentalUseCase(agreementsRepository)
    }

    // ===== NV-17 Smart Match ML (on-device, $0) =====

    val smartMatchModel: com.thehouse.app.domain.smartmatch.SmartMatchModel by lazy {
        com.thehouse.app.data.smartmatch.LogisticRegressionSmartMatchModel.loadFromAssets(appContext)
    }

    val runSmartMatch: com.thehouse.app.domain.usecase.RunSmartMatchUseCase by lazy {
        com.thehouse.app.domain.usecase.RunSmartMatchUseCase(smartMatchModel)
    }

    val smartMatchApi: SmartMatchApi by lazy {
        retrofit.create(SmartMatchApi::class.java)
    }

    val smartMatchRepository: SmartMatchRepository by lazy {
        SmartMatchRepository(api = smartMatchApi)
    }

    // ===== NV-15 Privacidad + Eliminar cuenta (D19: contrato Backend real) =====

    val accountApi: com.thehouse.app.data.remote.api.AccountApi by lazy {
        retrofit.create(com.thehouse.app.data.remote.api.AccountApi::class.java)
    }

    val accountRepository: com.thehouse.app.domain.repository.AccountRepository by lazy {
        com.thehouse.app.data.account.repository.AccountRepositoryImpl(api = accountApi, json = json)
    }

    val legalApi: com.thehouse.app.data.remote.api.LegalApi by lazy {
        retrofit.create(com.thehouse.app.data.remote.api.LegalApi::class.java)
    }

    val legalRepository: com.thehouse.app.domain.repository.LegalRepository by lazy {
        com.thehouse.app.data.legal.repository.LegalRepositoryImpl(api = legalApi, json = json)
    }

    /** D19: estado de aceptación legal vigente (se actualiza tras login/restore). */
    val legalUpToDate: kotlinx.coroutines.flow.MutableStateFlow<Boolean> =
        kotlinx.coroutines.flow.MutableStateFlow(true)

    val localUserDataPurger: com.thehouse.app.domain.repository.LocalUserDataPurger by lazy {
        com.thehouse.app.data.account.repository.LocalUserDataPurgerImpl(
            onboardingRepository = onboardingRepository,
            favoritesStore = favoriteIdsStore,
            pendingNotificationNavigation = pendingNotificationNavigation,
            localPushNotifier = localPushNotifier,
            httpCache = httpClient.cache,
            localSessionCleaner = localSessionCleaner,
            dispatchers = dispatchers,
            offlineCacheStore = offlineCacheStore,
        )
    }

    val deleteAccount: com.thehouse.app.domain.usecase.DeleteAccountUseCase by lazy {
        com.thehouse.app.domain.usecase.DeleteAccountUseCase(
            accountRepository = accountRepository,
            localUserDataPurger = localUserDataPurger,
            sessionState = sessionStore.state,
        )
    }

    // ===== NV-14 Fase A — Notificaciones in-app reales =====

    val notificationsApi: com.thehouse.app.data.remote.api.NotificationsApi by lazy {
        retrofit.create(com.thehouse.app.data.remote.api.NotificationsApi::class.java)
    }

    val notificationsRepository: com.thehouse.app.domain.repository.NotificationsRepository by lazy {
        com.thehouse.app.data.notifications.repository.NotificationsRepositoryImpl(
            api = notificationsApi,
            json = json,
        )
    }

    val getNotifications: com.thehouse.app.domain.usecase.GetNotificationsUseCase by lazy {
        com.thehouse.app.domain.usecase.GetNotificationsUseCase(notificationsRepository)
    }

    val getUnreadNotificationsCount: com.thehouse.app.domain.usecase.GetUnreadNotificationsCountUseCase by lazy {
        com.thehouse.app.domain.usecase.GetUnreadNotificationsCountUseCase(notificationsRepository)
    }

    val markNotificationRead: com.thehouse.app.domain.usecase.MarkNotificationReadUseCase by lazy {
        com.thehouse.app.domain.usecase.MarkNotificationReadUseCase(notificationsRepository)
    }

    val markAllNotificationsRead: com.thehouse.app.domain.usecase.MarkAllNotificationsReadUseCase by lazy {
        com.thehouse.app.domain.usecase.MarkAllNotificationsReadUseCase(notificationsRepository)
    }

    // ===== NV-14 Fase B — FCM / Push =====
    // D21: contrato real de devices verificado en Backend
    // `TheHouse.Http/PushDevicesEndpoint.cs` (PUT/GET/DELETE). Ya no existe
    // FCM-TOKEN-BACKEND-BLOCKED: el token se sincroniza con el Backend.

    val applicationScope: CoroutineScope by lazy {
        CoroutineScope(SupervisorJob() + dispatchers.io)
    }

    /** Señal FCM para que Home/Notification Center refresquen estado real del Backend. */
    val pushRefreshSignals: MutableSharedFlow<Unit> by lazy {
        MutableSharedFlow(extraBufferCapacity = 1)
    }

    val pushLogger: com.thehouse.app.domain.push.PushLogger by lazy {
        com.thehouse.app.data.notifications.push.AndroidPushLogger()
    }

    val notificationChannels by lazy {
        com.thehouse.app.data.notifications.push.NotificationChannelRegistry(appContext)
    }

    val appVisibilityTracker by lazy {
        com.thehouse.app.data.notifications.push.AppVisibilityTracker()
    }

    val localPushNotifier: com.thehouse.app.domain.push.LocalPushNotifier by lazy {
        com.thehouse.app.data.notifications.push.AndroidLocalPushNotifier(
            context = appContext,
            channels = notificationChannels,
            logger = pushLogger,
        )
    }

    val pushTokenProvider: com.thehouse.app.domain.push.PushTokenProvider by lazy {
        com.thehouse.app.data.notifications.push.FirebasePushTokenProvider(
            context = appContext,
            logger = pushLogger,
        )
    }

    val pushTokenCoordinator by lazy {
        com.thehouse.app.domain.push.PushTokenCoordinator(
            provider = pushTokenProvider,
            logger = pushLogger,
        )
    }

    // ===== D21: installationId + registro de dispositivos =====

    val installationIdStore: com.thehouse.app.domain.repository.InstallationIdStore by lazy {
        com.thehouse.app.data.device.DataStoreInstallationIdStore(preferences)
    }

    val deviceApi: com.thehouse.app.data.remote.api.DeviceApi by lazy {
        retrofit.create(com.thehouse.app.data.remote.api.DeviceApi::class.java)
    }

    val deviceRepository: com.thehouse.app.domain.repository.DeviceRepository by lazy {
        com.thehouse.app.data.device.DeviceRepositoryImpl(deviceApi)
    }

    val registerDevice: com.thehouse.app.domain.usecase.RegisterDeviceUseCase by lazy {
        com.thehouse.app.domain.usecase.RegisterDeviceUseCase(
            repository = deviceRepository,
            installationIdStore = installationIdStore,
            sessionState = { sessionStore.state.value },
            appVersion = { com.thehouse.app.BuildConfig.VERSION_NAME },
        )
    }

    val unregisterDevice: com.thehouse.app.domain.usecase.UnregisterDeviceUseCase by lazy {
        com.thehouse.app.domain.usecase.UnregisterDeviceUseCase(
            repository = deviceRepository,
            installationIdStore = installationIdStore,
        )
    }

    val deviceSyncCoordinator: com.thehouse.app.domain.device.DeviceSyncCoordinator by lazy {
        com.thehouse.app.domain.device.DeviceSyncCoordinator(
            repository = deviceRepository,
            installationIdStore = installationIdStore,
            sessionState = sessionStore.state,
            connectivity = connectivityObserver,
            appScope = applicationScope,
            tokenProvider = { pushTokenCoordinator.currentTokenForBackendSync() },
            tokenClear = { pushTokenCoordinator.clear() },
            logger = { event -> pushLogger.info(event) },
        ).also { coordinator ->
            // onNewToken (incluida rotación FCM) dispara sincronización.
            pushTokenCoordinator.onTokenChanged = { coordinator.onNewToken() }
        }
    }

    val logout: com.thehouse.app.domain.usecase.D21LogoutUseCase by lazy {
        com.thehouse.app.domain.usecase.D21LogoutUseCase(
            repository = authRepository,
            deviceSync = deviceSyncCoordinator,
        )
    }

    val moderationErrorParser: com.thehouse.app.data.moderation.ModerationErrorParser
        get() = com.thehouse.app.data.moderation.ModerationErrorParser

    val pushSessionGate: com.thehouse.app.domain.push.PushSessionGate by lazy {
        com.thehouse.app.data.notifications.push.PersistentPushSessionGate(
            sessionStore = sessionStore,
            restoreSession = restoreSession,
        )
    }

    val pushMessageHandler by lazy {
        com.thehouse.app.data.notifications.push.PushMessageHandler(
            parser = com.thehouse.app.domain.push.PushPayloadParser,
            sessionGate = pushSessionGate,
            visibility = appVisibilityTracker,
            localNotifier = localPushNotifier,
            refreshSignals = pushRefreshSignals,
            logger = pushLogger,
        )
    }

    val pendingNotificationNavigation by lazy {
        com.thehouse.app.data.notifications.push.PendingNotificationNavigation()
    }

    val pushNavigationIntentHandler by lazy {
        com.thehouse.app.data.notifications.push.AndroidPushNavigationIntentHandler(
            pendingNavigation = pendingNotificationNavigation,
            logger = pushLogger,
        )
    }

    /**
     * Observa transiciones de sesión relevantes para push:
     * - Active → no Active: cancela notificaciones locales y target pendiente
     *   (evita que datos de una sesión anterior queden visibles).
     * - → Active: obtiene/refresca token FCM (sin registrarlo en Backend).
     */
    fun observePushSession() {
        // D21: inicializar el coordinador para registrar el callback de onNewToken
        // antes de que Firebase pueda entregar una rotación de token.
        deviceSyncCoordinator
        applicationScope.launch {
            var previous: SessionState? = null
            sessionStore.state.collect { current ->
                val wasActive = previous is SessionState.Active
                if (wasActive && current !is SessionState.Active) {
                    val previousUserId = (previous as? SessionState.Active)?.session?.userId
                    if (!previousUserId.isNullOrBlank()) {
                        runCatching {
                            OfflineCacheStore.Keys.privateKeysFor(previousUserId).forEach { key ->
                                offlineCacheStore.remove(key)
                            }
                        }
                    }
                    pendingNotificationNavigation.clear()
                    runCatching { localPushNotifier.cancelAll() }
                }
                if (current is SessionState.Active && !wasActive) {
                    pushTokenCoordinator.refresh()
                    // D21: sincronizar dispositivo tras restauración/login.
                    deviceSyncCoordinator.onSessionRestored()
                }
                previous = current
            }
        }
    }
}