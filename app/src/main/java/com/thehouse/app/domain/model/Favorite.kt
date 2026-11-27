package com.thehouse.app.domain.model

/**
 * Favorito real. `roomId` es la identidad principal; `propertyId` se conserva
 * nullable para compatibilidad con respuestas legacy (nunca se confunde).
 */
data class Favorite(
    val roomId: String,
    val title: String,
    val price: Int? = null,
    val currency: String? = null,
    val roomType: String? = null,
    val bathroom: String? = null,
    val city: String? = null,
    val buildingId: String? = null,
    val favoritedAt: String? = null,
    val propertyId: String? = null,
    val demo: Boolean = false,
)
