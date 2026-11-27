package com.thehouse.app.domain.room

/**
 * Catálogos cerrados CONFIRMADOS en el Backend (`ListingRules`, `SearchService`).
 * Ojo: el enunciado de Fase B intercambiaba roomType/buildingType; aquí se usa
 * el contrato real. `placeType` de Preferencias NO se reutiliza como roomType.
 */
object RoomSearchCatalog {
    val ROOM_TYPES = listOf("single", "double", "shared")
    val BUILDING_TYPES = listOf("rooms_building", "house_rooms", "full_apartment")
    val BATHROOMS = listOf("private", "shared")
    val ALLOWED_TENANTS = listOf("women", "men", "any")
    val SORTS = listOf("distance", "price", "newest")

    const val MIN_PRICE = 500
    const val MAX_PRICE = 50000
    const val MIN_DISTANCE_KM = 0.5
    const val MAX_DISTANCE_KM = 30.0
    const val DEFAULT_DISTANCE_KM = 10.0
    const val MAX_PAGE_SIZE = 50
    const val DEFAULT_PAGE_SIZE = 20

    private val roomTypeLabels = mapOf(
        "single" to "Individual",
        "double" to "Doble",
        "shared" to "Compartido",
    )
    private val buildingTypeLabels = mapOf(
        "rooms_building" to "Cuartos en edificio",
        "house_rooms" to "Cuartos en casa",
        "full_apartment" to "Departamento completo",
    )
    private val bathroomLabels = mapOf("private" to "Privado", "shared" to "Compartido")
    private val tenantLabels = mapOf("women" to "Mujeres", "men" to "Hombres", "any" to "Cualquiera")
    private val sortLabels = mapOf(
        "distance" to "Distancia",
        "price" to "Precio",
        "newest" to "Más recientes",
    )

    fun label(kind: Kind, code: String): String = when (kind) {
        Kind.ROOM_TYPE -> roomTypeLabels[code] ?: code
        Kind.BUILDING_TYPE -> buildingTypeLabels[code] ?: code
        Kind.BATHROOM -> bathroomLabels[code] ?: code
        Kind.ALLOWED_TENANTS -> tenantLabels[code] ?: code
        Kind.SORT -> sortLabels[code] ?: code
    }

    enum class Kind { ROOM_TYPE, BUILDING_TYPE, BATHROOM, ALLOWED_TENANTS, SORT }
}
