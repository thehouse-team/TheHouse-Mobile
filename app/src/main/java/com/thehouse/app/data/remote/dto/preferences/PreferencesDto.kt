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
    val budget: BudgetDto? = null,
    val maxDistanceKm: Double? = null,
    val essentialServices: List<String>? = null,
    val livingWith: String? = null,
    val placeType: String? = null,
    val pets: Boolean? = null,
    val transport: String? = null,
    val propertyTypes: List<String>? = null,
)

@Serializable
data class BudgetDto(
    val min: Int? = null,
    val max: Int? = null,
    val currency: String? = null,
)

fun PreferencesDto.toDomain(): Preference = toDomainDisplay()

fun BudgetDto.toDomain(): Budget = Budget(
    min = min?.toDouble(),
    max = max?.toDouble(),
    currency = currency,
)
