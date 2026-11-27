package com.thehouse.app.data.favorites.mapper

import com.thehouse.app.data.remote.dto.favorites.FavoriteItemDto
import com.thehouse.app.domain.model.Favorite

/** Identidad principal: roomId. Legacy: propertyId solo si no hay roomId. */
fun FavoriteItemDto.toDomain(): Favorite = Favorite(
    roomId = roomId ?: propertyId.orEmpty(),
    title = title,
    price = price,
    currency = currency,
    roomType = roomType,
    bathroom = bathroom,
    city = city,
    buildingId = buildingId,
    favoritedAt = favoritedAt,
    propertyId = propertyId,
    demo = demo,
)
