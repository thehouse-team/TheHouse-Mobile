package com.thehouse.app.presentation.explore

import com.thehouse.app.domain.room.ApproxLocation
import com.thehouse.app.domain.room.RoomSearchFilters
import com.thehouse.app.domain.room.RoomSearchItem
import com.thehouse.app.domain.room.RoomSearchPage
import com.thehouse.app.domain.usecase.RoomSearchRepository
import com.thehouse.app.domain.usecase.SearchRoomsUseCase
import com.thehouse.app.testutil.TestMainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

private class RecordingRoomSearchRepository : RoomSearchRepository {
    val calls = mutableListOf<RoomSearchFilters>()
    var pages = 1

    override suspend fun searchRooms(filters: RoomSearchFilters): Result<RoomSearchPage> {
        calls += filters
        return Result.success(
            RoomSearchPage(
                items = listOf(
                    RoomSearchItem(
                        roomId = "room-1",
                        buildingId = "b-1",
                        title = "Cuarto real",
                        price = 3500,
                        currency = "MXN",
                        roomType = "single",
                        bathroom = "private",
                        buildingType = "rooms_building",
                        allowedTenants = "women",
                        services = listOf("wifi"),
                        distanceKm = 2.0,
                        walkingMinutes = 25,
                        approxLocation = ApproxLocation(19.5, -99.3),
                        thumbnailUrl = null,
                        demo = false,
                    ),
                ),
                page = filters.page,
                pageSize = 20,
                totalItems = 1,
                totalPages = pages,
            ),
        )
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ExploreFilterDraftTest {

    @get:Rule
    val dispatcherRule = TestMainDispatcherRule(UnconfinedTestDispatcher())

    private fun vm(repo: RecordingRoomSearchRepository): ExploreViewModel {
        val model = ExploreViewModel(
            searchRooms = SearchRoomsUseCase(repo),
            getUniversities = null,
        )
        model.onEvent(ExploreEvent.UniversityChanged("u-1"))
        repo.calls.clear() // descarta la consulta inicial de la universidad
        return model
    }

    @Test
    fun `partial price draft does not fire queries and preserves raw text`() = runTest {
        val repo = RecordingRoomSearchRepository()
        val model = vm(repo)

        model.onEvent(
            ExploreEvent.DraftFilterChanged(
                FilterDraftUi.from(model.uiState.value.filters).copy(minPriceRaw = "5"),
            ),
        )
        model.onEvent(
            ExploreEvent.DraftFilterChanged(
                FilterDraftUi.from(model.uiState.value.filters).copy(minPriceRaw = "50"),
            ),
        )
        advanceUntilIdle()

        assertEquals("50", model.uiState.value.filterDraft.minPriceRaw)
        assertNull(model.uiState.value.filters.minPrice)
        assertTrue(repo.calls.isEmpty())
    }

    @Test
    fun `apply converts once and fires one query with contractual params`() = runTest {
        val repo = RecordingRoomSearchRepository()
        val model = vm(repo)
        model.onEvent(
            ExploreEvent.DraftFilterChanged(
                FilterDraftUi.from(model.uiState.value.filters).copy(
                    minPriceRaw = "3500",
                    maxPriceRaw = "5000",
                    distanceRaw = "12.5",
                    base = model.uiState.value.filterDraft.base.copy(
                        roomType = "single",
                        sort = "price",
                    ),
                ),
            ),
        )
        model.onEvent(ExploreEvent.ApplyFilters)
        advanceUntilIdle()

        assertEquals(1, repo.calls.size)
        val call = repo.calls.single()
        assertEquals(3500, call.minPrice)
        assertEquals(5000, call.maxPrice)
        assertEquals(12.5, call.maxDistanceKm, 0.0)
        assertEquals("single", call.roomType)
        assertEquals("price", call.sort)
        assertEquals(1, call.page)
    }

    @Test
    fun `invalid draft blocks apply without query`() = runTest {
        val repo = RecordingRoomSearchRepository()
        val model = vm(repo)
        model.onEvent(
            ExploreEvent.DraftFilterChanged(
                FilterDraftUi.from(model.uiState.value.filters).copy(minPriceRaw = "49"),
            ),
        )
        model.onEvent(ExploreEvent.ApplyFilters)
        advanceUntilIdle()

        assertTrue(repo.calls.isEmpty())
        assertTrue(model.uiState.value.filterValidationError!!.contains("500"))
    }

    @Test
    fun `clear restores defaults preserving university and fires one query`() = runTest {
        val repo = RecordingRoomSearchRepository()
        val model = vm(repo)
        model.onEvent(
            ExploreEvent.DraftFilterChanged(
                FilterDraftUi.from(model.uiState.value.filters).copy(minPriceRaw = "3500"),
            ),
        )
        model.onEvent(ExploreEvent.ApplyFilters)
        advanceUntilIdle()
        repo.calls.clear()

        model.onEvent(ExploreEvent.ClearFilters)
        advanceUntilIdle()

        assertEquals(1, repo.calls.size)
        assertEquals("u-1", repo.calls.single().universityId)
        assertNull(repo.calls.single().minPrice)
        assertFalse(repo.calls.single().roomType != null)
        assertEquals("", model.uiState.value.filterDraft.minPriceRaw)
    }

    @Test
    fun `clear with no optional filters does not duplicate query`() = runTest {
        val repo = RecordingRoomSearchRepository()
        val model = vm(repo)
        model.onEvent(ExploreEvent.ClearFilters)
        advanceUntilIdle()
        assertTrue(repo.calls.isEmpty())
    }

    @Test
    fun `dismiss without applying does not change active search`() = runTest {
        val repo = RecordingRoomSearchRepository()
        val model = vm(repo)
        model.onEvent(ExploreEvent.ToggleFilters) // abre: sincroniza borrador
        model.onEvent(
            ExploreEvent.DraftFilterChanged(
                FilterDraftUi.from(model.uiState.value.filters).copy(maxPriceRaw = "9999"),
            ),
        )
        model.onEvent(ExploreEvent.ToggleFilters) // cierra sin aplicar
        advanceUntilIdle()

        assertTrue(repo.calls.isEmpty())
        assertNull(model.uiState.value.filters.maxPrice)
    }

    @Test
    fun `quick chip is based on applied filters not dirty draft`() = runTest {
        val repo = RecordingRoomSearchRepository()
        val model = vm(repo)
        model.onEvent(ExploreEvent.ToggleFilters)
        model.onEvent(
            ExploreEvent.DraftFilterChanged(
                FilterDraftUi.from(model.uiState.value.filters).copy(minPriceRaw = "3500"),
            ),
        )
        model.onEvent(
            ExploreEvent.FilterChanged(model.uiState.value.filters.copy(roomType = "single")),
        )
        advanceUntilIdle()

        assertEquals(1, repo.calls.size)
        assertNull(repo.calls.single().minPrice)
        assertEquals("single", repo.calls.single().roomType)
    }
}
