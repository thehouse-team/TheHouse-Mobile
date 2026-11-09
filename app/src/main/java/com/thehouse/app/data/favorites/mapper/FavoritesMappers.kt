package com.thehouse.app.data.favorites.mapper

import com.thehouse.app.data.remote.dto.favorites.FavoriteItemDto
import com.thehouse.app.domain.model.Favorite

fun FavoriteItemDto.toDomain(): Favorite = Favorite(
    propertyId = propertyId,
    title = title,
    price = price,
    currency = currency,
    city = city,
    universityId = universityId,
    bedrooms = bedrooms,
    bathrooms = bathrooms,
    areaM2 = areaM2,
    favoritedAt = favoritedAt,
)