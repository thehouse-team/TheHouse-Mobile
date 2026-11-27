package com.thehouse.app.data.room

import com.thehouse.app.data.error.DataError
import com.thehouse.app.data.error.toDomainError
import com.thehouse.app.data.remote.api.RoomApi
import com.thehouse.app.data.remote.dto.room.ApproxLocationDto
import com.thehouse.app.data.remote.dto.room.RoomSearchItemDto
import com.thehouse.app.data.remote.dto.room.RoomSearchResponseDto
import com.thehouse.app.domain.error.DomainException
import com.thehouse.app.domain.room.ApproxLocation
import com.thehouse.app.domain.room.RoomSearchFilters
import com.thehouse.app.domain.room.RoomSearchItem
import com.thehouse.app.domain.room.RoomSearchPage
import com.thehouse.app.domain.usecase.RoomSearchRepository
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * Implementación real de [RoomSearchRepository] contra `GET /api/v1/rooms/search`.
 * Sin datos hardcodeados: cada campo del DTO se mapea 1:1 al dominio.
 */
class RoomSearchRepositoryImpl(
    private val api: RoomApi,
) : RoomSearchRepository {

    override suspend fun searchRooms(filters: RoomSearchFilters): Result<RoomSearchPage> = try {
        com.thehouse.app.presentation.catalog.CatalogFlowLogger.search(
            "network_request", "source", "network",
            "universityId_set", filters.universityId.isNotBlank(),
            "page", filters.page, "pageSize", filters.pageSize,
        )
        val response = api.searchRooms(
            universityId = filters.universityId,
            minPrice = filters.minPrice,
            maxPrice = filters.maxPrice,
            roomType = filters.roomType,
            bathroom = filters.bathroom,
            buildingType = filters.buildingType,
            allowedTenants = filters.allowedTenants,
            services = filters.services.joinToString(",").ifBlank { null },
            maxDistanceKm = filters.maxDistanceKm,
            sort = filters.sort,
            page = filters.page,
            pageSize = filters.pageSize,
        )
        val page = response.toDomain()
        com.thehouse.app.presentation.catalog.CatalogFlowLogger.search(
            "deserialization_result", "source", "network", "success", true,
            "result_count", page.items.size, "total", page.totalItems,
        )
        Result.success(page)
    } catch (cancelled: CancellationException) {
        com.thehouse.app.presentation.catalog.CatalogFlowLogger.search(
            "error_type", "type", "cancellation",
        )
        throw cancelled
    } catch (t: Throwable) {
        val error = t.toSearchDomainError()
        com.thehouse.app.presentation.catalog.CatalogFlowLogger.search(
            "error_type", "type", t::class.simpleName.orEmpty(), "domain", error::class.simpleName.orEmpty(),
        )
        Result.failure(DomainException(error))
    }
}

private fun Throwable.toSearchDomainError() = when (this) {
    is DomainException -> this.error
    is HttpException -> DataError.Http(code()).toDomainError()
    is SocketTimeoutException -> DataError.Timeout.toDomainError()
    is IOException -> DataError.Network.toDomainError()
    else -> DataError.Serialization.toDomainError()
}

internal fun RoomSearchResponseDto.toDomain() = RoomSearchPage(
    items = items.map { it.toDomain() },
    page = page,
    pageSize = pageSize,
    totalItems = totalItems,
    totalPages = totalPages,
)

internal fun RoomSearchItemDto.toDomain() = RoomSearchItem(
    roomId = id,
    buildingId = buildingId,
    title = title,
    price = price,
    currency = currency,
    roomType = roomType,
    bathroom = bathroom,
    buildingType = buildingType,
    allowedTenants = allowedTenants,
    services = services,
    distanceKm = distanceKm,
    walkingMinutes = walkingMinutes,
    approxLocation = approxLocation.toDomain(),
    thumbnailUrl = thumbnailUrl,
    demo = demo,
)

internal fun ApproxLocationDto.toDomain() = ApproxLocation(lat, lng)
