package com.thehouse.app.data.remote.dto.favorites

import kotlinx.serialization.Serializable

/**
 * Espejo del contrato real (`FavoriteContracts.FavoriteItemResponse`).
 * `roomId` es la identidad principal; `propertyId` se conserva SOLO para
 * compatibilidad legacy (visitas/rentas históricas y respuestas antiguas).
 */
@Serializable
data class FavoriteItemDto(
    val roomId: String? = null,
    val propertyId: String? = null,
    val buildingId: String? = null,
    val title: String,
    val price: Int? = null,
    val currency: String? = null,
    val roomType: String? = null,
    val bathroom: String? = null,
    val city: String? = null,
    val approxLocation: ApproxLocationDto? = null,
    val demo: Boolean = false,
    val favoritedAt: String? = null,
)

@Serializable
data class ApproxLocationDto(val lat: Double, val lng: Double)

@Serializable
data class FavoritesListResponseDto(
    val items: List<FavoriteItemDto> = emptyList(),
    val page: Int = 1,
    val pageSize: Int = 20,
    val totalItems: Long = 0,
    val totalPages: Int = 0,
)

/** `/favorites/ids`: roomIds manda; propertyIds es legado. */
@Serializable
data class FavoriteIdsResponseDto(
    val roomIds: List<String> = emptyList(),
    val propertyIds: List<String> = emptyList(),
) {
    /** Identidad de habitación real; legacy solo si no hay roomIds. */
    fun effectiveRoomIds(): Set<String> =
        (roomIds.ifEmpty { propertyIds }).toSet()
}
