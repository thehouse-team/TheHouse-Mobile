package com.thehouse.app.presentation.explore

import com.thehouse.app.domain.error.DomainError
import com.thehouse.app.domain.error.DomainException
import com.thehouse.app.domain.model.GeoPoint
import com.thehouse.app.domain.model.MapBounds
import com.thehouse.app.domain.model.PagedResult
import com.thehouse.app.domain.model.PlaceSuggestion
import com.thehouse.app.domain.model.Property
import com.thehouse.app.domain.model.RoomDetail
import com.thehouse.app.domain.model.RoomMarker
import com.thehouse.app.domain.model.RoomSearchFilters
import com.thehouse.app.domain.model.RoomSort
import com.thehouse.app.domain.model.RoomSummary
import com.thehouse.app.domain.model.SessionState
import com.thehouse.app.domain.model.University
import com.thehouse.app.domain.model.UserSession
import com.thehouse.app.domain.repository.PropertyRepository
import com.thehouse.app.domain.repository.SelectedUniversityRepository
import com.thehouse.app.domain.repository.UniversityRepository
import com.thehouse.app.domain.usecase.GetRoomMapUseCase
import com.thehouse.app.domain.usecase.GetUniversitiesUseCase
import com.thehouse.app.domain.usecase.SearchPlacesUseCase
import com.thehouse.app.domain.usecase.SearchRoomsUseCase
import com.thehouse.app.domain.usecase.SelectUniversityUseCase
import com.thehouse.app.presentation.common.UiError
import com.thehouse.app.testutil.TestMainDispatcherRule
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

private const val UNIVERSITY_ID = "6578a8b8c29c000000000001"
private const val ROOM_ID = "6578a8b8c29c000000000010"
private const val ROOM_ID_2 = "6578a8b8c29c000000000012"
private const val ROOM_ID_3 = "6578a8b8c29c000000000013"
private const val BUILDING_ID = "6578a8b8c29c000000000011"

private val testUniversity = University(
    id = UNIVERSITY_ID,
    code = "UTTT",
    name = "Universidad Tecnológica de Tulancingo",
    city = "Tulancingo",
    country = "MX",
    createdAt = "2026-11-05T00:00:00Z",
)

private fun testRoom(id: String, price: Int = 3500): RoomSummary = RoomSummary(
    id = id,
    buildingId = BUILDING_ID,
    title = "Cuarto $id",
    price = price,
    currency = "MXN",
    roomType = "single",
    bathroom = "shared",
    buildingType = "house_rooms",
    allowedTenants = "any",
    services = listOf("wifi", "water"),
    distanceKm = 1.2,
    walkingMinutes = 15,
    approxLocation = GeoPoint(latitude = 20.08, longitude = -98.36),
    thumbnailUrl = null,
    demo = true,
)

private fun testMarker(id: String = ROOM_ID): RoomMarker = RoomMarker(
    id = id,
    price = 3500,
    approxLocation = GeoPoint(latitude = 20.08, longitude = -98.36),
    distanceKm = 1.2,
)

private class FakeUniversityRepository(
    private val result: Result<PagedResult<University>> =
        Result.success(PagedResult(listOf(testUniversity), 1, 20, 1, 1)),
) : UniversityRepository {
    override suspend fun getUniversities(
        page: Int,
        pageSize: Int,
    ): Result<PagedResult<University>> = result

    override suspend fun getUniversityById(id: String): Result<University> =
        Result.success(testUniversity)
}

private class FakeSelectedRepository(
    private val storedId: String? = UNIVERSITY_ID,
) : SelectedUniversityRepository {
    private val state = MutableStateFlow<String?>(null)
    override val selectedUniversityId: StateFlow<String?> = state

    override suspend fun loadSelected() {
        state.value = storedId
    }

    override suspend fun select(universityId: String) {
        state.value = universityId
    }

    override suspend fun clear() {
        state.value = null
    }
}

private data class SearchCall(
    val universityId: String,
    val filters: RoomSearchFilters,
    val page: Int,
    val pageSize: Int,
)

private data class MapCall(
    val universityId: String,
    val filters: RoomSearchFilters,
    val bounds: MapBounds,
)

private class FakeSearchRepository(
    var searchHandler: suspend (Int) -> Result<PagedResult<RoomSummary>> = { page ->
        Result.success(
            PagedResult(
                items = listOf(testRoom(ROOM_ID)),
                page = page,
                pageSize = 20,
                totalItems = 1,
                totalPages = 1,
            ),
        )
    },
    var mapHandler: suspend () -> Result<List<RoomMarker>> = {
        Result.success(listOf(testMarker()))
    },
    var placesHandler: suspend (String) -> Result<List<PlaceSuggestion>> = {
        Result.success(
            listOf(
                PlaceSuggestion(
                    id = "place-1",
                    name = "Centro",
                    type = "neighborhood",
                    approxLocation = GeoPoint(20.08, -98.36),
                    distanceKm = 0.8,
                ),
            ),
        )
    },
) : PropertyRepository {

    val searchCalls = mutableListOf<SearchCall>()
    val mapCalls = mutableListOf<MapCall>()
    val placeQueries = mutableListOf<String>()

    override suspend fun getProperties(page: Int, pageSize: Int) =
        Result.success(PagedResult(emptyList<Property>(), page, pageSize, 0, 0))

    override suspend fun getPropertyById(id: String): Result<Property> =
        Result.failure(IllegalStateException("No usado"))

    override suspend fun searchRooms(
        universityId: String,
        filters: RoomSearchFilters,
        page: Int,
        pageSize: Int,
    ): Result<PagedResult<RoomSummary>> {
        searchCalls += SearchCall(universityId, filters, page, pageSize)
        return searchHandler(page)
    }

    override suspend fun getRoomMapMarkers(
        universityId: String,
        filters: RoomSearchFilters,
        bounds: MapBounds,
    ): Result<List<RoomMarker>> {
        mapCalls += MapCall(universityId, filters, bounds)
        return mapHandler()
    }

    override suspend fun searchPlaces(
        query: String,
        universityId: String,
    ): Result<List<PlaceSuggestion>> {
        placeQueries += query
        return placesHandler(query)
    }

    override suspend fun getRoomById(id: String): Result<RoomDetail> =
        Result.failure(IllegalStateException("No usado"))
}

class ExploreViewModelTest {

    @get:Rule
    val mainDispatcherRule = TestMainDispatcherRule(StandardTestDispatcher())

    private fun createViewModel(
        repository: FakeSearchRepository = FakeSearchRepository(),
        selected: FakeSelectedRepository = FakeSelectedRepository(),
        sessionState: StateFlow<SessionState> = MutableStateFlow(SessionState.Anonymous),
    ): ExploreViewModel = ExploreViewModel(
        getUniversities = GetUniversitiesUseCase(FakeUniversityRepository()),
        searchRooms = SearchRoomsUseCase(repository),
        searchPlacesUseCase = SearchPlacesUseCase(repository),
        getRoomMap = GetRoomMapUseCase(repository),
        selectedRepository = selected,
        selectUniversity = SelectUniversityUseCase(selected),
        sessionState = sessionState,
    )

    @Test
    fun `carga universidad seleccionada y busqueda real con content`() = runTest {
        val repo = FakeSearchRepository()
        val selected = FakeSelectedRepository()
        val vm = createViewModel(repository = repo, selected = selected)
        runCurrent()

        assertEquals(UNIVERSITY_ID, vm.uiState.value.selectedUniversityId)
        val results = vm.uiState.value.results as SearchResultsState.Content
        assertEquals(1, results.items.size)
        assertEquals(1L, results.totalItems)
        assertEquals(1, repo.searchCalls.size)
        assertEquals(UNIVERSITY_ID, repo.searchCalls.first().universityId)
        assertEquals(1, repo.searchCalls.first().page)
        assertEquals(20, repo.searchCalls.first().pageSize)
        assertEquals(UNIVERSITY_ID, selected.selectedUniversityId.value)
    }

    @Test
    fun `filtros y orden se envian al contrato real`() = runTest {
        val repo = FakeSearchRepository()
        val vm = createViewModel(repository = repo)
        runCurrent()

        val filters = RoomSearchFilters(
            minPrice = 1500,
            maxPrice = 5000,
            roomType = "single",
            bathroom = "private",
            buildingType = "house_rooms",
            allowedTenants = "women",
            services = setOf("wifi", "security"),
            maxDistanceKm = 5.0,
            sort = RoomSort.PRICE,
        )
        vm.onEvent(ExploreEvent.FiltersChanged(filters))
        advanceUntilIdle()

        assertEquals(2, repo.searchCalls.size)
        assertEquals(filters, repo.searchCalls.last().filters)
    }

    @Test
    fun `paginacion carga pagina 2 y acumula resultados`() = runTest {
        val repo = FakeSearchRepository(
            searchHandler = { page ->
                if (page == 1) {
                    Result.success(
                        PagedResult(
                            listOf(testRoom(ROOM_ID), testRoom(ROOM_ID_2)),
                            1,
                            20,
                            3,
                            2,
                        ),
                    )
                } else {
                    Result.success(
                        PagedResult(listOf(testRoom(ROOM_ID_3)), 2, 20, 3, 2),
                    )
                }
            },
        )
        val vm = createViewModel(repository = repo)
        runCurrent()

        vm.onEvent(ExploreEvent.LoadMore)
        advanceUntilIdle()

        val results = vm.uiState.value.results as SearchResultsState.Content
        assertEquals(3, results.items.size)
        assertEquals(2, results.page)
        assertEquals(3L, results.totalItems)
        assertEquals(2, repo.searchCalls.size)
        assertEquals(2, repo.searchCalls.last().page)
    }

    @Test
    fun `sin resultados muestra Empty`() = runTest {
        val repo = FakeSearchRepository(
            searchHandler = {
                Result.success(PagedResult(emptyList<RoomSummary>(), 1, 20, 0, 0))
            },
        )
        val vm = createViewModel(repository = repo)
        runCurrent()
        assertTrue(vm.uiState.value.results is SearchResultsState.Empty)
    }

    @Test
    fun `network error muestra Error y retry recupera`() = runTest {
        var calls = 0
        val repo = FakeSearchRepository(
            searchHandler = {
                calls++
                if (calls == 1) {
                    Result.failure(DomainException(DomainError.Network))
                } else {
                    Result.success(PagedResult(listOf(testRoom(ROOM_ID)), 1, 20, 1, 1))
                }
            },
        )
        val vm = createViewModel(repository = repo)
        runCurrent()
        assertEquals(
            UiError.Network,
            (vm.uiState.value.results as SearchResultsState.Error).error,
        )

        vm.onEvent(ExploreEvent.Retry)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.results is SearchResultsState.Content)
    }

    @Test
    fun `timeout muestra Error Timeout sin crash`() = runTest {
        val repo = FakeSearchRepository(
            searchHandler = {
                Result.failure(DomainException(DomainError.Timeout))
            },
        )
        val vm = createViewModel(repository = repo)
        runCurrent()
        assertEquals(
            UiError.Timeout,
            (vm.uiState.value.results as SearchResultsState.Error).error,
        )
    }

    @Test
    fun `filtro invalido 400 se informa sin llamar al repositorio`() = runTest {
        val repo = FakeSearchRepository()
        val vm = createViewModel(repository = repo)
        runCurrent()
        assertEquals(1, repo.searchCalls.size)

        vm.onEvent(ExploreEvent.FiltersChanged(RoomSearchFilters(minPrice = 100)))
        advanceUntilIdle()

        assertEquals(
            UiError.BadRequest,
            (vm.uiState.value.results as SearchResultsState.Error).error,
        )
        assertEquals(1, repo.searchCalls.size)
    }

    @Test
    fun `mapa carga marcadores reales con bbox valido`() = runTest {
        val repo = FakeSearchRepository()
        val vm = createViewModel(repository = repo)
        runCurrent()

        vm.onEvent(ExploreEvent.ModeChanged(ExploreMode.MAP))
        runCurrent()

        val map = vm.uiState.value.map as MapResultsState.Content
        assertEquals(1, map.markers.size)
        assertEquals(1, repo.mapCalls.size)
        assertTrue(repo.mapCalls.first().bounds.isValid())
    }

    @Test
    fun `viewport invalido muestra InvalidArea y retry restablece bbox`() = runTest {
        val repo = FakeSearchRepository()
        val vm = createViewModel(repository = repo)
        runCurrent()
        vm.onEvent(ExploreEvent.ModeChanged(ExploreMode.MAP))
        runCurrent()

        val invalid = MapBounds(-98.5, 20.0, -98.0, 20.5)
        vm.onEvent(ExploreEvent.MapViewportChanged(invalid))
        runCurrent()
        assertTrue(vm.uiState.value.map is MapResultsState.InvalidArea)

        vm.onEvent(ExploreEvent.RetryMap)
        runCurrent()

        assertTrue(vm.uiState.value.map is MapResultsState.Content)
        val bounds = vm.uiState.value.mapBounds
        assertTrue(bounds != null && bounds.isValid())
    }

    @Test
    fun `viewport valido se debounce antes de recargar mapa`() = runTest {
        val repo = FakeSearchRepository()
        val vm = createViewModel(repository = repo)
        runCurrent()
        vm.onEvent(ExploreEvent.ModeChanged(ExploreMode.MAP))
        runCurrent()
        assertEquals(1, repo.mapCalls.size)

        vm.onEvent(
            ExploreEvent.MapViewportChanged(
                MapBounds(-98.38, 20.06, -98.34, 20.10),
            ),
        )
        advanceTimeBy(349)
        assertEquals(1, repo.mapCalls.size)

        advanceUntilIdle()
        assertEquals(2, repo.mapCalls.size)
        assertEquals(-98.38, repo.mapCalls.last().bounds.minLongitude, 0.0001)
    }

    @Test
    fun `busqueda de colonia usa debounce y query del contrato`() = runTest {
        val repo = FakeSearchRepository()
        val vm = createViewModel(repository = repo)
        runCurrent()

        vm.onEvent(ExploreEvent.QueryChanged("Centro"))
        advanceUntilIdle()

        assertEquals(listOf("Centro"), repo.placeQueries)
        assertEquals(1, vm.uiState.value.placeSuggestions.size)
        assertEquals("Centro", vm.uiState.value.placeSuggestions.first().name)
    }

    @Test
    fun `query corta limpia sugerencias sin llamar backend`() = runTest {
        val repo = FakeSearchRepository()
        val vm = createViewModel(repository = repo)
        runCurrent()

        vm.onEvent(ExploreEvent.QueryChanged("a"))
        advanceUntilIdle()

        assertTrue(repo.placeQueries.isEmpty())
        assertTrue(vm.uiState.value.placeSuggestions.isEmpty())
    }

    @Test
    fun `resultado autenticado emite OpenRoomDetail`() = runTest {
        val repo = FakeSearchRepository()
        val session = MutableStateFlow<SessionState>(
            SessionState.Active(
                UserSession(userId = "u1", displayName = "Victor", email = "v@correo.mx"),
            ),
        )
        val vm = createViewModel(repository = repo, sessionState = session)
        runCurrent()

        val effect = async(start = CoroutineStart.UNDISPATCHED) { vm.effects.first() }
        vm.onEvent(ExploreEvent.RoomClicked(ROOM_ID))
        runCurrent()

        assertEquals(ExploreEffect.OpenRoomDetail(ROOM_ID), effect.await())
    }

    @Test
    fun `resultado guest emite RequireLogin`() = runTest {
        val vm = createViewModel()
        runCurrent()

        val effect = async(start = CoroutineStart.UNDISPATCHED) { vm.effects.first() }
        vm.onEvent(ExploreEvent.RoomClicked(ROOM_ID))
        runCurrent()

        assertEquals(ExploreEffect.RequireLogin, effect.await())
    }

    @Test
    fun `marcador autenticado abre detalle real`() = runTest {
        val session = MutableStateFlow<SessionState>(
            SessionState.Active(
                UserSession(userId = "u1", displayName = "Victor", email = "v@correo.mx"),
            ),
        )
        val vm = createViewModel(sessionState = session)
        runCurrent()

        vm.onEvent(ExploreEvent.MarkerClicked(testMarker()))
        val effect = async(start = CoroutineStart.UNDISPATCHED) { vm.effects.first() }
        vm.onEvent(ExploreEvent.MarkerDetailClicked)
        runCurrent()

        assertEquals(ExploreEffect.OpenRoomDetail(ROOM_ID), effect.await())
    }

    @Test
    fun `error de mapa se informa y retry recarga`() = runTest {
        var calls = 0
        val repo = FakeSearchRepository(
            mapHandler = {
                calls++
                if (calls == 1) {
                    Result.failure(DomainException(DomainError.Server))
                } else {
                    Result.success(listOf(testMarker()))
                }
            },
        )
        val vm = createViewModel(repository = repo)
        runCurrent()
        vm.onEvent(ExploreEvent.ModeChanged(ExploreMode.MAP))
        runCurrent()
        assertEquals(
            UiError.Server,
            (vm.uiState.value.map as MapResultsState.Error).error,
        )

        vm.onEvent(ExploreEvent.RetryMap)
        runCurrent()
        assertTrue(vm.uiState.value.map is MapResultsState.Content)
    }
}
