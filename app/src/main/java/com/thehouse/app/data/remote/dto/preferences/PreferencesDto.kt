package com.thehouse.app.data.remote.dto.preferences

import com.thehouse.app.domain.model.Budget
import com.thehouse.app.domain.model.Preference
import kotlinx.serialization.Serializable

/**
 * DTO de GET/PUT /api/v1/users/me/preferences. Solo mapeo, sin lógica de negocio.
 *
 * Los tipos son espejo del contrato productivo: `pets: bool?` y `Budget.min/max: int?`.
 * La traducción de códigos ↔ etiquetas humanas vive en [PreferencesContract].
 *
 * `@Serializable` es obligatorio: sin él Retrofit/kotlinx.serialization lanza
 * SerializationException en cada respuesta y la UI cae en el error genérico.
 */
@Serializable
data class PreferencesDto(
    val budget: BudgetDto?,
    val maxDistanceKm: Double?,
    val essentialServices: List<String>?,
    val livingWith: String?,
    val placeType: String?,
    val pets: Boolean?,
    val transport: String?,
    val propertyTypes: List<String>?,
    val currency: String?,
)

@Serializable
data class BudgetDto(
    val min: Int?,
    val max: Int?,
    val currency: String?,
)

fun PreferencesDto.toDomain(): Preference = toDomainDisplay()

fun BudgetDto.toDomain(): Budget = Budget(
    min = min?.toDouble(),
    max = max?.toDouble(),
    currency = currency,
)
