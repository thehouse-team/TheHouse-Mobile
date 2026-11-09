package com.thehouse.app.domain.model

data class Favorite(
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