package com.thehouse.app.presentation.home

import com.thehouse.app.domain.auth.AccessPolicy
import com.thehouse.app.domain.auth.ProtectedAction
import com.thehouse.app.domain.model.Property
import com.thehouse.app.domain.model.SessionState
import com.thehouse.app.domain.model.University
import com.thehouse.app.domain.model.UnreadCountFetch
import com.thehouse.app.domain.repository.SelectedUniversityRepository
import com.thehouse.app.domain.room.RoomSearchFilters
import com.thehouse.app.domain.usecase.FavoriteStateStore
import com.thehouse.app.domain.usecase.GetPropertiesUseCase
import com.thehouse.app.domain.usecase.GetUniversitiesUseCase
import com.thehouse.app.domain.usecase.GetUnreadNotificationsCountUseCase
import com.thehouse.app.domain.usecase.LogoutUseCase
import com.thehouse.app.domain.usecase.SearchRoomsUseCase
import com.thehouse.app.domain.usecase.SelectUniversityUseCase
import com.thehouse.app.domain.usecase.ToggleFavoriteUseCase
import com.thehouse.app.presentation.catalog.CatalogLoadState
import com.thehouse.app.presentation.catalog.RoomSearchState
import com.thehouse.app.presentation.catalog.toCatalogState
import com.thehouse.app.presentation.catalog.toRoomSearchStateError
import com.thehouse.app.presentation.common.BaseViewModel
import com.thehouse.app.presentation.common.UiEffect
import com.thehouse.app.presentation.common.UiEvent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow

/**
 * MVVM + UDF del Home (D4/D5 + D19.1 + D20).
 *
 * - Universidad guardada en Perfil o seleccionada en modo guest (sin hardcode).
 * - D20: recommended reales vía `GET /api/v1/rooms/search` (roomId real).
 * - Legacy `properties` se mantiene SOLO para tests/previa que aún inyectan
 *   `getProperties`; producción usa `rooms`.
 * - Estado compartido de favoritos por roomId; guest → RequireLogin.
 */
data class HomeUiState(
    val displayName: String = "",
    val email: String = "",
    val isLoggingOut: Boolean = false,
    val isGuest: Boolean = false,
    val isAuthenticated: Boolean = false,
    val universities: CatalogLoadState<University> = CatalogLoadState.Loading,
    val properties: CatalogLoadState<Property> = CatalogLoadState.Loading,
    val rooms: RoomSearchState = RoomSearchState.Loading,
    val selectedUniversityId: String? = null,
    val featuredProperty: Property? = null,
    val propertiesVersion: Int = 0,
    val universitiesVersion: Int = 0,
    val unreadNotifications: Int = 0,
    val favoriteRoomIds: Set<String> = emptySet(),
)

sealed interface HomeEvent : UiEvent {
    data object LogoutClicked : HomeEvent
    data object FavoriteClicked : HomeEvent
    data class FavoriteRoomClicked(val roomId: String) : HomeEvent
    data object ContactClicked : HomeEvent
    data object PublishClicked : HomeEvent
    data object RetryUniversitiesClicked : HomeEvent
    data object RetryPropertiesClicked : HomeEvent
    data object RefreshRoomsClicked : HomeEvent
    data object LoadMoreRoomsClicked : HomeEvent
    data class UniversitySelected(val universityId: String) : HomeEvent
    data object RefreshUnread : HomeEvent
    data object StartUnreadPolling : HomeEvent
    data object StopUnreadPolling : HomeEvent
}

sealed interface HomeEffect : UiEffect {
    data object LoggedOut : HomeEffect
    data object RequireLogin : HomeEffect
    data class Message(val text: String) : HomeEffect
}

class HomeViewModel(
    private val logoutUseCase: LogoutUseCase,
    private val sessionState: StateFlow<SessionState> = MutableStateFlow(SessionState.Anonymous),
    private val getUniversities: GetUniversitiesUseCase? = null,
    private val getProperties: GetPropertiesUseCase? = null,
    private val selectedRepository: SelectedUniversityRepository? = null,
    private val selectUniversity: SelectUniversityUseCase? = null,
    private val getUnreadNotificationsCount: GetUnreadNotificationsCountUseCase? = null,
    private val pushRefreshSignals: Flow<Unit> = emptyFlow(),
    /** D19.1: universidad guardada en Perfil (fuente de verdad remota). */
    private val profileUniversityState: Flow<com.thehouse.app.domain.model.University?>? = null,
    private val refreshProfile: suspend () -> Unit = {},
    /** D20: búsqueda real de habitaciones. */
    private val searchRooms: SearchRoomsUseCase? = null,
    private val favoriteStateStore: FavoriteStateStore? = null,
    private val toggleFavorite: ToggleFavoriteUseCase? = null,
) : BaseViewModel<HomeUiState, HomeEvent, HomeEffect>(HomeUiState()) {

    private var unreadEtag: String? = null
    private var unreadPollingJob: Job? = null
    private var unreadRefreshJob: Job? = null
    private var roomsJob: Job? = null
    private var searchGeneration = 0
    private var lastLoadedUniversity: String? = null
    private val searchEnabled = searchRooms != null

    override fun onCleared() {
        unreadPollingJob?.cancel()
        roomsJob?.cancel()
        super.onCleared()
    }

    init {
        updateState {
            val isGuest = sessionState.value is SessionState.Anonymous
            it.copy(isGuest = isGuest, isAuthenticated = !isGuest, favoriteRoomIds = favoriteStateStore?.roomIds?.value.orEmpty())
        }
        if (selectedRepository != null) {
            launch {
                selectedRepository.loadSelected()
                updateState { it.copy(selectedUniversityId = selectedRepository.selectedUniversityId.value) }
                onUniversityResolved()
            }
        }
        if (sessionState.value is com.thehouse.app.domain.model.SessionState.Active) {
            launch {
                runCatching { refreshProfile() }
                profileUniversityState?.collect { university ->
                    if (university != null) {
                        updateState { it.copy(selectedUniversityId = university.id) }
                        onUniversityResolved()
                    }
                }
            }
        }
        favoriteStateStore?.roomIds?.let { flow ->
            launch { flow.collect { ids -> updateState { it.copy(favoriteRoomIds = ids) } } }
        }
        if (searchEnabled && uiState.value.selectedUniversityId.isNullOrBlank()) {
            updateState { it.copy(rooms = RoomSearchState.Empty) }
        }
        loadUniversities()
        if (!searchEnabled) loadProperties()
        launch {
            pushRefreshSignals.collect {
                if (sessionState.value is SessionState.Active) refreshUnread()
            }
        }
    }

    override fun onEvent(event: HomeEvent) {
        when (event) {
            HomeEvent.LogoutClicked -> logout()
            HomeEvent.FavoriteClicked -> guard(ProtectedAction.SaveFavorite)
            is HomeEvent.FavoriteRoomClicked -> toggleRoomFavorite(event.roomId)
            HomeEvent.ContactClicked -> guard(ProtectedAction.Contact)
            HomeEvent.PublishClicked -> guard(ProtectedAction.Publish)
            HomeEvent.RetryUniversitiesClicked -> loadUniversities()
            HomeEvent.RetryPropertiesClicked -> {
                if (searchEnabled) loadRooms(refresh = true) else loadProperties()
            }
            HomeEvent.RefreshRoomsClicked -> loadRooms(refresh = true)
            HomeEvent.LoadMoreRoomsClicked -> loadMoreRooms()
            is HomeEvent.UniversitySelected -> select(event.universityId)
            HomeEvent.RefreshUnread -> refreshUnread()
            HomeEvent.StartUnreadPolling -> startUnreadPolling()
            HomeEvent.StopUnreadPolling -> stopUnreadPolling()
        }
    }

    private fun refreshUnread() {
        val useCase = getUnreadNotificationsCount ?: return
        if (sessionState.value !is SessionState.Active) {
            updateState { it.copy(unreadNotifications = 0) }
            return
        }
        if (unreadRefreshJob?.isActive == true) return
        unreadRefreshJob = launch {
            try {
                val result = useCase(unreadEtag)
                result.fold(
                    onSuccess = { fetch ->
                        when (fetch) {
                            is UnreadCountFetch.NotModified -> Unit
                            is UnreadCountFetch.Modified -> {
                                unreadEtag = fetch.etag
                                updateState { it.copy(unreadNotifications = fetch.count.unread.coerceAtLeast(0)) }
                            }
                        }
                    },
                    onFailure = { /* mantener estado actual */ },
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            }
        }
    }

    private fun startUnreadPolling() {
        if (getUnreadNotificationsCount == null) return
        if (sessionState.value !is SessionState.Active) return
        if (unreadPollingJob?.isActive == true) return
        unreadPollingJob = launch {
            try {
                while (true) {
                    refreshUnread()
                    delay(30_000L)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            }
        }
    }

    private fun stopUnreadPolling() {
        unreadPollingJob?.cancel()
        unreadPollingJob = null
    }

    fun loadUniversities() {
        val useCase = getUniversities
        if (useCase == null) {
            updateState { it.copy(universities = CatalogLoadState.Empty) }
            return
        }
        updateState { it.copy(universities = CatalogLoadState.Loading) }
        launch {
            val result = useCase().map { page -> page.items }
            updateState {
                it.copy(
                    universities = result.toCatalogState(),
                    universitiesVersion = it.universitiesVersion + 1,
                )
            }
        }
    }

    fun loadProperties() {
        val useCase = getProperties
        if (useCase == null) {
            updateState { it.copy(properties = CatalogLoadState.Empty) }
            return
        }
        updateState { it.copy(properties = CatalogLoadState.Loading) }
        launch {
            val result = useCase().map { page -> page.items }
            val state = result.toCatalogState()
            val featured = (state as? CatalogLoadState.Success)?.items?.firstOrNull()
            updateState {
                it.copy(
                    properties = state,
                    featuredProperty = featured,
                    propertiesVersion = it.propertiesVersion + 1,
                )
            }
        }
    }

    /** D20: carga/refresca habitaciones reales para la universidad vigente. */
    fun loadRooms(refresh: Boolean = false) {
        val useCase = searchRooms ?: return
        val universityId = uiState.value.selectedUniversityId
        if (universityId.isNullOrBlank()) {
            updateState { it.copy(rooms = RoomSearchState.Empty) }
            return
        }
        // Single-flight + generación: evita duplicados y respuestas obsoletas.
        if (roomsJob?.isActive == true && !refresh) return
        roomsJob?.cancel()
        val generation = ++searchGeneration
        lastLoadedUniversity = universityId
        updateState {
            val previous = it.rooms
            it.copy(
                rooms = if (refresh && previous is RoomSearchState.Content) {
                    previous.copy(isRefreshing = true)
                } else RoomSearchState.Loading,
            )
        }
        roomsJob = launch {
            val result = useCase(RoomSearchFilters(universityId = universityId))
            if (generation != searchGeneration) return@launch
            result.fold(
                onSuccess = { page ->
                    updateState {
                        it.copy(
                            rooms = if (page.items.isEmpty()) RoomSearchState.Empty else RoomSearchState.Content(
                                items = page.items,
                                page = page.page,
                                totalPages = page.totalPages,
                                totalItems = page.totalItems,
                            ),
                        )
                    }
                },
                onFailure = { updateState { it.copy(rooms = result.toRoomSearchStateError()) } },
            )
        }
    }

    fun loadMoreRooms() {
        val useCase = searchRooms ?: return
        val current = uiState.value.rooms as? RoomSearchState.Content ?: return
        val universityId = uiState.value.selectedUniversityId ?: return
        if (current.isPaginating || current.page >= current.totalPages) return
        val generation = searchGeneration
        updateState { it.copy(rooms = current.copy(isPaginating = true, paginationError = null)) }
        launch {
            val result = useCase(
                RoomSearchFilters(universityId = universityId, page = current.page + 1),
            )
            if (generation != searchGeneration) return@launch
            result.fold(
                onSuccess = { page ->
                    updateState {
                        val state = it.rooms as? RoomSearchState.Content ?: return@updateState it
                        it.copy(
                            rooms = state.copy(
                                items = state.items + page.items,
                                page = page.page,
                                totalPages = page.totalPages,
                                totalItems = page.totalItems,
                                isPaginating = false,
                            ),
                        )
                    }
                },
                onFailure = {
                    updateState {
                        val state = it.rooms as? RoomSearchState.Content ?: return@updateState it
                        it.copy(rooms = state.copy(isPaginating = false, paginationError = result.toRoomSearchStateError().error))
                    }
                },
            )
        }
    }

    private fun onUniversityResolved() {
        val id = uiState.value.selectedUniversityId
        if (searchEnabled && !id.isNullOrBlank() && id != lastLoadedUniversity) loadRooms()
    }

    private fun toggleRoomFavorite(roomId: String) {
        val useCase = toggleFavorite
        if (useCase == null) {
            guard(ProtectedAction.SaveFavorite)
            return
        }
        if (!uiState.value.isAuthenticated) {
            sendEffect(HomeEffect.RequireLogin)
            return
        }
        launch {
            when (val result = useCase(roomId)) {
                is com.thehouse.app.domain.usecase.FavoriteToggleResult.Added ->
                    sendEffect(HomeEffect.Message("Guardado en favoritos"))
                is com.thehouse.app.domain.usecase.FavoriteToggleResult.Removed ->
                    sendEffect(HomeEffect.Message("Quitado de favoritos"))
                is com.thehouse.app.domain.usecase.FavoriteToggleResult.Failed ->
                    if (result.error == com.thehouse.app.domain.error.DomainError.Unauthorized) {
                        sendEffect(HomeEffect.RequireLogin)
                    } else {
                        sendEffect(HomeEffect.Message("No se pudo actualizar el favorito"))
                    }
            }
        }
    }

    private fun select(universityId: String) {
        launch {
            selectUniversity?.invoke(universityId)
            selectedRepository?.selectedUniversityId?.value?.let { id ->
                updateState { it.copy(selectedUniversityId = id) }
            } ?: updateState { it.copy(selectedUniversityId = universityId) }
            onUniversityResolved()
        }
    }

    fun guard(action: ProtectedAction) {
        val isAuthenticated = sessionState.value is SessionState.Active
        updateState { it.copy(isGuest = !isAuthenticated, isAuthenticated = isAuthenticated) }
        if (AccessPolicy.shouldRedirectToLogin(action, isAuthenticated)) {
            sendEffect(HomeEffect.RequireLogin)
        }
    }

    fun logout() {
        if (uiState.value.isLoggingOut) return
        launch {
            updateState { it.copy(isLoggingOut = true) }
            logoutUseCase()
            updateState { it.copy(isLoggingOut = false) }
            sendEffect(HomeEffect.LoggedOut)
        }
    }
}
