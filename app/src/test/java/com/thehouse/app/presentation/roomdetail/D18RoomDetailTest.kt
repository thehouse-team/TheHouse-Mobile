package com.thehouse.app.presentation.roomdetail

import com.thehouse.app.data.error.DataError
import com.thehouse.app.data.error.toDomainError
import com.thehouse.app.data.remote.api.RoomApi
import com.thehouse.app.data.remote.dto.room.ApproxLocationDto
import com.thehouse.app.data.remote.dto.room.RoomBuildingDto
import com.thehouse.app.data.remote.dto.room.RoomDetailResponseDto
import com.thehouse.app.data.remote.dto.room.RoomPhotoDto
import com.thehouse.app.data.room.RoomRepository
import com.thehouse.app.domain.error.DomainError
import com.thehouse.app.domain.error.DomainException
import com.thehouse.app.domain.room.RoomDetail
import com.thehouse.app.domain.usecase.GetRoomDetailUseCase
import com.thehouse.app.presentation.common.BaseViewModel
import com.thehouse.app.presentation.common.UiEffect
import com.thehouse.app.presentation.common.UiEvent
import com.thehouse.app.presentation.navigation.RoomDetailRoute
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** D18 — tests 1–12 del contrato real de detalle de cuarto. */
@OptIn(ExperimentalCoroutinesApi::class)
class D18RoomDetailTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before fun setUp() { Dispatchers.setMain(testDispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    private val dispatchers = object : com.thehouse.app.domain.common.DispatcherProvider {
        override val io: CoroutineDispatcher = testDispatcher
        override val default: CoroutineDispatcher = testDispatcher
    }

    // ---------- 11: DTO mapping ----------
    @Test
    fun `11 DTO maps every real field without inventing data`() = runTest {
        val dto = roomDto()
        val room = RoomRepository(object : RoomApi {
            override suspend fun getRoom(id: String) = dto
        }).getRoomDetail("room-1").getOrThrow()
        assertEquals("room-1", room.roomId)
        assertEquals("b-1", room.buildingId)
        assertEquals(4500, room.price)
        assertEquals("MXN", room.currency)
        assertEquals(3.5, room.sizeM2!!, 0.0)
        assertEquals(listOf("wifi", "water"), room.building.services)
        assertEquals("No fumar", room.building.rules)
        assertEquals("UNIVER", room.nearestUniversity!!.code)
        assertEquals(1, room.photos.size)
        assertEquals("p1", room.photos.first().id)
        assertEquals(19.1, room.approxLocation.lat, 0.0)
    }

    @Test
    fun `11 DTO decodes from exact backend JSON`() {
        val raw = """
            {"id":"room-1","buildingId":"b-1","title":"Cuarto","price":4500,"currency":"MXN",
            "roomType":"private","bathroom":"private","sizeM2":3.5,"furniture":["cama"],
            "availableFrom":"2026-12-01","minContract":"6 meses","distanceKm":1.4,"walkingMinutes":18,
            "approxLocation":{"lat":19.1,"lng":-99.1},
            "nearestUniversity":{"id":"u1","code":"UNIVER","name":"Universidad"},
            "building":{"id":"b-1","name":"Edificio","type":"student_residence","allowedTenants":"any",
            "services":["wifi","water"],"rules":"No fumar"},
            "thumbnailUrl":null,"demo":false,
            "photos":[{"id":"p1","url":"/api/v1/media/p1","thumbUrl":"/api/v1/media/p1/thumb",
            "width":800,"height":600,"bytes":1234,"createdAt":"2026-11-01T00:00:00Z"}]}
        """.trimIndent()
        val dto = Json { ignoreUnknownKeys = true }.decodeFromString<RoomDetailResponseDto>(raw)
        assertEquals(4500, dto.price)
        assertEquals("No fumar", dto.building.rules)
    }

    // ---------- 1: GET room detail exactamente 1 vez ----------
    @Test
    fun `1 valid roomId calls GET room detail exactly once`() = runTest {
        val api = FakeRoomApi()
        val result = RoomRepository(api).getRoomDetail("room-1")
        assertTrue(result.isSuccess)
        assertEquals(1, api.calls)
        assertEquals("room-1", api.lastId)
    }

    // ---------- 2: 200 → Content ----------
    @Test
    fun `2 200 produces Content`() = runTest {
        val vm = vm("room-1", FakeRoomApi())
        advanceUntilIdle()
        val state = vm.uiState.value
        assertTrue(state is RoomDetailUiState.Content)
        assertEquals("room-1", (state as RoomDetailUiState.Content).room.roomId)
    }

    // ---------- 3: 404 → NotFound ----------
    @Test
    fun `3 404 produces NotFound`() = runTest {
        val vm = vm("room-1", FakeRoomApi(error = DomainException(DomainError.NotFound)))
        advanceUntilIdle()
        assertTrue(vm.uiState.value is RoomDetailUiState.NotFound)
    }

    // ---------- 4: 401 → RequiresAuth ----------
    @Test
    fun `4 401 produces RequiresAuth`() = runTest {
        val vm = vm("room-1", FakeRoomApi(error = DomainException(DomainError.Unauthorized)))
        advanceUntilIdle()
        assertTrue(vm.uiState.value is RoomDetailUiState.RequiresAuth)
    }

    // ---------- 5/6: 429 y network tipados ----------
    @Test
    fun `5 429 is typed RateLimited`() = runTest {
        val error = RoomRepository(FakeRoomApi(error = DomainException(DomainError.RateLimited)))
            .getRoomDetail("room-1").exceptionOrNull()
        assertEquals(DomainError.RateLimited, (error as DomainException).error)
    }

    @Test
    fun `6 network is typed Network`() = runTest {
        val error = RoomRepository(FakeRoomApi(error = DomainException(DomainError.Network)))
            .getRoomDetail("room-1").exceptionOrNull()
        assertEquals(DomainError.Network, (error as DomainException).error)
    }

    @Test
    fun `503 is typed ServiceUnavailable`() = runTest {
        val error = RoomRepository(FakeRoomApi(error = DomainException(DomainError.ServiceUnavailable)))
            .getRoomDetail("room-1").exceptionOrNull()
        assertEquals(DomainError.ServiceUnavailable, (error as DomainException).error)
    }

    // ---------- 7/8/9: Smart Match navigation ----------
    @Test
    fun `7 8 9 Smart Match click uses roomId and never PropertyDetail`() = runTest {
        // DTO AUTH anidado expone room.id como roomId; el ViewModel lo preserva.
        val result = com.thehouse.app.domain.smartmatch.SmartMatchResult(
            roomId = "room-xyz",
            property = com.thehouse.app.domain.model.Property(
                id = "room-xyz", title = "t", price = 1.0, currency = "MXN", city = "",
                universityId = null, bedrooms = 1, bathrooms = 1, areaM2 = 1.0,
                createdAt = "", description = null, amenities = emptyList(),
                thumbnailUrl = null, photos = emptyList(),
            ),
            probability = 0.7,
            matchScore = 73,
            factors = emptyList(),
        )
        assertEquals("room-xyz", result.roomId)

        val route = RoomDetailRoute(result.roomId)
        assertTrue(route.route.startsWith(RoomDetailRoute.PATTERN))
        assertTrue(route.route.contains("room-xyz"))
        assertFalse(route.route.contains("propiedad"))
        assertFalse(route.route.contains("property_detail"))
    }

    @Test
    fun `10 room route validates id and supports back argument`() {
        assertTrue(RoomDetailRoute.isValid("room-1"))
        assertFalse(RoomDetailRoute.isValid(null))
        assertFalse(RoomDetailRoute.isValid(" "))
        assertEquals("roomId", RoomDetailRoute.ID_ARGUMENT)
    }

    // ---------- 12: screen state rendering ----------
    @Test
    fun `12 content state exposes all real fields for screen`() = runTest {
        val vm = vm("room-1", FakeRoomApi())
        advanceUntilIdle()
        val room = (vm.uiState.value as RoomDetailUiState.Content).room
        assertNotNull(room.title)
        assertNotNull(room.building.name)
        assertTrue(room.photos.isNotEmpty())
        assertEquals("2026-12-01", room.availableFrom)
        assertEquals("6 meses", room.minContract)
    }

    // ---------- helpers ----------
    private fun roomDto() = RoomDetailResponseDto(
        id = "room-1", buildingId = "b-1", title = "Cuarto", price = 4500, currency = "MXN",
        roomType = "private", bathroom = "private", sizeM2 = 3.5,
        furniture = listOf("cama"), availableFrom = "2026-12-01", minContract = "6 meses",
        distanceKm = 1.4, walkingMinutes = 18,
        approxLocation = ApproxLocationDto(19.1, -99.1),
        nearestUniversity = com.thehouse.app.data.remote.dto.room.RoomUniversityDto("u1", "UNIVER", "Universidad"),
        building = RoomBuildingDto("b-1", "Edificio", "student_residence", "any", listOf("wifi", "water"), "No fumar"),
        thumbnailUrl = null, demo = false,
        photos = listOf(RoomPhotoDto("p1", "/api/v1/media/p1", "/api/v1/media/p1/thumb", 800, 600, 1234, "2026-11-01T00:00:00Z")),
    )

    private fun vm(id: String, api: FakeRoomApi) = RoomDetailViewModel(id, GetRoomDetailUseCase(RoomRepository(api)))

    private class FakeRoomApi(
        val error: Throwable? = null,
    ) : RoomApi {
        var calls = 0
            private set
        var lastId: String? = null
            private set
        override suspend fun getRoom(id: String): RoomDetailResponseDto {
            calls++
            lastId = id
            error?.let { throw it }
            return RoomDetailResponseDto(
                id = id, buildingId = "b-1", title = "Cuarto", price = 4500, currency = "MXN",
                roomType = "private", bathroom = "private", sizeM2 = 3.5,
                furniture = listOf("cama"), availableFrom = "2026-12-01", minContract = "6 meses",
                distanceKm = 1.4, walkingMinutes = 18,
                approxLocation = ApproxLocationDto(19.1, -99.1),
                nearestUniversity = com.thehouse.app.data.remote.dto.room.RoomUniversityDto("u1", "UNIVER", "Universidad"),
                building = RoomBuildingDto("b-1", "Edificio", "student_residence", "any", listOf("wifi"), "No fumar"),
                thumbnailUrl = null, demo = false,
                photos = listOf(RoomPhotoDto("p1", "/api/v1/media/p1", "/api/v1/media/p1/thumb", 800, 600, 1234, "2026-11-01T00:00:00Z")),
            )
        }
    }
}
