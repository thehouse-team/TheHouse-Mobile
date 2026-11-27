package com.thehouse.app.domain.room

/** Modelos mínimos del detalle de cuarto. `roomId` es la identidad real. */
data class ApproxLocation(val lat: Double, val lng: Double)

data class RoomUniversity(val id: String, val code: String, val name: String)

data class RoomBuilding(
    val id: String,
    val name: String,
    val type: String,
    val allowedTenants: String,
    val services: List<String>,
    val rules: String?,
)

data class RoomPhoto(
    val id: String,
    val url: String,
    val thumbUrl: String,
    val width: Int,
    val height: Int,
    val bytes: Long,
    val createdAt: String,
)

data class RoomDetail(
    val roomId: String,
    val buildingId: String,
    val title: String,
    val price: Int,
    val currency: String,
    val roomType: String?,
    val bathroom: String?,
    val sizeM2: Double?,
    val furniture: List<String>,
    val availableFrom: String?,
    val minContract: String?,
    val distanceKm: Double,
    val walkingMinutes: Int,
    val approxLocation: ApproxLocation,
    val nearestUniversity: RoomUniversity?,
    val building: RoomBuilding,
    val thumbnailUrl: String?,
    val demo: Boolean,
    val photos: List<RoomPhoto>,
)
