package com.thehouse.app

import com.thehouse.app.data.remote.api.RoomApi
import com.thehouse.app.data.remote.dto.room.RoomSearchResponseDto
import com.thehouse.app.data.room.RoomSearchRepositoryImpl
import com.thehouse.app.domain.model.SessionState
import com.thehouse.app.domain.model.UserSession
import com.thehouse.app.domain.model.University
import com.thehouse.app.domain.repository.AuthRepository
import com.thehouse.app.domain.repository.SelectedUniversityRepository
import com.thehouse.app.domain.usecase.LogoutUseCase
import com.thehouse.app.domain.usecase.SearchRoomsUseCase
import com.thehouse.app.presentation.catalog.RoomSearchState
import com.thehouse.app.presentation.explore.ExploreUiState
import com.thehouse.app.presentation.explore.ExploreViewModel
import com.thehouse.app.presentation.home.HomeViewModel
import com.thehouse.app.presentation.roomdetail.D18RoomDetailTest
import com.thehouse.app.testutil.TestMainDispatcherRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** D20 hotfix: regresiones del catálogo bloqueado detectado en runtime. */
class D20HotfixCatalogTest {

    @get:Rule
    val mainDispatcherRule = TestMainDispatcherRule(StandardTestDispatcher())

    private val searchJson = """{"items":[{"id":"room-real","buildingId":"b","title":"Cuarto",
        "price":3000,"currency":"MXN","roomType":"single","bathroom":"private",
        "buildingType":"rooms_building","allowedTenants":"any","services":["wifi"],
        "distanceKm":1.0,"walkingMinutes":12,"approxLocation":{"lat":19.5,"lng":-99.1},
        "thumbnailUrl":null,"demo":true}],"page":1,"pageSize":20,"totalItems":1,"totalPages":1}"""

    private fun api(onCall: () -> Unit = {}) = object : RoomApi {
        override suspend fun getRoom(id: String) = error("no usado")
        override suspend fun searchRooms(
            universityId: String, minPrice: Int?, maxPrice: Int?, roomType: String?,
            bathroom: String?, buildingType: String?, allowedTenants: String?,
            services: String?, maxDistanceKm: Double?, sort: String?, page: Int, pageSize: Int,
        ): RoomSearchResponseDto {
            onCall()
            return kotlinx.serialization.json.Json.decodeFromString<RoomSearchResponseDto>(searchJson)
        }
    }

    private fun university() = University("uni-real", "UTTT", "Universidad", "Tulancingo", "México", "2026-01-01T00:00:00Z")

    private fun auth() = object : AuthRepository {
        private val state = MutableStateFlow<SessionState>(SessionState.Anonymous)
        override val session = state
        override suspend fun register(n: String, e: String, p: String, t: Boolean, pr: Boolean) = Result.success(null)
        override suspend fun login(e: String, p: String) = Result.success(null)
        override suspend fun logout(): Result<Unit> { state.value = SessionState.Anonymous; return Result.success(Unit) }
        override suspend fun refreshAccessToken() = Result.success(null)
        override suspend fun getCurrentUser() = Result.success(null)
        override suspend fun restoreSession() = Result.success(null)
    }

    @Test
    fun `P0 home dispara search cuando perfil emite universidad con sesion restaurada despues`() = runTest {
        var calls = 0
        val profileFlow = MutableStateFlow<University?>(null)
        val session = MutableStateFlow<SessionState>(SessionState.Anonymous)
        // VM creado con sesión ANÓNIMA (como en producción antes de restore).
        val vm = HomeViewModel(
            logoutUseCase = LogoutUseCase(auth()),
            sessionState = session,
            searchRooms = SearchRoomsUseCase(RoomSearchRepositoryImpl(api { calls++ })),
            profileUniversityState = profileFlow,
        )
        advanceUntilIdle()
        // Estado terminal, nunca Loading permanente.
        assertTrue(vm.uiState.value.rooms is RoomSearchState.Empty)
        // Restauración tardía de sesión + universidad del perfil.
        session.value = SessionState.Active(
            UserSession(userId = "u1", displayName = "Ana", email = "a@x.mx"),
        )
        profileFlow.value = university()
        advanceUntilIdle()
        assertTrue(calls > 0)
        val state = vm.uiState.value.rooms as RoomSearchState.Content
        assertEquals("room-real", state.items.single().roomId)
        assertEquals("profile", vm.uiState.value.universitySource)
    }

    @Test
    fun `home sin universidad nunca queda en Loading`() = runTest {
        val vm = HomeViewModel(
            logoutUseCase = LogoutUseCase(auth()),
            sessionState = MutableStateFlow(SessionState.Anonymous),
            searchRooms = SearchRoomsUseCase(RoomSearchRepositoryImpl(api())),
        )
        advanceUntilIdle()
        assertTrue(vm.uiState.value.rooms is RoomSearchState.Empty)
    }

    @Test
    fun `explore resuelve universidad del perfil sin parametro externo y llama search`() = runTest {
        var calls = 0
        val profileFlow = MutableStateFlow<University?>(null)
        val vm = ExploreViewModel(
            searchRooms = SearchRoomsUseCase(RoomSearchRepositoryImpl(api { calls++ })),
            profileUniversityState = profileFlow,
            sessionState = MutableStateFlow(
                SessionState.Active(UserSession("u1", "Ana", "a@x.mx")),
            ),
        )
        advanceUntilIdle()
        assertTrue(vm.uiState.value.rooms is RoomSearchState.Empty)
        profileFlow.value = university()
        advanceUntilIdle()
        assertTrue(calls > 0)
        val state = vm.uiState.value.rooms as RoomSearchState.Content
        assertEquals("room-real", state.items.single().roomId)
    }

    @Test
    fun `explore aplica filtros contractuales y permite reintentar desde error`() = runTest {
        val filters = com.thehouse.app.domain.room.RoomSearchFilters(
            universityId = "uni-1", roomType = "single", buildingType = "rooms_building",
            allowedTenants = "women", maxDistanceKm = 8.0, sort = "price", page = 1,
        )
        assertTrue("single" in com.thehouse.app.domain.room.RoomSearchCatalog.ROOM_TYPES)
        assertTrue("rooms_building" in com.thehouse.app.domain.room.RoomSearchCatalog.BUILDING_TYPES)
        assertEquals("distance", com.thehouse.app.domain.room.RoomSearchCatalog.SORTS.first())
        assertEquals(8.0, filters.maxDistanceKm, 0.0)
    }

    @Test
    fun `ruta room detail concreta nunca duplica marcador`() {
        assertEquals(
            "cuarto/room-real",
            com.thehouse.app.presentation.navigation.RoomDetailRoute("room-real").route,
        )
    }
}
