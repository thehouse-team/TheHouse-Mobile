package com.thehouse.app.data.room

import com.thehouse.app.data.error.DataError
import com.thehouse.app.data.error.toDomainError
import com.thehouse.app.data.remote.api.RoomApi
import com.thehouse.app.data.remote.dto.room.RoomDetailResponseDto
import com.thehouse.app.domain.error.DomainException
import com.thehouse.app.domain.room.ApproxLocation
import com.thehouse.app.domain.room.RoomBuilding
import com.thehouse.app.domain.room.RoomDetail
import com.thehouse.app.domain.room.RoomPhoto
import com.thehouse.app.domain.room.RoomUniversity
import com.thehouse.app.domain.usecase.RoomDetailRepository
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

/** Implementación real de [RoomDetailRepository] contra `GET /api/v1/rooms/{id}`. */
class RoomRepository(
    private val api: RoomApi,
) : RoomDetailRepository {

    override suspend fun getRoomDetail(roomId: String): Result<RoomDetail> = try {
        Result.success(api.getRoom(roomId).toDomain())
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (t: Throwable) {
        Result.failure(DomainException(t.toRoomDomainError()))
    }
}

private fun Throwable.toRoomDomainError() = when (this) {
    is DomainException -> this.error
    is HttpException -> DataError.Http(code()).toDomainError()
    is SocketTimeoutException -> DataError.Timeout.toDomainError()
    is IOException -> DataError.Network.toDomainError()
    else -> DataError.Serialization.toDomainError()
}

internal fun RoomDetailResponseDto.toDomain() = RoomDetail(
    roomId = id,
    buildingId = buildingId,
    title = title,
    price = price,
    currency = currency,
    roomType = roomType,
    bathroom = bathroom,
    sizeM2 = sizeM2,
    furniture = furniture,
    availableFrom = availableFrom,
    minContract = minContract,
    distanceKm = distanceKm,
    walkingMinutes = walkingMinutes,
    approxLocation = ApproxLocation(approxLocation.lat, approxLocation.lng),
    nearestUniversity = nearestUniversity?.let { RoomUniversity(it.id, it.code, it.name) },
    building = RoomBuilding(
        id = building.id,
        name = building.name,
        type = building.type,
        allowedTenants = building.allowedTenants,
        services = building.services,
        rules = building.rules,
    ),
    thumbnailUrl = thumbnailUrl,
    demo = demo,
    photos = photos.orEmpty().map {
        RoomPhoto(it.id, it.url, it.thumbUrl, it.width, it.height, it.bytes, it.createdAt)
    },
)
