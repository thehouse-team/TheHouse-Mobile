package com.thehouse.app.domain.model

/**
 * Modelos de búsqueda de cuartos (NV-07 D10).
 *
 * Reflectan exactamente el contrato público de Backend:
 * GET /api/v1/rooms/search, /rooms/map, /rooms/{id} y /places/search.
 * No incluyen dirección, coordenadas reales, dueño ni teléfono.
 */
data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
)

/** Bbox en orden minLng,minLat,maxLng,maxLat (contrato Backend). */
data class MapBounds(
    val minLongitude: Double,
    val minLatitude: Double,
    val maxLongitude: Double,
    val maxLatitude: Double,
) {
    val longitudeSpan: Double get() = maxLongitude - minLongitude
    val latitudeSpan: Double get() = maxLatitude - minLatitude
    val center: GeoPoint
        get() = GeoPoint(
            latitude = (minLatitude + maxLatitude) / 2.0,
            longitude = (minLongitude + maxLongitude) / 2.0,
        )

    fun isValid(): Boolean =
        minLongitude.isFinite() && minLatitude.isFinite() &&
            maxLongitude.isFinite() && maxLatitude.isFinite() &&
            minLongitude in -180.0..180.0 && maxLongitude in -180.0..180.0 &&
            minLatitude in -90.0..90.0 && maxLatitude in -90.0..90.0 &&
            minLongitude < maxLongitude && minLatitude < maxLatitude &&
            longitudeSpan <= MAX_SPAN_DEGREES && latitudeSpan <= MAX_SPAN_DEGREES

    fun asBbox(): String = "$minLongitude,$minLatitude,$maxLongitude,$maxLatitude"

    companion object {
        /** Lado máximo aceptado por Backend: 0.3°. */
        const val MAX_SPAN_DEGREES = 0.3

        fun around(
            center: GeoPoint,
            latitudeSpan: Double = 0.06,
            longitudeSpan: Double = 0.06,
        ): MapBounds {
            val latSpan = latitudeSpan.coerceIn(0.001, MAX_SPAN_DEGREES)
            val lngSpan = longitudeSpan.coerceIn(0.001, MAX_SPAN_DEGREES)
            return MapBounds(
                minLongitude = (center.longitude - lngSpan / 2.0).coerceIn(-180.0, 180.0),
                minLatitude = (center.latitude - latSpan / 2.0).coerceIn(-90.0, 90.0),
                maxLongitude = (center.longitude + lngSpan / 2.0).coerceIn(-180.0, 180.0),
                maxLatitude = (center.latitude + latSpan / 2.0).coerceIn(-90.0, 90.0),
            )
        }
    }
}

enum class RoomSort(val apiValue: String) {
    DISTANCE("distance"),
    PRICE("price"),
    NEWEST("newest"),
}

/**
 * Filtros soportados realmente por /rooms/search y /rooms/map.
 * No existe búsqueda por texto de cuartos en el contrato; no se simula.
 */
data class RoomSearchFilters(
    val minPrice: Int? = null,
    val maxPrice: Int? = null,
    val roomType: String? = null,
    val bathroom: String? = null,
    val buildingType: String? = null,
    val allowedTenants: String? = null,
    val services: Set<String> = emptySet(),
    val maxDistanceKm: Double = 10.0,
    val sort: RoomSort = RoomSort.DISTANCE,
)

object SearchCatalogs {
    val services: List<String> = listOf(
        "wifi", "water", "electricity", "gas", "hot_water", "laundry",
        "kitchen", "furnished", "parking", "security", "cleaning",
    )
    val roomTypes: List<String> = listOf("single", "double", "shared")
    val bathrooms: List<String> = listOf("private", "shared")
    val buildingTypes: List<String> = listOf("rooms_building", "house_rooms", "full_apartment")
    val allowedTenants: List<String> = listOf("women", "men", "any")

    const val MIN_PRICE: Int = 500
    const val MAX_PRICE: Int = 50_000
    const val MIN_DISTANCE_KM: Double = 0.5
    const val MAX_DISTANCE_KM: Double = 30.0
    const val DEFAULT_DISTANCE_KM: Double = 10.0
}

data class RoomSummary(
    val id: String,
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
    val approxLocation: GeoPoint,
    val thumbnailUrl: String?,
    val demo: Boolean,
)

data class RoomMarker(
    val id: String,
    val price: Int,
    val approxLocation: GeoPoint,
    val distanceKm: Double,
)

data class PlaceSuggestion(
    val id: String,
    val name: String,
    val type: String,
    val approxLocation: GeoPoint,
    val distanceKm: Double,
)

data class RoomUniversity(
    val id: String,
    val code: String,
    val name: String,
)

data class RoomBuilding(
    val id: String,
    val name: String,
    val type: String,
    val allowedTenants: String,
    val services: List<String>,
    val rules: String?,
)

data class RoomDetail(
    val id: String,
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
    val approxLocation: GeoPoint,
    val nearestUniversity: RoomUniversity?,
    val building: RoomBuilding,
    val thumbnailUrl: String?,
    val demo: Boolean,
)
