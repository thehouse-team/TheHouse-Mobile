package com.thehouse.app.data.catalog.mapper

import com.thehouse.app.data.remote.dto.catalog.PropertyDetailDto
import com.thehouse.app.data.remote.dto.catalog.PropertyListItemDto
import com.thehouse.app.data.remote.dto.catalog.UniversityDto
import com.thehouse.app.domain.model.Property
import com.thehouse.app.domain.model.University

/**
 * Mapeo DTO ↔ Domain (catálogo). Solo transformación, sin red ni UI.
 */
object CatalogMappers {

    fun UniversityDto.toDomain(): University = University(
        id = id,
        code = code,
        name = name,
        city = city,
        country = country,
        createdAt = createdAt,
    )

    fun PropertyListItemDto.toDomain(): Property = Property(
        id = id,
        title = title,
        price = price,
        currency = currency,
        city = city,
        universityId = universityId,
        bedrooms = bedrooms,
        bathrooms = bathrooms,
        areaM2 = areaM2,
        createdAt = createdAt,
    )

    fun PropertyDetailDto.toDomain(): Property = Property(
        id = id,
        title = title,
        price = price,
        currency = currency,
        city = city,
        universityId = universityId,
        bedrooms = bedrooms,
        bathrooms = bathrooms,
        areaM2 = areaM2,
        createdAt = createdAt,
        description = description,
        amenities = amenities,
    )
}
