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

/**
 * Contrato real verificado de GET /api/v1/universities/search?lat=&lng=
 * (UniversitySearchEndpoint.cs / ProfileContracts.cs, solo lectura):
 * items con id, code, name, city y distanceKm (double?, omitido si null).
 * Máximo 20, ordenado por distancia ascendente y luego nombre.
 * No incluye paginación ni country/createdAt.
 */
@Serializable
data class NearbyUniversityDto(
    val id: String,
    val code: String,
    val name: String,
    val city: String,
    val distanceKm: Double? = null,
)

@Serializable
data class NearbyUniversityResponseDto(
    val items: List<NearbyUniversityDto> = emptyList(),
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
    val thumbnailUrl: String? = null,
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
data class MediaItemDto(
    val id: String,
    val url: String,
    val thumbUrl: String,
    val width: Int? = null,
    val height: Int? = null,
    val bytes: Long? = null,
    val createdAt: String? = null,
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
    val photos: List<MediaItemDto> = emptyList(),
)
