package com.thehouse.app.data.remote.dto.preferences

import com.thehouse.app.domain.model.Budget
import com.thehouse.app.domain.model.Preference

/**
 * DTO de GET/PUT /api/v1/users/me/preferences. Solo mapeo, sin lógica de negocio.
 */
data class PreferencesDto(
    val budget: BudgetDto?,
    val maxDistanceKm: Double?,
    val essentialServices: List<String>?,
    val livingWith: String?,
    val placeType: String?,
    val pets: String?,
    val transport: String?,
    val propertyTypes: List<String>?,
    val currency: String?
)

data class BudgetDto(
    val min: Double?,
    val max: Double?,
    val currency: String?
)

fun PreferencesDto.toDomain(): Preference = Preference(
    budget = budget?.toDomain(),
    maxDistanceKm = maxDistanceKm,
    essentialServices = essentialServices,
    livingWith = livingWith,
    placeType = placeType,
    pets = pets,
    transport = transport,
    propertyTypes = propertyTypes,
    currency = currency,
)

fun BudgetDto.toDomain(): Budget = Budget(
    min = min,
    max = max,
    currency = currency,
)
