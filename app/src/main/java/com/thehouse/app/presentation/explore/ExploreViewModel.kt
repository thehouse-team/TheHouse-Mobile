package com.thehouse.app.presentation.explore

import com.thehouse.app.domain.auth.AccessPolicy
import com.thehouse.app.domain.auth.ProtectedAction
import com.thehouse.app.domain.error.DomainError
import com.thehouse.app.domain.error.DomainException
import com.thehouse.app.domain.model.GeoPoint
import com.thehouse.app.domain.model.MapBounds
import com.thehouse.app.domain.model.PlaceSuggestion
import com.thehouse.app.domain.model.RoomMarker
import com.thehouse.app.domain.model.RoomSearchFilters
import com.thehouse.app.domain.model.RoomSummary
import com.thehouse.app.domain.model.SessionState
import com.thehouse.app.domain.model.University
import com.thehouse.app.domain.repository.SelectedUniversityRepository
import com.thehouse.app.domain.usecase.GetRoomMapUseCase
import com.thehouse.app.domain.usecase.GetUniversitiesUseCase
import com.thehouse.app.domain.usecase.SearchPlacesUseCase
import com.thehouse.app.domain.usecase.SearchRoomsUseCase
import com.thehouse.app.domain.usecase.SelectUniversityUseCase
import com.thehouse.app.presentation.catalog.CatalogLoadState
import com.thehouse.app.presentation.catalog.toCatalogState
import com.thehouse.app.presentation.common.BaseViewModel
import com.thehouse.app.presentation.common.UiEffect
import com.thehouse.app.presentation.common.UiError
import com.thehouse.app.presentation.common.UiEvent
import com.thehouse.app.presentation.common.toUiError
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * MVVM + UDF de Explorar (NV-07 D10).
 *
 * - Búsqueda real `/rooms/search` con universidad obligatoria, filtros,
 *   orden y paginación confirmados.
 * - Mapa real `/rooms/map` con bbox; el viewport se debounce y se valida
 *   (máximo 0.3° por lado) antes de llamar.
 * - Colonias `/places/search` con debounce; el texto NO filtra cuartos
 *   porque el contrato no expone `q` en /rooms/search.
 * - Guest: detalle requiere sesión (401 real); se redirige a Login.
 */
enum class ExploreMode { LIST, MAP }

sealed interface SearchResultsState {
    data object Idle : SearchResultsState
    data object Loading : SearchResultsState
    data class Content(
        val items: List<RoomSummary>,
        val page: Int,
        val pageSize: Int,
        val totalItems: Long,
        val totalPages: Int,
        val isLoadingMore: Boolean = false,
        val loadMoreError: UiError? = null,
    ) : SearchResultsState

    data object Empty : SearchResultsState
    data class Error(val error: UiError) : SearchResultsState
}

sealed interface MapResultsState {
    data object Idle : MapResultsState
    data object Loading : MapResultsState
    data class Content(val markers: List<RoomMarker>) : MapResultsState
    data object Empty : MapResultsState
    data object InvalidArea : MapResultsState
    data class Error(val error: UiError) : MapResultsState
}

data class ExploreUiState(
    val universities: CatalogLoadState<University> = CatalogLoadState.Loading,
    val selectedUniversityId: String? = null,
    val selectedUniversity: University? = null,
    val placesQuery: String = "",
    val placeSuggestions: List<PlaceSuggestion> = emptyList(),
    val selectedPlace: PlaceSuggestion? = null,
    val isSearchingPlaces: Boolean = false,
    val placesError: UiError? = null,
    val filters: RoomSearchFilters = RoomSearchFilters(),
    val filtersExpanded: Boolean = false,
    val mode: ExploreMode = ExploreMode.LIST,
    val results: SearchResultsState = SearchResultsState.Idle,
    val map: MapResultsState = MapResultsState.Idle,
    val mapBounds: MapBounds? = null,
    val selectedMarker: RoomMarker? = null,
)

sealed interface ExploreEvent : UiEvent {
    data object Retry : ExploreEvent
    data class QueryChanged(val query: String) : ExploreEvent
    data class PlaceSelected(val place: PlaceSuggestion) : ExploreEvent
    data object ClearPlace : ExploreEvent
    data class UniversitySelected(val universityId: String) : ExploreEvent
    data class FiltersChanged(val filters: RoomSearchFilters) : ExploreEvent
    data object ToggleFilters : ExploreEvent
    data object LoadMore : ExploreEvent
    data class ModeChanged(val mode: ExploreMode) : ExploreEvent
    data class MapViewportChanged(val bounds: MapBounds) : ExploreEvent
    data class MarkerClicked(val marker: RoomMarker) : ExploreEvent
    data object MarkerPreviewClosed : ExploreEvent
    data class RoomClicked(val roomId: String) : ExploreEvent
    data object MarkerDetailClicked : ExploreEvent
    data object RetryMap : ExploreEvent
}

sealed interface ExploreEffect : UiEffect {
    data class OpenRoomDetail(val roomId: String) : ExploreEffect
    data object RequireLogin : ExploreEffect
}

class ExploreViewModel(
    private val getUniversities: GetUniversitiesUseCase? = null,
    private val searchRooms: SearchRoomsUseCase? = null,
    private val searchPlacesUseCase: SearchPlacesUseCase? = null,
    private val getRoomMap: GetRoomMapUseCase? = null,
    private val selectedRepository: SelectedUniversityRepository? = null,
    private val selectUniversity: SelectUniversityUseCase? = null,
    private val sessionState: StateFlow<SessionState> = MutableStateFlow(SessionState.Anonymous),
) : BaseViewModel<ExploreUiState, ExploreEvent, ExploreEffect>(ExploreUiState()) {

    private var searchJob: Job? = null
    private var placesJob: Job? = null
    private var mapJob: Job? = null
    private var viewportJob: Job? = null
    private var searchVersion = 0
    private var mapVersion = 0

    init {
        loadUniversities()
        loadSelectedUniversity()
    }

    override fun onEvent(event: ExploreEvent) {
        when (event) {
            ExploreEvent.Retry -> retry()
            is ExploreEvent.QueryChanged -> onQueryChanged(event.query)
            is ExploreEvent.PlaceSelected -> selectPlace(event.place)
            ExploreEvent.ClearPlace -> clearPlace()
            is ExploreEvent.UniversitySelected -> changeUniversity(event.universityId)
            is ExploreEvent.FiltersChanged -> applyFilters(event.filters)
            ExploreEvent.ToggleFilters -> updateState { it.copy(filtersExpanded = !it.filtersExpanded) }
            ExploreEvent.LoadMore -> loadMore()
            is ExploreEvent.ModeChanged -> changeMode(event.mode)
            is ExploreEvent.MapViewportChanged -> onViewportChanged(event.bounds)
            is ExploreEvent.MarkerClicked -> updateState { it.copy(selectedMarker = event.marker) }
            ExploreEvent.MarkerPreviewClosed -> updateState { it.copy(selectedMarker = null) }
            is ExploreEvent.RoomClicked -> openRoom(event.roomId)
            ExploreEvent.MarkerDetailClicked -> uiState.value.selectedMarker?.let { openRoom(it.id) }
            ExploreEvent.RetryMap -> refreshMap(debounce = false)
        }
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
            val state = result.toCatalogState()
            val selectedId = uiState.value.selectedUniversityId
            updateState {
                it.copy(
                    universities = state,
                    selectedUniversity = (state as? CatalogLoadState.Success)
                        ?.items
                        ?.firstOrNull { university -> university.id == selectedId },
                )
            }
        }
    }

    private fun loadSelectedUniversity() {
        val repository = selectedRepository ?: return
        launch {
            repository.loadSelected()
            val id = repository.selectedUniversityId.value
            if (id == null) {
                updateState { it.copy(results = SearchResultsState.Idle) }
                return@launch
            }
            updateState {
                it.copy(
                    selectedUniversityId = id,
                    selectedUniversity = (it.universities as? CatalogLoadState.Success)
                        ?.items
                        ?.firstOrNull { university -> university.id == id },
                )
            }
            searchFirstPage(debounceMs = 0)
        }
    }

    private fun changeUniversity(universityId: String) {
        val university = (uiState.value.universities as? CatalogLoadState.Success)
            ?.items
            ?.firstOrNull { it.id == universityId }
        updateState {
            it.copy(
                selectedUniversityId = universityId,
                selectedUniversity = university,
                selectedPlace = null,
                placesQuery = "",
                placeSuggestions = emptyList(),
                placesError = null,
                map = MapResultsState.Idle,
                mapBounds = null,
                selectedMarker = null,
            )
        }
        launch { selectUniversity?.invoke(universityId) }
        searchFirstPage(debounceMs = 0)
    }

    private fun applyFilters(filters: RoomSearchFilters) {
        updateState { it.copy(filters = filters) }
        searchFirstPage(debounceMs = FILTER_DEBOUNCE_MS)
        if (uiState.value.mode == ExploreMode.MAP) {
            refreshMap(debounce = true)
        }
    }

    private fun onQueryChanged(query: String) {
        updateState { it.copy(placesQuery = query, selectedPlace = null, selectedMarker = null) }
        placesJob?.cancel()
        val universityId = uiState.value.selectedUniversityId
        val trimmed = query.trim()
        if (universityId == null || trimmed.length !in 2..60) {
            updateState {
                it.copy(
                    placeSuggestions = emptyList(),
                    isSearchingPlaces = false,
                    placesError = null,
                )
            }
            return
        }
        updateState { it.copy(isSearchingPlaces = true, placesError = null) }
        placesJob = launch {
            delay(PLACES_DEBOUNCE_MS)
            val useCase = searchPlacesUseCase
            if (useCase == null) {
                updateState { it.copy(isSearchingPlaces = false) }
                return@launch
            }
            val result = useCase(trimmed, universityId)
            result.fold(
                onSuccess = { places ->
                    updateState {
                        it.copy(placeSuggestions = places, isSearchingPlaces = false, placesError = null)
                    }
                },
                onFailure = { failure ->
                    if (failure is kotlinx.coroutines.CancellationException) throw failure
                    updateState {
                        it.copy(
                            placeSuggestions = emptyList(),
                            isSearchingPlaces = false,
                            placesError = failure.toUiError(),
                        )
                    }
                },
            )
        }
    }

    private fun selectPlace(place: PlaceSuggestion) {
        updateState {
            it.copy(
                selectedPlace = place,
                placesQuery = place.name,
                placeSuggestions = emptyList(),
                placesError = null,
                selectedMarker = null,
            )
        }
        if (uiState.value.mode == ExploreMode.MAP) {
            val bounds = MapBounds.around(place.approxLocation)
            updateState { it.copy(mapBounds = bounds) }
            loadMapNow(bounds)
        }
    }

    private fun clearPlace() {
        updateState {
            it.copy(
                selectedPlace = null,
                placesQuery = "",
                placeSuggestions = emptyList(),
                placesError = null,
            )
        }
        if (uiState.value.mode == ExploreMode.MAP) {
            refreshMap(debounce = false)
        }
    }

    private fun changeMode(mode: ExploreMode) {
        updateState { it.copy(mode = mode) }
        if (mode == ExploreMode.MAP) {
            refreshMap(debounce = false)
        }
    }

    private fun searchFirstPage(debounceMs: Long) {
        requestSearch(page = 1, append = false, debounceMs = debounceMs)
    }

    private fun loadMore() {
        val content = uiState.value.results as? SearchResultsState.Content ?: return
        if (content.isLoadingMore || content.page >= content.totalPages) return
        updateState {
            val current = it.results as? SearchResultsState.Content ?: return@updateState it
            it.copy(results = current.copy(isLoadingMore = true, loadMoreError = null))
        }
        requestSearch(page = content.page + 1, append = true, debounceMs = 0)
    }

    private fun requestSearch(page: Int, append: Boolean, debounceMs: Long) {
        val universityId = uiState.value.selectedUniversityId
            ?: return
        val useCase = searchRooms
        if (useCase == null) {
            updateState { it.copy(results = SearchResultsState.Empty) }
            return
        }
        searchJob?.cancel()
        val version = ++searchVersion
        if (!append) {
            updateState { it.copy(results = SearchResultsState.Loading) }
        }
        searchJob = launch {
            if (debounceMs > 0) delay(debounceMs)
            if (version != searchVersion) return@launch
            val result = useCase(universityId, uiState.value.filters, page, PAGE_SIZE)
            if (version != searchVersion) return@launch
            result.fold(
                onSuccess = { paged ->
                    val previous = if (append) {
                        (uiState.value.results as? SearchResultsState.Content)?.items.orEmpty()
                    } else {
                        emptyList()
                    }
                    val items = (previous + paged.items).distinctBy { it.id }
                    updateState {
                        it.copy(
                            results = if (items.isEmpty()) {
                                SearchResultsState.Empty
                            } else {
                                SearchResultsState.Content(
                                    items = items,
                                    page = paged.page,
                                    pageSize = paged.pageSize,
                                    totalItems = paged.totalItems,
                                    totalPages = paged.totalPages,
                                )
                            },
                        )
                    }
                    val mapState = uiState.value.map
                    if (!append && uiState.value.mode == ExploreMode.MAP &&
                        (mapState is MapResultsState.Idle || mapState is MapResultsState.Empty)
                    ) {
                        refreshMap(debounce = false)
                    }
                },
                onFailure = { failure ->
                    val error = failure.toUiError()
                    updateState {
                        if (append) {
                            val current = it.results as? SearchResultsState.Content
                            if (current != null) {
                                it.copy(results = current.copy(isLoadingMore = false, loadMoreError = error))
                            } else {
                                it.copy(results = SearchResultsState.Error(error))
                            }
                        } else {
                            it.copy(results = SearchResultsState.Error(error))
                        }
                    }
                },
            )
        }
    }

    private fun retry() {
        if (uiState.value.selectedUniversityId == null) {
            loadUniversities()
            loadSelectedUniversity()
            return
        }
        searchFirstPage(debounceMs = 0)
        if (uiState.value.mode == ExploreMode.MAP) {
            refreshMap(debounce = false)
        }
    }

    private fun refreshMap(debounce: Boolean) {
        val currentBounds = uiState.value.mapBounds
        val bounds = when {
            currentBounds == null -> initialMapBounds()
            currentBounds.isValid() -> currentBounds
            else -> {
                // Retry/refresh tras un viewport inválido: se restablece un
                // bbox válido (centro anterior o resultados actuales).
                initialMapBounds() ?: currentBounds.center.takeIf {
                    it.latitude.isFinite() && it.longitude.isFinite()
                }?.let { MapBounds.around(it) }
            }
        }
        if (bounds == null) {
            updateState { it.copy(map = MapResultsState.Empty, selectedMarker = null) }
            return
        }
        if (!bounds.isValid()) {
            updateState { it.copy(map = MapResultsState.InvalidArea, mapBounds = bounds) }
            return
        }
        updateState { it.copy(mapBounds = bounds, selectedMarker = null) }
        if (debounce) {
            scheduleMapRequest(bounds)
        } else {
            loadMapNow(bounds)
        }
    }

    private fun onViewportChanged(bounds: MapBounds) {
        updateState { it.copy(mapBounds = bounds, selectedMarker = null) }
        if (!bounds.isValid()) {
            viewportJob?.cancel()
            mapJob?.cancel()
            updateState { it.copy(map = MapResultsState.InvalidArea) }
            return
        }
        scheduleMapRequest(bounds)
    }

    private fun scheduleMapRequest(bounds: MapBounds) {
        viewportJob?.cancel()
        viewportJob = launch {
            delay(VIEWPORT_DEBOUNCE_MS)
            loadMapNow(bounds)
        }
    }

    private fun loadMapNow(bounds: MapBounds) {
        val universityId = uiState.value.selectedUniversityId
            ?: return
        val useCase = getRoomMap
        if (useCase == null) {
            updateState { it.copy(map = MapResultsState.Empty) }
            return
        }
        mapJob?.cancel()
        val version = ++mapVersion
        updateState { it.copy(map = MapResultsState.Loading) }
        mapJob = launch {
            val result = useCase(universityId, uiState.value.filters, bounds)
            if (version != mapVersion) return@launch
            result.fold(
                onSuccess = { markers ->
                    updateState {
                        it.copy(
                            map = if (markers.isEmpty()) {
                                MapResultsState.Empty
                            } else {
                                MapResultsState.Content(markers)
                            },
                            selectedMarker = null,
                        )
                    }
                },
                onFailure = { failure ->
                    val error = (failure as? DomainException)?.error ?: DomainError.Unknown
                    updateState {
                        it.copy(
                            map = if (error == DomainError.BadRequest) {
                                MapResultsState.InvalidArea
                            } else {
                                MapResultsState.Error(error.toUiError())
                            },
                        )
                    }
                },
            )
        }
    }

    private fun initialMapBounds(): MapBounds? {
        val place = uiState.value.selectedPlace
        if (place != null) return MapBounds.around(place.approxLocation)
        val items = (uiState.value.results as? SearchResultsState.Content)?.items.orEmpty()
        if (items.isEmpty()) return null
        val center = GeoPoint(
            latitude = items.map { it.approxLocation.latitude }.average(),
            longitude = items.map { it.approxLocation.longitude }.average(),
        )
        return MapBounds.around(center, latitudeSpan = 0.08, longitudeSpan = 0.08)
    }

    private fun openRoom(roomId: String) {
        val authenticated = sessionState.value is SessionState.Active
        if (AccessPolicy.shouldRedirectToLogin(ProtectedAction.ViewRoomDetails, authenticated)) {
            sendEffect(ExploreEffect.RequireLogin)
        } else {
            sendEffect(ExploreEffect.OpenRoomDetail(roomId))
        }
    }

    private fun Throwable.toUiError(): UiError =
        ((this as? DomainException)?.error ?: DomainError.Unknown).toUiError()

    private companion object {
        const val PAGE_SIZE = 20
        const val PLACES_DEBOUNCE_MS = 350L
        const val FILTER_DEBOUNCE_MS = 250L
        const val VIEWPORT_DEBOUNCE_MS = 350L
    }
}
