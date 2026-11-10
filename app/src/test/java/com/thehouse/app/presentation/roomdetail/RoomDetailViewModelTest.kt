package com.thehouse.app.presentation.roomdetail

import com.thehouse.app.domain.error.DomainError
import com.thehouse.app.domain.error.DomainException
import com.thehouse.app.domain.model.GeoPoint
import com.thehouse.app.domain.model.MapBounds
import com.thehouse.app.domain.model.PagedResult
import com.thehouse.app.domain.model.PlaceSuggestion
import com.thehouse.app.domain.model.Property
import com.thehouse.app.domain.model.RoomBuilding
import com.thehouse.app.domain.model.RoomDetail
import com.thehouse.app.domain.model.RoomMarker
import com.thehouse.app.domain.model.RoomSearchFilters
import com.thehouse.app.domain.model.RoomSummary
import com.thehouse.app.domain.repository.PropertyRepository
import com.thehouse.app.domain.usecase.GetRoomByIdUseCase
import com.thehouse.app.presentation.common.UiError
import com.thehouse.app.testutil.TestMainDispatcherRule
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

private const val ROOM_ID = "6578a8b8c29c000000000010"

private val roomDetail = RoomDetail(
    id = ROOM_ID,
    buildingId = "6578a8b8c29c000000000011",
    title = "Cuarto individual",
    price = 3500,
    currency = "MXN",
    roomType = "single",
    bathroom = "shared",
    sizeM2 = 14.0,
    furniture = listOf("bed", "desk"),
    availableFrom = "2026-12-01",
    minContract = "six_months",
    distanceKm = 1.2,
    walkingMinutes = 15,
    approxLocation = GeoPoint(20.08, -98.36),
    nearestUniversity = null,
    building = RoomBuilding(
        id = "6578a8b8c29c000000000011",
        name = "Casa Universidad",
        type = "house_rooms",
        allowedTenants = "any",
        services = listOf("wifi"),
        rules = "No fiestas",
    ),
    thumbnailUrl = null,
    demo = true,
)

private class FakeRoomRepository(
    var result: Result<RoomDetail>,
) : PropertyRepository {
    override suspend fun getProperties(page: Int, pageSize: Int) =
        Result.success(PagedResult(emptyList<Property>(), page, pageSize, 0, 0))

    override suspend fun getPropertyById(id: String): Result<Property> =
        Result.failure(IllegalStateException("No usado"))

    override suspend fun searchRooms(
        universityId: String,
        filters: RoomSearchFilters,
        page: Int,
        pageSize: Int,
    ): Result<PagedResult<RoomSummary>> =
        Result.success(PagedResult(emptyList(), page, pageSize, 0, 0))

    override suspend fun getRoomMapMarkers(
        universityId: String,
        filters: RoomSearchFilters,
        bounds: MapBounds,
    ): Result<List<RoomMarker>> = Result.success(emptyList())

    override suspend fun searchPlaces(
        query: String,
        universityId: String,
    ): Result<List<PlaceSuggestion>> = Result.success(emptyList())

    override suspend fun getRoomById(id: String): Result<RoomDetail> = result
}

class RoomDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = TestMainDispatcherRule(StandardTestDispatcher())

    @Test
    fun `detail 200 muestra Content real`() = runTest {
        val vm = RoomDetailViewModel(
            roomId = ROOM_ID,
            getRoomById = GetRoomByIdUseCase(FakeRoomRepository(Result.success(roomDetail))),
        )
        runCurrent()
        val content = vm.uiState.value as RoomDetailUiState.Content
        assertEquals("Cuarto individual", content.room.title)
        assertEquals("Casa Universidad", content.room.building.name)
        assertEquals(listOf("bed", "desk"), content.room.furniture)
    }

    @Test
    fun `detail 404 muestra NotFound`() = runTest {
        val vm = RoomDetailViewModel(
            roomId = ROOM_ID,
            getRoomById = GetRoomByIdUseCase(
                FakeRoomRepository(Result.failure(DomainException(DomainError.NotFound))),
            ),
        )
        runCurrent()
        assertTrue(vm.uiState.value is RoomDetailUiState.NotFound)
    }

    @Test
    fun `detail 401 informa y pide login`() = runTest {
        val vm = RoomDetailViewModel(
            roomId = ROOM_ID,
            getRoomById = GetRoomByIdUseCase(
                FakeRoomRepository(Result.failure(DomainException(DomainError.Unauthorized))),
            ),
        )
        val effect = async(start = CoroutineStart.UNDISPATCHED) { vm.effects.first() }
        runCurrent()

        assertEquals(
            UiError.Unauthorized,
            (vm.uiState.value as RoomDetailUiState.Error).error,
        )
        assertEquals(RoomDetailEffect.RequireLogin, effect.await())
    }

    @Test
    fun `network error y retry recupera`() = runTest {
        val repo = FakeRoomRepository(Result.failure(DomainException(DomainError.Network)))
        val vm = RoomDetailViewModel(
            roomId = ROOM_ID,
            getRoomById = GetRoomByIdUseCase(repo),
        )
        runCurrent()
        assertTrue(vm.uiState.value is RoomDetailUiState.Error)

        repo.result = Result.success(roomDetail)
        vm.onEvent(RoomDetailEvent.Retry)
        advanceUntilIdle()
        assertTrue(vm.uiState.value is RoomDetailUiState.Content)
    }
}
