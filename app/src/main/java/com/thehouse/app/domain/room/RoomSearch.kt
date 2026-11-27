package com.thehouse.app.domain.room

/**
 * Modelo de dominio de un resultado de `GET /api/v1/rooms/search`.
 * `roomId` es la identidad real; nunca se usa propertyId para navegar.
 * Sin dirección exacta: `approxLocation` es la celda publicada por el Backend.
 */
data class RoomSearchItem(
    val roomId: String,
    val buildingId: String,
    val title: String,
    val price: Int,
    val currency: String,
    val roomType: String?,
    val bathroom: String?,
    val buildingType: String,
    val allowedTenants: String,
    val services: List<String>,
    val distanceKm: Double,
    val walkingMinutes: Int,
    val approxLocation: ApproxLocation,
    val thumbnailUrl: String?,
    val demo: Boolean,
)

data class RoomSearchPage(
    val items: List<RoomSearchItem>,
    val page: Int,
    val pageSize: Int,
    val totalItems: Long,
    val totalPages: Int,
)

/**
 * Filtros contractuales de `rooms/search`. Los códigos se envían tal cual al
 * Backend (fuente de verdad). Los catálogos cerrados viven en `RoomSearchCatalog`.
 */
data class RoomSearchFilters(
    val universityId: String,
    val minPrice: Int? = null,
    val maxPrice: Int? = null,
    val roomType: String? = null,
    val bathroom: String? = null,
    val buildingType: String? = null,
    val allowedTenants: String? = null,
    val services: List<String> = emptyList(),
    val maxDistanceKm: Double = 10.0,
    val sort: String = "distance",
    val page: Int = 1,
    val pageSize: Int = 20,
)
