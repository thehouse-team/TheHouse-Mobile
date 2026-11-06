package com.thehouse.app.data.remote.dto.catalog

import kotlinx.serialization.Serializable

/**
 * DTOs del contrato real NV-27 (D5, AllowAnonymous):
 * GET /api/v1/universities, /universities/{id},
 * GET /api/v1/properties, /properties/{id}.
 *
 * Nombres JSON camelCase del backend ASP.NET. Sin campos inventados.
 * La paginación es page/pageSize (defaults 1/20, max 50).
 */
@Serializable
data class UniversityDto(
    val id: String,
    val code: String,
    val name: String,
    val city: String,
    val country: String,
    val createdAt: String,
)

@Serializable
data class UniversityListResponseDto(
    val items: List<UniversityDto> = emptyList(),
    val page: Int = 1,
    val pageSize: Int = 20,
    val totalItems: Long = 0,
    val totalPages: Int = 0,
)

@Serializable
data class PropertyListItemDto(
    val id: String,
    val title: String,
    val price: Double,
    val currency: String,
    val city: String,
    val universityId: String? = null,
    val bedrooms: Int,
    val bathrooms: Int,
    val areaM2: Double,
    val createdAt: String,
)

@Serializable
data class PropertyListResponseDto(
    val items: List<PropertyListItemDto> = emptyList(),
    val page: Int = 1,
    val pageSize: Int = 20,
    val totalItems: Long = 0,
    val totalPages: Int = 0,
)

@Serializable
data class PropertyDetailDto(
    val id: String,
    val title: String,
    val description: String? = null,
    val price: Double,
    val currency: String,
    val city: String,
    val universityId: String? = null,
    val bedrooms: Int,
    val bathrooms: Int,
    val areaM2: Double,
    val amenities: List<String> = emptyList(),
    val createdAt: String,
)
