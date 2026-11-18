package com.thehouse.app.domain.model

import kotlinx.serialization.Serializable

/** Totales generales del dashboard del arrendador. */
@Serializable
data class LandlordStatsTotals(
    val views: Int = 0,
    val favorites: Int = 0,
    val requests: Int = 0,
    val acceptedRequests: Int = 0,
    val visits: Int = 0,
    val confirmedVisits: Int = 0,
    val agreements: Int = 0,
    val conversion: Double = 0.0,
)

/** Comparación "previous" del dashboard del arrendador. */
@Serializable
data class LandlordStatsPrevious(
    val views: Int = 0,
    val favorites: Int = 0,
    val requests: Int = 0,
    val acceptedRequests: Int = 0,
    val visits: Int = 0,
    val confirmedVisits: Int = 0,
    val agreements: Int = 0,
    val conversion: Double = 0.0,
)

/** Ítem de la serie diaria. */
@Serializable
data class LandlordStatsDailyItem(
    val date: String = "",
    val views: Int = 0,
    val requests: Int = 0,
)

/** Response completa de GET /api/v1/landlord/stats. */
@Serializable
data class LandlordStatsResponse(
    val from: String = "",
    val to: String = "",
    val buildingId: String? = null,
    val totals: LandlordStatsTotals = LandlordStatsTotals(),
    val previous: LandlordStatsPrevious = LandlordStatsPrevious(),
    val daily: List<LandlordStatsDailyItem> = emptyList(),
    val favoritesNow: Int = 0,
    val updatedAt: String = "",
)

/** Item de recomendación del arrendador. */
@Serializable
data class LandlordStatsRecommendation(
    val code: String = "",
    val priority: String = "",
    val buildingId: String? = null,
    val roomId: String? = null,
    val title: String = "",
    val detail: String = "",
)

/** Conjunto de recomendaciones del arrendador. */
@Serializable
data class LandlordStatsRecommendations(
    val items: List<LandlordStatsRecommendation> = emptyList(),
)

/** Item de estadísticas de un cuarto del arrendador. */
@Serializable
data class LandlordRoomStats(
    val roomId: String,
    val buildingId: String,
    val buildingName: String,
    val number: String,
    val status: String,
    val price: Int,
    val views: Int,
    val favorites: Int,
    val requests: Int,
    val visits: Int,
    val agreements: Int,
    val conversion: Double,
    val rating: LandlordRating? = null,
)

/** Calificación de un cuarto del arrendador. */
@Serializable
data class LandlordRating(
    val average: Double,
    val count: Int,
)

/** Respuesta paginada de estadísticas de cuartos del arrendador. */
@Serializable
data class PagedLandlordRoomStats(
    val items: List<LandlordRoomStats> = emptyList(),
    val page: Int = 1,
    val pageSize: Int = 50,
    val totalItems: Long = 0,
    val totalPages: Int = 0,
)