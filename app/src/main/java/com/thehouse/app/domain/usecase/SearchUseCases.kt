package com.thehouse.app.domain.usecase

import com.thehouse.app.domain.error.DomainError
import com.thehouse.app.domain.error.DomainException
import com.thehouse.app.domain.model.MapBounds
import com.thehouse.app.domain.model.PagedResult
import com.thehouse.app.domain.model.PlaceSuggestion
import com.thehouse.app.domain.model.RoomDetail
import com.thehouse.app.domain.model.RoomMarker
import com.thehouse.app.domain.model.RoomSearchFilters
import com.thehouse.app.domain.model.RoomSummary
import com.thehouse.app.domain.model.SearchCatalogs
import com.thehouse.app.domain.repository.PropertyRepository

/**
 * Casos de uso NV-07 D10. Validan antes de llamar y no inventan filtros:
 * los parámetros enviados existen en el contrato de Backend.
 */
class SearchRoomsUseCase(private val repository: PropertyRepository) {

    suspend operator fun invoke(
        universityId: String,
        filters: RoomSearchFilters,
        page: Int = 1,
        pageSize: Int = DEFAULT_PAGE_SIZE,
    ): Result<PagedResult<RoomSummary>> {
        validate(universityId, filters, page, pageSize)?.let { return Result.failure(it) }
        return repository.searchRooms(universityId, filters, page, pageSize)
    }

    private fun validate(
        universityId: String,
        filters: RoomSearchFilters,
        page: Int,
        pageSize: Int,
    ): DomainException? {
        if (!isObjectId(universityId)) return badRequest()
        if (page < 1 || pageSize !in 1..MAX_PAGE_SIZE) return badRequest()
        val min = filters.minPrice
        val max = filters.maxPrice
        if (min != null && min !in SearchCatalogs.MIN_PRICE..SearchCatalogs.MAX_PRICE) return badRequest()
        if (max != null && max !in SearchCatalogs.MIN_PRICE..SearchCatalogs.MAX_PRICE) return badRequest()
        if (min != null && max != null && min > max) return badRequest()
        if (filters.maxDistanceKm !in
            SearchCatalogs.MIN_DISTANCE_KM..SearchCatalogs.MAX_DISTANCE_KM
        ) {
            return badRequest()
        }
        if (filters.roomType != null && filters.roomType !in SearchCatalogs.roomTypes) return badRequest()
        if (filters.bathroom != null && filters.bathroom !in SearchCatalogs.bathrooms) return badRequest()
        if (filters.buildingType != null && filters.buildingType !in SearchCatalogs.buildingTypes) {
            return badRequest()
        }
        if (filters.allowedTenants != null && filters.allowedTenants !in SearchCatalogs.allowedTenants) {
            return badRequest()
        }
        if (filters.services.any { it !in SearchCatalogs.services }) return badRequest()
        return null
    }

    private fun badRequest() = DomainException(DomainError.BadRequest)

    private fun isObjectId(value: String): Boolean =
        value.length == 24 && value.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }

    companion object {
        const val DEFAULT_PAGE_SIZE = 20
        const val MAX_PAGE_SIZE = 50
    }
}

class GetRoomMapUseCase(private val repository: PropertyRepository) {

    suspend operator fun invoke(
        universityId: String,
        filters: RoomSearchFilters,
        bounds: MapBounds,
    ): Result<List<RoomMarker>> {
        if (!isObjectId(universityId) || !bounds.isValid()) {
            return Result.failure(DomainException(DomainError.BadRequest))
        }
        return repository.getRoomMapMarkers(universityId, filters, bounds)
    }

    private fun isObjectId(value: String): Boolean =
        value.length == 24 && value.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }
}

class SearchPlacesUseCase(private val repository: PropertyRepository) {

    suspend operator fun invoke(
        query: String,
        universityId: String,
    ): Result<List<PlaceSuggestion>> {
        val trimmed = query.trim()
        if (trimmed.length !in MIN_QUERY_LENGTH..MAX_QUERY_LENGTH || !isObjectId(universityId)) {
            return Result.failure(DomainException(DomainError.BadRequest))
        }
        return repository.searchPlaces(trimmed, universityId)
    }

    private fun isObjectId(value: String): Boolean =
        value.length == 24 && value.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }

    companion object {
        const val MIN_QUERY_LENGTH = 2
        const val MAX_QUERY_LENGTH = 60
    }
}

class GetRoomByIdUseCase(private val repository: PropertyRepository) {
    suspend operator fun invoke(id: String): Result<RoomDetail> =
        if (id.isBlank()) {
            Result.failure(DomainException(DomainError.BadRequest))
        } else {
            repository.getRoomById(id)
        }
}
