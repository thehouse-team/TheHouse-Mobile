package com.thehouse.app.domain.model

/**
 * Preferencias de búsqueda del usuario (GET/PUT /api/v1/users/me/preferences).
 * Campos espejo del contrato; sin enums inventados (valores como strings).
 */
data class Preference(
    val budget: Budget?,
    val maxDistanceKm: Double?,
    val essentialServices: List<String>?,
    val livingWith: String?,
    val placeType: String?,
    val pets: String?,
    val transport: String?,
    val propertyTypes: List<String>?,
    val currency: String?
)

data class Budget(
    val min: Double?,
    val max: Double?,
    val currency: String?
)
