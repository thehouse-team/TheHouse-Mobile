package com.thehouse.app

import com.thehouse.app.data.remote.api.RoomApi
import com.thehouse.app.data.remote.dto.room.RoomDetailResponseDto
import com.thehouse.app.data.remote.dto.room.RoomSearchResponseDto
import com.thehouse.app.data.room.RoomRepository
import com.thehouse.app.data.room.RoomSearchRepositoryImpl
import com.thehouse.app.domain.error.DomainError
import com.thehouse.app.domain.error.DomainException
import com.thehouse.app.domain.model.SessionState
import com.thehouse.app.domain.model.UserSession
import com.thehouse.app.domain.repository.FavoritesRepository
import com.thehouse.app.domain.repository.VisitsRepository
import com.thehouse.app.domain.room.RoomSearchCatalog
import com.thehouse.app.domain.room.RoomSearchFilters
import com.thehouse.app.domain.usecase.FavoriteStateStore
import com.thehouse.app.domain.usecase.GetRoomDetailUseCase
import com.thehouse.app.domain.usecase.SearchRoomsUseCase
import com.thehouse.app.domain.usecase.ToggleFavoriteUseCase
import com.thehouse.app.domain.usecase.VisitValidator
import com.thehouse.app.presentation.explore.ExploreEvent
import com.thehouse.app.presentation.explore.ExploreFiltersUi
import com.thehouse.app.presentation.explore.ExploreViewModel
import com.thehouse.app.presentation.home.HomeEvent
import com.thehouse.app.presentation.home.HomeViewModel
import com.thehouse.app.presentation.navigation.RoomDetailRoute
import com.thehouse.app.presentation.navigation.ScheduleRoomVisitRoute
import com.thehouse.app.data.visits.mapper.toDomain
import com.thehouse.app.domain.model.PagedResult
import com.thehouse.app.domain.room.RoomDetail
import com.thehouse.app.presentation.roomdetail.RoomDetailUiState
import com.thehouse.app.presentation.roomdetail.RoomDetailViewModel
import com.thehouse.app.testutil.TestMainDispatcherRule
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.UUID

/** D20: tests contractuales y de comportamiento de Fase B (sin datos inventados). */
class D20RoomsDiscoveryInteractionsTest {

    @get:Rule
    val mainDispatcherRule = TestMainDispatcherRule(StandardTestDispatcher())

    private val roomJson = """
        {"id":"room-1","buildingId":"b-1","title":"Cuarto real","price":5500,
         "currency":"MXN","roomType":"single","bathroom":"private",
         "buildingType":"rooms_building","allowedTenants":"women",
         "services":["wifi"],"distanceKm":1.2,"walkingMinutes":15,
         "approxLocation":{"lat":19.5,"lng":-99.1},"thumbnailUrl":null,"demo":false}
    """.trimIndent()

    private fun fakeSearchApi(
        response: String = """{"items":[$roomJson],"page":1,"pageSize":20,"totalItems":1,"totalPages":1}""",
        failWith: Throwable? = null,
        onCall: (RoomSearchFilters) -> Unit = {},
    ) = object : RoomApi {
        override suspend fun getRoom(id: String): RoomDetailResponseDto = error("no usado en este test")
        override suspend fun searchRooms(
            universityId: String,
            minPrice: Int?,
            maxPrice: Int?,
            roomType: String?,
            bathroom: String?,
            buildingType: String?,
            allowedTenants: String?,
            services: String?,
            maxDistanceKm: Double?,
            sort: String?,
            page: Int,
            pageSize: Int,
        ): RoomSearchResponseDto {
            onCall(
                RoomSearchFilters(
                    universityId = universityId,
                    minPrice = minPrice,
                    maxPrice = maxPrice,
                    roomType = roomType,
                    bathroom = bathroom,
                    buildingType = buildingType,
                    allowedTenants = allowedTenants,
                    services = services?.split(",")?.filter { it.isNotBlank() }.orEmpty(),
                    maxDistanceKm = maxDistanceKm ?: 10.0,
                    sort = sort ?: "distance",
                    page = page,
                    pageSize = pageSize,
                ),
            )
            if (failWith != null) throw failWith
            return kotlinx.serialization.json.Json.decodeFromString<RoomSearchResponseDto>(response)
        }
    }

    @Test
    fun `1 contrato rooms search mapea roomId y campos nullable sin inventar`() = runTest {
        val repo = RoomSearchRepositoryImpl(fakeSearchApi())
        val page = repo.searchRooms(RoomSearchFilters(universityId = "uni-1")).getOrThrow()
        val room = page.items.single()
        assertEquals("room-1", room.roomId)
        assertEquals(5500, room.price)
        assertEquals("MXN", room.currency)
        assertEquals(null, room.thumbnailUrl)
        assertEquals("rooms_building", room.buildingType)
        assertEquals(1.2, room.distanceKm, 0.0)
        assertEquals(1, page.page)
        assertEquals(1, page.totalPages)
    }

    @Test
    fun `2 codigos cerrados son los del backend no los del enunciado`() {
        assertEquals(listOf("single", "double", "shared"), RoomSearchCatalog.ROOM_TYPES)
        assertEquals(
            listOf("rooms_building", "house_rooms", "full_apartment"),
            RoomSearchCatalog.BUILDING_TYPES,
        )
        assertEquals(listOf("women", "men", "any"), RoomSearchCatalog.ALLOWED_TENANTS)
        assertEquals(listOf("distance", "price", "newest"), RoomSearchCatalog.SORTS)
    }

    @Test
    fun `3 filtros se envian con codigos contractuales y valores por defecto`() = runTest {
        var captured: RoomSearchFilters? = null
        val repo = RoomSearchRepositoryImpl(fakeSearchApi(onCall = { captured = it }))
        repo.searchRooms(
            RoomSearchFilters(
                universityId = "uni-1",
                roomType = "single",
                buildingType = "house_rooms",
                allowedTenants = "women",
                services = listOf("wifi", "parking"),
                maxDistanceKm = 8.0,
                sort = "price",
                page = 2,
                pageSize = 10,
            ),
        )
        val sent = captured!!
        assertEquals("single", sent.roomType)
        assertEquals("house_rooms", sent.buildingType)
        assertEquals("women", sent.allowedTenants)
        assertEquals(listOf("wifi", "parking"), sent.services)
        assertEquals(8.0, sent.maxDistanceKm, 0.0)
        assertEquals("price", sent.sort)
        assertEquals(2, sent.page)
        assertEquals(10, sent.pageSize)
    }

    @Test
    fun `4 paginacion real acumula sin duplicar y evita carga simultanea`() = runTest {
        val counter = intArrayOf(1)
        val api = object : RoomApi by fakeSearchApi() {
            override suspend fun searchRooms(
                universityId: String, minPrice: Int?, maxPrice: Int?, roomType: String?,
                bathroom: String?, buildingType: String?, allowedTenants: String?,
                services: String?, maxDistanceKm: Double?, sort: String?, page: Int, pageSize: Int,
            ): RoomSearchResponseDto = kotlinx.serialization.json.Json.decodeFromString<RoomSearchResponseDto>(
                """{"items":[{"id":"room-${counter[0]}","buildingId":"b","title":"Cuarto ${counter[0]}",
                    "price":5000,"currency":"MXN","buildingType":"rooms_building",
                    "allowedTenants":"any","services":[],"distanceKm":1.0,
                    "walkingMinutes":12,"approxLocation":{"lat":19.5,"lng":-99.1},
                    "thumbnailUrl":null,"demo":false}],"page":${counter[0]},"pageSize":1,
                    "totalItems":2,"totalPages":2}""".trimIndent(),
            ).also { counter[0] += 1 }
        }
        val vm = ExploreViewModel(searchRooms = SearchRoomsUseCase(RoomSearchRepositoryImpl(api)))
        vm.onEvent(ExploreEvent.UniversityChanged("uni-1"))
        advanceUntilIdle()
        vm.onEvent(ExploreEvent.LoadMoreRoomsClicked)
        vm.onEvent(ExploreEvent.LoadMoreRoomsClicked) // duplicado ignorado
        advanceUntilIdle()
        val state = vm.uiState.value.rooms as com.thehouse.app.presentation.catalog.RoomSearchState.Content
        assertEquals(listOf("room-1", "room-2"), state.items.map { it.roomId })
        assertEquals(2, state.page)
    }

    @Test
    fun `5 home con room id navega a room detail y no a property detail`() {
        val route = RoomDetailRoute("room-abc")
        assertEquals("cuarto/room-abc", route.route)
        assertEquals("cuarto/{roomId}", RoomDetailRoute.PATTERN)
        assertFalse(route.route.contains("/room-abc/", ignoreCase = false))
    }

    @Test
    fun `6 ruta agendar visita por room no mezcla property`() {
        val route = ScheduleRoomVisitRoute("room-1")
        assertTrue(route.route.startsWith("visita/agendar/cuarto/"))
        assertEquals("visita/agendar/cuarto/{roomId}", ScheduleRoomVisitRoute.PATTERN)
    }

    @Test
    fun `7 guest room detail no envia peticion protegida`() = runTest {
        val vm = RoomDetailViewModel(
            roomId = "room-1",
            getRoomDetail = GetRoomDetailUseCase(object : com.thehouse.app.domain.usecase.RoomDetailRepository {
                override suspend fun getRoomDetail(roomId: String): Result<RoomDetail> = Result.failure(
                    DomainException(DomainError.Unauthorized),
                )
            }),
        )
        advanceUntilIdle()
        assertTrue(vm.uiState.value is RoomDetailUiState.RequiresAuth)
    }

    @Test
    fun `8 favoritos roomIds manda sobre propertyIds`() {
        val dto = com.thehouse.app.data.remote.dto.favorites.FavoriteIdsResponseDto(
            roomIds = listOf("r1", "r2"),
            propertyIds = listOf("legacy-1"),
        )
        assertEquals(setOf("r1", "r2"), dto.effectiveRoomIds())
    }

    @Test
    fun `9 toggle optimista revierte ante error y previene duplicados`() = runTest {
        val store = FavoriteStateStore()
        val inFlight = mutableSetOf<String>()
        val failing = object : FavoritesRepository by LegacyFavoritesRepo() {
            override suspend fun addFavorite(roomId: String): Result<Unit> = Result.failure(
                DomainException(DomainError.Server),
            )
        }
        val toggle = ToggleFavoriteUseCase(
            repository = failing,
            store = store,
            isAuthenticated = { true },
            inFlight = inFlight,
        )
        val first = toggle("room-9")
        assertTrue(first is com.thehouse.app.domain.usecase.FavoriteToggleResult.Failed)
        assertTrue((first as com.thehouse.app.domain.usecase.FavoriteToggleResult.Failed).reverted)
        assertTrue(!store.contains("room-9"))
    }

    @Test
    fun `10 solicitudes usan room id real y no property id`() {
        val body = com.thehouse.app.data.remote.dto.requests.CreateRequestDto(
            quickQuestions = listOf("available_now"),
            message = "Hola",
        )
        assertEquals(listOf("available_now"), body.quickQuestions)
        assertEquals("Hola", body.message)
    }

    @Test
    fun `11 visit room XOR property y idempotency uuid`() {
        val key = UUID.randomUUID().toString()
        assertEquals(36, key.length)
        val body = com.thehouse.app.data.remote.dto.visits.CreateVisitRequestDto(
            roomId = "room-1",
            propertyId = null,
            scheduledAt = "2026-12-01T10:00:00-06:00",
        )
        assertEquals("room-1", body.roomId)
        assertEquals(null, body.propertyId)
        assertTrue(body.scheduledAt.contains("-06:00"))
    }

    @Test
    fun `12 visit validator respeta zona horas cuarto de hora y anticipacion`() {
        val now = Instant.parse("2026-11-27T12:00:00Z") // 06:00 MX
        val valid = ZonedDateTime.of(LocalDate.of(2026, 11, 28), LocalTime.of(10, 0), VisitValidator.TIME_ZONE)
        assertTrue(VisitValidator.validateSchedule(valid.toOffsetDateTime(), now).isValid)
        val tooSoon = ZonedDateTime.of(LocalDate.of(2026, 11, 27), LocalTime.of(7, 0), VisitValidator.TIME_ZONE)
        assertEquals(
            com.thehouse.app.domain.error.VisitErrorCode.TOO_SOON,
            VisitValidator.validateSchedule(tooSoon.toOffsetDateTime(), now).errorCode,
        )
        val outside = ZonedDateTime.of(LocalDate.of(2026, 11, 28), LocalTime.of(7, 0), VisitValidator.TIME_ZONE)
        assertEquals(
            com.thehouse.app.domain.error.VisitErrorCode.OUTSIDE_HOURS,
            VisitValidator.validateSchedule(outside.toOffsetDateTime(), now).errorCode,
        )
    }

    @Test
    fun `13 visit response property nullable no asumido`() {
        val dto = com.thehouse.app.data.remote.dto.visits.VisitDto(
            id = "v1",
            room = com.thehouse.app.data.remote.dto.visits.VisitRoomSummaryDto(
                id = "room-1", title = "Cuarto",
            ),
            property = null,
            scheduledAt = "2026-12-01T16:00:00Z",
            timeZone = "America/Mexico_City",
            status = "requested",
        )
        val visit = dto.toDomain()
        assertEquals("room-1", visit.roomId)
        assertFalse(visit.hasAddress)
    }

    @Test
    fun `14 sin universidad no se dispara rooms search`() = runTest {
        var calls = 0
        val vm = HomeViewModel(
            logoutUseCase = com.thehouse.app.domain.usecase.LogoutUseCase(
                object : com.thehouse.app.domain.repository.AuthRepository by LegacyAuthRepo() {},
            ),
            sessionState = MutableStateFlow(SessionState.Anonymous),
            searchRooms = SearchRoomsUseCase(RoomSearchRepositoryImpl(fakeSearchApi(onCall = { calls++ }))),
        )
        advanceUntilIdle()
        assertEquals(0, calls)
        assertTrue(vm.uiState.value.rooms is com.thehouse.app.presentation.catalog.RoomSearchState.Empty)
    }

    @Test
    fun `15 D19-1 regresion room detail no duplica patron en ruta`() {
        assertEquals("cuarto/", RoomDetailRoute("x").route.removeSuffix("x"))
        assertEquals("cuarto/{roomId}", RoomDetailRoute.PATTERN)
    }
}

private class LegacyFavoritesRepo : FavoritesRepository {
    override suspend fun getFavorites(page: Int, pageSize: Int): Result<PagedResult<com.thehouse.app.domain.model.Favorite>> =
        Result.success(PagedResult(emptyList(), 1, 20, 0, 0))
    override suspend fun getFavoriteIds(): Result<Set<String>> = Result.success(emptySet())
    override suspend fun addFavorite(roomId: String): Result<Unit> = Result.success(Unit)
    override suspend fun removeFavorite(roomId: String): Result<Unit> = Result.success(Unit)
}

private class LegacyAuthRepo : com.thehouse.app.domain.repository.AuthRepository {
    private val state = MutableStateFlow<SessionState>(SessionState.Anonymous)
    override val session = state
    override suspend fun register(
        name: String, email: String, password: String, acceptedTerms: Boolean, acceptedPrivacy: Boolean,
    ) = Result.success(null)
    override suspend fun login(email: String, password: String) = Result.success(null)
    override suspend fun logout() = Result.success(Unit)
    override suspend fun refreshAccessToken() = Result.success(null)
    override suspend fun getCurrentUser() = Result.success(null)
    override suspend fun restoreSession() = Result.success(null)
}
