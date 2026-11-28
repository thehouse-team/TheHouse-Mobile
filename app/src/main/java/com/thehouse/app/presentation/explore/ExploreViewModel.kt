package com.thehouse.app.presentation.explore

import com.thehouse.app.domain.connectivity.ConnectivityObserver
import com.thehouse.app.domain.model.Property
import com.thehouse.app.domain.model.SessionState
import com.thehouse.app.domain.model.University
import com.thehouse.app.domain.repository.SelectedUniversityRepository
import com.thehouse.app.domain.room.FilterDraftParser
import com.thehouse.app.domain.room.RoomSearchCatalog
import com.thehouse.app.domain.room.RoomSearchFilters
import com.thehouse.app.domain.usecase.FavoriteStateStore
import com.thehouse.app.domain.usecase.ToggleFavoriteUseCase
import com.thehouse.app.domain.usecase.GetPropertiesUseCase
import com.thehouse.app.domain.usecase.SearchPropertiesUseCase
import com.thehouse.app.domain.usecase.SearchRoomsUseCase
import com.thehouse.app.presentation.catalog.CatalogLoadState
import com.thehouse.app.presentation.catalog.CatalogFlowLogger
import com.thehouse.app.presentation.catalog.RoomSearchState
import com.thehouse.app.presentation.catalog.toCatalogState
import com.thehouse.app.presentation.catalog.toRoomSearchStateError
import com.thehouse.app.presentation.common.BaseViewModel
import com.thehouse.app.presentation.common.UiEffect
import com.thehouse.app.presentation.common.UiEvent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * MVVM + UDF de Explorar (D5 + D20 + hotfix).
 * D20 hotfix: la universidad se resuelve desde el repositorio local y desde el
 * Perfil remoto (auth), con observación continua. Nunca se queda en Loading:
 * sin universidad → Empty con selector; con universidad → rooms/search real.
 */
data class ExploreFiltersUi(
    val universityId: String? = null,
    val minPrice: Int? = null,
    val maxPrice: Int? = null,
    val roomType: String? = null,
    val bathroom: String? = null,
    val buildingType: String? = null,
    val allowedTenants: String? = null,
    val services: List<String> = emptyList(),
    val maxDistanceKm: Double = RoomSearchCatalog.DEFAULT_DISTANCE_KM,
    val sort: String = "distance",
)

/**
 * Borrador del panel de filtros. Los campos numéricos se guardan como texto
 * para que la edición parcial nunca se convierta en cero ni se pierda.
 * La conversión ocurre una sola vez, al pulsar «Aplicar filtros».
 */
data class FilterDraftUi(
    val minPriceRaw: String = "",
    val maxPriceRaw: String = "",
    val distanceRaw: String = RoomSearchCatalog.DEFAULT_DISTANCE_KM.toString(),
    val base: ExploreFiltersUi = ExploreFiltersUi(),
    val validationError: String? = null,
) {
    fun toFilters(): Pair<ExploreFiltersUi, String?> {
        val min = FilterDraftParser.parsePrice(minPriceRaw, "Precio mínimo")
        val max = FilterDraftParser.parsePrice(maxPriceRaw, "Precio máximo")
        val distance = FilterDraftParser.parseDistance(distanceRaw)
        val error = listOfNotNull(
            min.error,
            max.error,
            distance.error,
            FilterDraftParser.priceRangeError(min.value, max.value),
        ).firstOrNull()
        if (error != null) return base to error
        return base.copy(
            minPrice = min.value,
            maxPrice = max.value,
            maxDistanceKm = distance.value ?: RoomSearchCatalog.DEFAULT_DISTANCE_KM,
        ) to null
    }

    companion object {
        fun from(filters: ExploreFiltersUi) = FilterDraftUi(
            minPriceRaw = filters.minPrice?.toString().orEmpty(),
            maxPriceRaw = filters.maxPrice?.toString().orEmpty(),
            distanceRaw = filters.maxDistanceKm.toString(),
            base = filters,
        )
    }
}

data class ExploreUiState(
    val query: String = "",
    val properties: CatalogLoadState<Property> = CatalogLoadState.Empty,
    val allProperties: List<Property> = emptyList(),
    val isSearching: Boolean = false,
    val rooms: RoomSearchState = RoomSearchState.Loading,
    val filters: ExploreFiltersUi = ExploreFiltersUi(),
    val favoriteRoomIds: Set<String> = emptySet(),
    val showFilters: Boolean = false,
    val universitySource: String = "none",
    val isAuthenticated: Boolean = false,
    val universities: List<University> = emptyList(),
    /** Borrador: los campos se editan sin lanzar petición hasta "Aplicar". */
    val filterDraft: FilterDraftUi = FilterDraftUi(),
    val filterValidationError: String? = null,
) {
    fun visibleRooms(): List<com.thehouse.app.domain.room.RoomSearchItem> {
        val items = (rooms as? RoomSearchState.Content)?.items.orEmpty()
        val q = query.trim()
        if (q.isBlank()) return items
        return items.filter {
            it.title.contains(q, ignoreCase = true) ||
                it.buildingType.contains(q, ignoreCase = true) ||
                it.roomType?.contains(q, ignoreCase = true) == true ||
                it.services.any { s -> s.contains(q, ignoreCase = true) }
        }
    }
}

sealed interface ExploreEvent : UiEvent {
    data object RetryClicked : ExploreEvent
    data class QueryChanged(val query: String) : ExploreEvent
    data class UniversityChanged(val universityId: String) : ExploreEvent
    data class FilterChanged(val filters: ExploreFiltersUi) : ExploreEvent
    /** Edición del borrador (texto crudo): NO dispara petición. */
    data class DraftFilterChanged(val draft: FilterDraftUi) : ExploreEvent
    data object ApplyFilters : ExploreEvent
    data object ClearFilters : ExploreEvent
    data object ToggleFilters : ExploreEvent
    data object RefreshRoomsClicked : ExploreEvent
    data object LoadMoreRoomsClicked : ExploreEvent
    data class FavoriteRoomClicked(val roomId: String) : ExploreEvent
}

sealed interface ExploreEffect : UiEffect {
    data object RequireLogin : ExploreEffect
    data class Message(val text: String) : ExploreEffect
}

class ExploreViewModel(
    private val getProperties: GetPropertiesUseCase? = null,
    private val searchProperties: SearchPropertiesUseCase = SearchPropertiesUseCase(),
    connectivityObserver: ConnectivityObserver? = null,
    private val searchRooms: SearchRoomsUseCase? = null,
    private val favoriteStateStore: FavoriteStateStore? = null,
    private val toggleFavorite: ToggleFavoriteUseCase? = null,
    private val selectedRepository: SelectedUniversityRepository? = null,
    private val profileUniversityState: Flow<University?>? = null,
    private val sessionState: StateFlow<SessionState> = MutableStateFlow(SessionState.Anonymous),
    private val getUniversities: com.thehouse.app.domain.usecase.GetUniversitiesUseCase? = null,
) : BaseViewModel<ExploreUiState, ExploreEvent, ExploreEffect>(ExploreUiState()) {

    val isOnline: StateFlow<Boolean> = connectivityObserver?.isOnline ?: MutableStateFlow(true)
    private var roomsJob: Job? = null
    private var sessionJob: Job? = null
    private var searchGeneration = 0
    private var lastLoadedFiltersKey: String? = null
    private val searchEnabled = searchRooms != null

    init {
        CatalogFlowLogger.explore("viewmodel_created", "searchEnabled", searchEnabled)
        if (!searchEnabled) load()

        favoriteStateStore?.roomIds?.let { flow ->
            launch { flow.collect { ids -> updateState { it.copy(favoriteRoomIds = ids) } } }
        }

        // Universidad local (guest): StateFlow + carga inicial.
        if (selectedRepository != null) {
            launch {
                selectedRepository.loadSelected()
                selectedRepository.selectedUniversityId.collect { id ->
                    if (!id.isNullOrBlank()) {
                        CatalogFlowLogger.explore("university_available", "source", "local_preference")
                        applyUniversity(id, "local_preference")
                    }
                }
            }
        }

        // Universidad remota (Perfil auth): observada siempre, con sesión asíncrona.
        if (profileUniversityState != null) {
            launch {
                profileUniversityState.collect { university ->
                    if (university != null && university.id.isNotBlank()) {
                        CatalogFlowLogger.explore("university_available", "source", "profile")
                        applyUniversity(university.id, "profile")
                    }
                }
            }
        }

        sessionJob = launch {
            sessionState.collect { session ->
                val active = session is SessionState.Active
                updateState { it.copy(isAuthenticated = active) }
                CatalogFlowLogger.explore("session_state", "active", active)
            }
        }

        if (searchEnabled && uiState.value.filters.universityId.isNullOrBlank()) {
            CatalogFlowLogger.explore("state_transition", "to", "Empty", "reason", "no_university")
            updateState { it.copy(rooms = RoomSearchState.Empty) }
        }

        if (getUniversities != null) {
            launch {
                val result = try {
                    getUniversities()
                } catch (cancelled: kotlinx.coroutines.CancellationException) {
                    return@launch
                } catch (network: java.io.IOException) {
                    Result.failure(com.thehouse.app.domain.error.DomainException(
                        com.thehouse.app.domain.error.DomainError.Network,
                    ))
                }
                result.onSuccess { page ->
                    updateState { it.copy(universities = page.items) }
                }
            }
        }
    }

    override fun onCleared() {
        roomsJob?.cancel()
        sessionJob?.cancel()
        super.onCleared()
    }

    override fun onEvent(event: ExploreEvent) {
        CatalogFlowLogger.explore("load_trigger", "event", event::class.simpleName.orEmpty())
        when (event) {
            ExploreEvent.RetryClicked -> if (searchEnabled) loadRooms(refresh = true) else load()
            is ExploreEvent.QueryChanged -> {
                updateState { it.copy(query = event.query) }
                if (!searchEnabled) applyQuery(event.query)
            }
            is ExploreEvent.UniversityChanged -> {
                applyUniversity(event.universityId, "user_selection")
            }
            is ExploreEvent.FilterChanged -> {
                // Los chips rápidos se basan en los filtros APLICADOS: un borrador
                // sucio nunca debe colarse en una búsqueda que el usuario no pidió.
                updateState {
                    it.copy(
                        filters = event.filters,
                        filterDraft = FilterDraftUi.from(event.filters),
                        filterValidationError = null,
                    )
                }
                loadRooms(refresh = true)
            }
            is ExploreEvent.DraftFilterChanged -> updateState {
                it.copy(filterDraft = event.draft, filterValidationError = null)
            }
            ExploreEvent.ApplyFilters -> {
                val (candidate, error) = uiState.value.filterDraft.toFilters()
                if (error != null) {
                    // Sin petición: el usuario debe corregir antes de aplicar.
                    updateState { it.copy(filterValidationError = error) }
                    return
                }
                updateState { it.copy(filters = candidate, filterValidationError = null) }
                loadRooms(refresh = true)
            }
            ExploreEvent.ClearFilters -> {
                val currentUniversity = uiState.value.filters.universityId
                val cleared = ExploreFiltersUi(universityId = currentUniversity)
                val current = uiState.value.filters
                val unchanged = cleared == current
                updateState {
                    it.copy(
                        filters = cleared,
                        filterDraft = FilterDraftUi.from(cleared),
                        filterValidationError = null,
                    )
                }
                // Evita peticiones duplicadas cuando no había nada que limpiar.
                if (!unchanged) loadRooms(refresh = true)
            }
            ExploreEvent.ToggleFilters -> {
                // Al abrir, sincroniza el borrador con los filtros aplicados.
                // Al cerrar sin aplicar, NO se toca la búsqueda activa.
                updateState {
                    it.copy(
                        showFilters = !it.showFilters,
                        filterDraft = if (!it.showFilters) {
                            FilterDraftUi.from(it.filters)
                        } else it.filterDraft,
                        filterValidationError = null,
                    )
                }
            }
            ExploreEvent.RefreshRoomsClicked -> loadRooms(refresh = true)
            ExploreEvent.LoadMoreRoomsClicked -> loadMoreRooms()
            is ExploreEvent.FavoriteRoomClicked -> toggleRoomFavorite(event.roomId)
        }
    }

    private fun applyUniversity(id: String, source: String) {
        val current = uiState.value
        if (current.filters.universityId == id && lastLoadedFiltersKey?.startsWith(id) == true) return
        updateState {
            it.copy(
                universitySource = source,
                filters = it.filters.copy(universityId = id),
                filterDraft = it.filterDraft.copy(
                    base = it.filterDraft.base.copy(universityId = id),
                ),
            )
        }
        loadRooms(refresh = true)
    }

    fun load() {
        val useCase = getProperties ?: return
        updateState { it.copy(properties = CatalogLoadState.Loading) }
        launch {
            val result = useCase().map { page -> page.items }
            val state = result.toCatalogState()
            val all = if (state is CatalogLoadState.Success) state.items else emptyList()
            updateState { it.copy(properties = state, allProperties = all) }
        }
    }

    fun loadRooms(refresh: Boolean = false) {
        val useCase = searchRooms
        if (useCase == null) {
            updateState { it.copy(rooms = RoomSearchState.Empty) }
            return
        }
        CatalogFlowLogger.explore("repository_available", "value", true)
        val filters = uiState.value.filters
        if (filters.universityId.isNullOrBlank()) {
            CatalogFlowLogger.explore("state_transition", "to", "Empty", "reason", "no_university")
            updateState { it.copy(rooms = RoomSearchState.Empty) }
            return
        }
        val key = filtersKey(filters, 1)
        if (!refresh && key == lastLoadedFiltersKey && roomsJob?.isActive == true) return
        roomsJob?.cancel()
        val generation = ++searchGeneration
        lastLoadedFiltersKey = key
        CatalogFlowLogger.explore(
            "search_start", "source", uiState.value.universitySource,
            "roomType", filters.roomType ?: "null", "sort", filters.sort, "refresh", refresh,
        )
        updateState {
            val previous = it.rooms
            it.copy(
                rooms = if (refresh && previous is RoomSearchState.Content) {
                    previous.copy(isRefreshing = true)
                } else RoomSearchState.Loading,
            )
        }
        roomsJob = launch {
            try {
                val result = useCase(filters.toDomain(page = 1))
                if (generation != searchGeneration) {
                    CatalogFlowLogger.search("search_cancelled", "generation", generation)
                    return@launch
                }
                CatalogFlowLogger.search(
                    "search_result", "success", result.isSuccess,
                    "error_type", result.exceptionOrNull()?.let { it::class.simpleName } ?: "none",
                )
                result.fold(
                    onSuccess = { page ->
                        CatalogFlowLogger.search(
                            "state_transition", "to", if (page.items.isEmpty()) "Empty" else "Content",
                            "count", page.items.size, "total", page.totalItems,
                        )
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
                    onFailure = {
                        CatalogFlowLogger.search("state_transition", "to", "Error", "exception_type", it::class.simpleName.orEmpty())
                        updateState { it.copy(rooms = result.toRoomSearchStateError()) }
                    },
                )
            } catch (ce: CancellationException) {
                CatalogFlowLogger.search("search_cancelled", "reason", "cancellation")
                throw ce
            }
        }
    }

    fun loadMoreRooms() {
        val useCase = searchRooms ?: return
        val current = uiState.value.rooms as? RoomSearchState.Content ?: return
        if (current.isPaginating || current.page >= current.totalPages) return
        val generation = searchGeneration
        val filters = uiState.value.filters
        updateState { it.copy(rooms = current.copy(isPaginating = true, paginationError = null)) }
        launch {
            val result = useCase(filters.toDomain(page = current.page + 1))
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
                        it.copy(
                            rooms = state.copy(
                                isPaginating = false,
                                paginationError = result.toRoomSearchStateError().error,
                            ),
                        )
                    }
                },
            )
        }
    }

    private fun toggleRoomFavorite(roomId: String) {
        val useCase = toggleFavorite
        if (useCase == null) {
            sendEffect(ExploreEffect.RequireLogin)
            return
        }
        launch {
            when (val result = useCase(roomId)) {
                is com.thehouse.app.domain.usecase.FavoriteToggleResult.Added ->
                    sendEffect(ExploreEffect.Message("Guardado en favoritos"))
                is com.thehouse.app.domain.usecase.FavoriteToggleResult.Removed ->
                    sendEffect(ExploreEffect.Message("Quitado de favoritos"))
                is com.thehouse.app.domain.usecase.FavoriteToggleResult.Failed ->
                    if (result.error == com.thehouse.app.domain.error.DomainError.Unauthorized) {
                        sendEffect(ExploreEffect.RequireLogin)
                    } else {
                        sendEffect(ExploreEffect.Message("No se pudo actualizar el favorito"))
                    }
            }
        }
    }

    private fun applyQuery(query: String) {
        val all = uiState.value.allProperties
        updateState {
            val filtered = searchProperties(all, query)
            val state: CatalogLoadState<Property> = when {
                filtered.isNotEmpty() -> CatalogLoadState.Success(filtered)
                query.isBlank() -> it.properties
                else -> CatalogLoadState.Empty
            }
            it.copy(query = query, properties = state)
        }
    }
}

private fun filtersKey(filters: ExploreFiltersUi, page: Int): String = listOf(
    filters.universityId, filters.minPrice, filters.maxPrice, filters.roomType,
    filters.bathroom, filters.buildingType, filters.allowedTenants,
    filters.services.joinToString(","), filters.maxDistanceKm, filters.sort, page,
).joinToString("|")

private fun ExploreFiltersUi.toDomain(page: Int) = RoomSearchFilters(
    universityId = universityId.orEmpty(),
    minPrice = minPrice,
    maxPrice = maxPrice,
    roomType = roomType,
    bathroom = bathroom,
    buildingType = buildingType,
    allowedTenants = allowedTenants,
    services = services,
    maxDistanceKm = maxDistanceKm,
    sort = sort,
    page = page,
)
