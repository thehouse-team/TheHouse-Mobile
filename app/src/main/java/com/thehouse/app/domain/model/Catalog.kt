package com.thehouse.app.domain.model

/**
 * Paginación confirmada NV-27 (D5).
 * Backend devuelve page/pageSize/totalItems/totalPages (defaults 1/20, max 50).
 */
data class PagedResult<T>(
    val items: List<T>,
    val page: Int,
    val pageSize: Int,
    val totalItems: Long,
    val totalPages: Int,
)

/**
 * Universidad del contrato real:
 * GET /api/v1/universities y GET /api/v1/universities/{id}.
 */
data class University(
    val id: String,
    val code: String,
    val name: String,
    val city: String,
    val country: String,
    val createdAt: String,
)

/**
 * Propiedad del contrato real (lista y detalle).
 * La lista entrega estos campos; el detalle agrega description/amenities.
 * Sin Match/verificación/distancia (el backend no los entrega).
 */
data class Property(
    val id: String,
    val title: String,
    val price: Double,
    val currency: String,
    val city: String,
    val universityId: String?,
    val bedrooms: Int,
    val bathrooms: Int,
    val areaM2: Double,
    val createdAt: String,
    val description: String? = null,
    val amenities: List<String> = emptyList(),
)
