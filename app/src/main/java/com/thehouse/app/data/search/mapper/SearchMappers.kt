package com.thehouse.app.data.search.mapper

import com.thehouse.app.data.remote.dto.search.ApproxLocationDto
import com.thehouse.app.data.remote.dto.search.PlaceItemDto
import com.thehouse.app.data.remote.dto.search.RoomDetailDto
import com.thehouse.app.data.remote.dto.search.RoomMarkerDto
import com.thehouse.app.data.remote.dto.search.RoomSearchItemDto
import com.thehouse.app.data.remote.dto.search.SearchBuildingDto
import com.thehouse.app.data.remote.dto.search.SearchUniversityDto
import com.thehouse.app.domain.model.GeoPoint
import com.thehouse.app.domain.model.PlaceSuggestion
import com.thehouse.app.domain.model.RoomBuilding
import com.thehouse.app.domain.model.RoomDetail
import com.thehouse.app.domain.model.RoomMarker
import com.thehouse.app.domain.model.RoomSummary
import com.thehouse.app.domain.model.RoomUniversity

/** Mapeo DTO → Domain (búsqueda NV-14). Solo transformación. */
object SearchMappers {

    fun ApproxLocationDto.toDomain(): GeoPoint = GeoPoint(latitude = lat, longitude = lng)

    fun RoomSearchItemDto.toDomain(): RoomSummary = RoomSummary(
        id = id,
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

    fun RoomMarkerDto.toDomain(): RoomMarker = RoomMarker(
        id = id,
        price = price,
        approxLocation = approxLocation.toDomain(),
        distanceKm = distanceKm,
    )

    fun SearchUniversityDto.toDomain(): RoomUniversity = RoomUniversity(
        id = id,
        code = code,
        name = name,
    )

    fun SearchBuildingDto.toDomain(): RoomBuilding = RoomBuilding(
        id = id,
        name = name,
        type = type,
        allowedTenants = allowedTenants,
        services = services,
        rules = rules,
    )

    fun RoomDetailDto.toDomain(): RoomDetail = RoomDetail(
        id = id,
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
        approxLocation = approxLocation.toDomain(),
        nearestUniversity = nearestUniversity?.toDomain(),
        building = building.toDomain(),
        thumbnailUrl = thumbnailUrl,
        demo = demo,
    )

    fun PlaceItemDto.toDomain(): PlaceSuggestion = PlaceSuggestion(
        id = id,
        name = name,
        type = type,
        approxLocation = approxLocation.toDomain(),
        distanceKm = distanceKm,
    )
}
