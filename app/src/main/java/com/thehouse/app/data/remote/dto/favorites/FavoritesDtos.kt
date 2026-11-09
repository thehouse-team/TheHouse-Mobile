package com.thehouse.app.data.remote.dto.favorites

import kotlinx.serialization.Serializable

@Serializable
data class FavoriteItemDto(
    val propertyId: String,
    val title: String,
    val price: Double? = null,
    val currency: String? = null,
    val city: String? = null,
    val universityId: String? = null,
    val bedrooms: Int? = null,
    val bathrooms: Int? = null,
    val areaM2: Double? = null,
    val favoritedAt: String? = null,
)

@Serializable
data class FavoritesListResponseDto(
    val items: List<FavoriteItemDto> = emptyList(),
    val page: Int = 1,
    val pageSize: Int = 20,
    val totalItems: Int = 0,
    val totalPages: Int = 0,
)

@Serializable
data class FavoriteIdsResponseDto(
    val propertyIds: List<String> = emptyList(),
)