package com.thehouse.app.data.remote.dto.search

import kotlinx.serialization.Serializable

/**
 * DTOs exactos del contrato NV-14 consumido por Mobile (D10).
 * Nombres camelCase del Backend ASP.NET. Sin campos inventados.
 */
@Serializable
data class ApproxLocationDto(
    val lat: Double,
    val lng: Double,
)

@Serializable
data class RoomSearchItemDto(
    val id: String,
    val buildingId: String,
    val title: String,
    val price: Int,
    val currency: String,
    val roomType: String? = null,
    val bathroom: String? = null,
    val buildingType: String,
    val allowedTenants: String,
    val services: List<String> = emptyList(),
    val distanceKm: Double,
    val walkingMinutes: Int,
    val approxLocation: ApproxLocationDto,
    val thumbnailUrl: String? = null,
    val demo: Boolean = false,
)

@Serializable
data class RoomSearchResponseDto(
    val items: List<RoomSearchItemDto> = emptyList(),
    val page: Int = 1,
    val pageSize: Int = 20,
    val totalItems: Long = 0,
    val totalPages: Int = 0,
)

@Serializable
data class RoomMarkerDto(
    val id: String,
    val price: Int,
    val approxLocation: ApproxLocationDto,
    val distanceKm: Double,
)

@Serializable
data class RoomMapResponseDto(
    val items: List<RoomMarkerDto> = emptyList(),
)

@Serializable
data class SearchUniversityDto(
    val id: String,
    val code: String,
    val name: String,
)

@Serializable
data class SearchBuildingDto(
    val id: String,
    val name: String,
    val type: String,
    val allowedTenants: String,
    val services: List<String> = emptyList(),
    val rules: String? = null,
)

@Serializable
data class RoomDetailDto(
    val id: String,
    val buildingId: String,
    val title: String,
    val price: Int,
    val currency: String,
    val roomType: String? = null,
    val bathroom: String? = null,
    val sizeM2: Double? = null,
    val furniture: List<String> = emptyList(),
    val availableFrom: String? = null,
    val minContract: String? = null,
    val distanceKm: Double,
    val walkingMinutes: Int,
    val approxLocation: ApproxLocationDto,
    val nearestUniversity: SearchUniversityDto? = null,
    val building: SearchBuildingDto,
    val thumbnailUrl: String? = null,
    val demo: Boolean = false,
)

@Serializable
data class PlaceItemDto(
    val id: String,
    val name: String,
    val type: String,
    val approxLocation: ApproxLocationDto,
    val distanceKm: Double,
)

@Serializable
data class PlaceSearchResponseDto(
    val items: List<PlaceItemDto> = emptyList(),
)
