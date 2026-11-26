package com.thehouse.app.data.remote.dto.room

import kotlinx.serialization.Serializable

/** Espejo exacto de `TheHouse.Application.Contracts.Search.RoomDetailResponse` (Backend origin/main). */
@Serializable
data class RoomDetailResponseDto(
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
    val nearestUniversity: RoomUniversityDto? = null,
    val building: RoomBuildingDto,
    val thumbnailUrl: String? = null,
    val demo: Boolean = false,
    val photos: List<RoomPhotoDto>? = null,
)

@Serializable
data class ApproxLocationDto(val lat: Double, val lng: Double)

@Serializable
data class RoomUniversityDto(val id: String, val code: String, val name: String)

@Serializable
data class RoomBuildingDto(
    val id: String,
    val name: String,
    val type: String,
    val allowedTenants: String,
    val services: List<String> = emptyList(),
    val rules: String? = null,
)

@Serializable
data class RoomPhotoDto(
    val id: String,
    val url: String,
    val thumbUrl: String,
    val width: Int,
    val height: Int,
    val bytes: Long,
    val createdAt: String,
)
