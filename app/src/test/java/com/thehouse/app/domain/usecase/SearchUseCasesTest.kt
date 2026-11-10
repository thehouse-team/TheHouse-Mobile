package com.thehouse.app.domain.usecase

import com.thehouse.app.domain.error.DomainError
import com.thehouse.app.domain.error.DomainException
import com.thehouse.app.domain.model.MapBounds
import com.thehouse.app.domain.model.PagedResult
import com.thehouse.app.domain.model.PlaceSuggestion
import com.thehouse.app.domain.model.Property
import com.thehouse.app.domain.model.RoomDetail
import com.thehouse.app.domain.model.RoomMarker
import com.thehouse.app.domain.model.RoomSearchFilters
import com.thehouse.app.domain.model.RoomSummary
import com.thehouse.app.domain.repository.PropertyRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchUseCasesTest {

    private val universityId = "6578a8b8c29c000000000001"
    private val bounds = MapBounds(-98.37, 20.07, -98.35, 20.09)

    private class RecordingRepository : PropertyRepository {
        var searchCalls = 0
        var mapCalls = 0
        var placesCalls = 0
        var roomCalls = 0

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
            searchCalls++
            return Result.success(PagedResult(emptyList(), page, pageSize, 0, 0))
        }

        override suspend fun getRoomMapMarkers(
            universityId: String,
            filters: RoomSearchFilters,
            bounds: MapBounds,
        ): Result<List<RoomMarker>> {
            mapCalls++
            return Result.success(emptyList())
        }

        override suspend fun searchPlaces(
            query: String,
            universityId: String,
        ): Result<List<PlaceSuggestion>> {
            placesCalls++
            return Result.success(emptyList())
        }

        override suspend fun getRoomById(id: String): Result<RoomDetail> {
            roomCalls++
            return Result.failure(IllegalStateException("No usado"))
        }
    }

    private fun assertBadRequest(result: Result<*>) {
        assertTrue(result.isFailure)
        assertEquals(
            DomainError.BadRequest,
            (result.exceptionOrNull() as DomainException).error,
        )
    }

    @Test
    fun `search rechaza universidad que no es ObjectId sin llamar repositorio`() = runTest {
        val repo = RecordingRepository()
        val result = SearchRoomsUseCase(repo)("uni-1", RoomSearchFilters())
        assertBadRequest(result)
        assertEquals(0, repo.searchCalls)
    }

    @Test
    fun `search rechaza precio fuera de catalogo`() = runTest {
        val repo = RecordingRepository()
        val result = SearchRoomsUseCase(repo)(
            universityId,
            RoomSearchFilters(minPrice = 499),
        )
        assertBadRequest(result)
        assertEquals(0, repo.searchCalls)
    }

    @Test
    fun `search rechaza filtro no soportado por el contrato`() = runTest {
        val repo = RecordingRepository()
        val result = SearchRoomsUseCase(repo)(
            universityId,
            RoomSearchFilters(roomType = "suite"),
        )
        assertBadRequest(result)
        assertEquals(0, repo.searchCalls)
    }

    @Test
    fun `search rechaza paginacion invalida`() = runTest {
        val repo = RecordingRepository()
        val result = SearchRoomsUseCase(repo)(
            universityId,
            RoomSearchFilters(),
            page = 0,
            pageSize = 51,
        )
        assertBadRequest(result)
        assertEquals(0, repo.searchCalls)
    }

    @Test
    fun `search valido delega en repositorio`() = runTest {
        val repo = RecordingRepository()
        val result = SearchRoomsUseCase(repo)(universityId, RoomSearchFilters())
        assertTrue(result.isSuccess)
        assertEquals(1, repo.searchCalls)
    }

    @Test
    fun `map rechaza viewport mayor a 03 grados sin llamar repositorio`() = runTest {
        val repo = RecordingRepository()
        val invalid = MapBounds(-98.5, 20.0, -98.0, 20.5)
        val result = GetRoomMapUseCase(repo)(universityId, RoomSearchFilters(), invalid)
        assertBadRequest(result)
        assertEquals(0, repo.mapCalls)
    }

    @Test
    fun `map valido delega en repositorio`() = runTest {
        val repo = RecordingRepository()
        val result = GetRoomMapUseCase(repo)(universityId, RoomSearchFilters(), bounds)
        assertTrue(result.isSuccess)
        assertEquals(1, repo.mapCalls)
    }

    @Test
    fun `places rechaza query de un caracter`() = runTest {
        val repo = RecordingRepository()
        val result = SearchPlacesUseCase(repo)("a", universityId)
        assertBadRequest(result)
        assertEquals(0, repo.placesCalls)
    }

    @Test
    fun `places valido delega en repositorio`() = runTest {
        val repo = RecordingRepository()
        val result = SearchPlacesUseCase(repo)("centro", universityId)
        assertTrue(result.isSuccess)
        assertEquals(1, repo.placesCalls)
    }

    @Test
    fun `room detail rechaza id blank`() = runTest {
        val repo = RecordingRepository()
        assertBadRequest(GetRoomByIdUseCase(repo)(" "))
        assertEquals(0, repo.roomCalls)
    }
}
