package com.thehouse.app.data.remote.dto.preferences

import com.thehouse.app.data.remote.api.BudgetPatchBody
import com.thehouse.app.data.remote.api.PreferencesPatchBody
import com.thehouse.app.domain.model.Budget
import com.thehouse.app.domain.model.Preference

/**
 * Traducción entre los valores humanos de la UI y los códigos contractuales del Backend
 * (`PreferencesRequest` / `PreferencesResponse` de `GET|PUT /api/v1/users/me/preferences`).
 *
 * El Backend es la única fuente de verdad:
 * - `pets` es `bool?` (la UI muestra "Sí"/"No", nunca envía texto).
 * - Los catálogos son cerrados: se envían códigos, no etiquetas visuales.
 * - `budget.min/max` son `int?` en MXN; nunca se redondea un decimal del usuario.
 *
 * Los códigos aquí son exactamente los confirmados en el contrato productivo; no se inventan.
 */
object PreferencesContract {

    const val CURRENCY = "MXN"
    const val BUDGET_MIN = 0
    const val BUDGET_MAX = 50_000
    const val DISTANCE_MIN = 0.5
    const val DISTANCE_MAX = 30.0

    val ESSENTIAL_SERVICES_CODES = listOf(
        "wifi", "water", "electricity", "gas", "hot_water", "laundry",
        "kitchen", "furnished", "parking", "security", "cleaning",
    )
    val LIVING_WITH_CODES = listOf("women", "men", "any")
    val PLACE_TYPE_CODES = listOf("room", "room_private_bath", "studio", "shared")
    val TRANSPORT_CODES = listOf("walk", "bike", "public", "car")
    val PROPERTY_TYPE_CODES = listOf("house", "apartment", "student_residence")

    /** Etiquetas humanas → códigos. Solo se envía un código si la etiqueta es reconocida. */
    private val ESSENTIAL_SERVICES_LABELS = mapOf(
        "wifi" to "Internet", "water" to "Agua", "electricity" to "Luz", "gas" to "Gas",
        "hot_water" to "Agua caliente", "laundry" to "Lavandería", "kitchen" to "Cocina",
        "furnished" to "Amueblado", "parking" to "Estacionamiento", "security" to "Seguridad",
        "cleaning" to "Limpieza",
    )
    private val LIVING_WITH_LABELS = mapOf(
        "women" to "Mujeres", "men" to "Hombres", "any" to "Cualquiera",
    )
    private val PLACE_TYPE_LABELS = mapOf(
        "room" to "Cuarto", "room_private_bath" to "Cuarto con baño privado",
        "studio" to "Estudio", "shared" to "Cuarto compartido",
    )
    private val TRANSPORT_LABELS = mapOf(
        "walk" to "Caminar", "bike" to "Bicicleta", "public" to "Transporte público", "car" to "Auto",
    )
    private val PROPERTY_TYPE_LABELS = mapOf(
        "house" to "Casa", "apartment" to "Departamento", "student_residence" to "Residencia estudiantil",
    )

    fun codeToLabel(code: String): String = when (code) {
        in ESSENTIAL_SERVICES_CODES -> ESSENTIAL_SERVICES_LABELS[code] ?: code
        in LIVING_WITH_CODES -> LIVING_WITH_LABELS[code] ?: code
        in PLACE_TYPE_CODES -> PLACE_TYPE_LABELS[code] ?: code
        in TRANSPORT_CODES -> TRANSPORT_LABELS[code] ?: code
        in PROPERTY_TYPE_CODES -> PROPERTY_TYPE_LABELS[code] ?: code
        else -> code
    }

    fun labelToCode(label: String, allowed: List<String>, labels: Map<String, String>): String? {
        val normalized = label.trim().lowercase()
        return allowed.firstOrNull { code ->
            code == normalized || labels[code]?.lowercase() == normalized
        }
    }

    fun essentialServicesCode(label: String) =
        labelToCode(label, ESSENTIAL_SERVICES_CODES, ESSENTIAL_SERVICES_LABELS)
    fun livingWithCode(label: String) = labelToCode(label, LIVING_WITH_CODES, LIVING_WITH_LABELS)
    fun placeTypeCode(label: String) = labelToCode(label, PLACE_TYPE_CODES, PLACE_TYPE_LABELS)
    fun transportCode(label: String) = labelToCode(label, TRANSPORT_CODES, TRANSPORT_LABELS)
    fun propertyTypeCode(label: String) = labelToCode(label, PROPERTY_TYPE_CODES, PROPERTY_TYPE_LABELS)

    fun petsToBoolean(label: String): Boolean? = when (label.trim().lowercase()) {
        "sí", "si", "true", "yes" -> true
        "no", "false" -> false
        else -> null
    }

    fun booleanToLabel(value: Boolean?): String = when (value) {
        true -> "Sí"
        false -> "No"
        null -> ""
    }

    /** Convierte el modelo de dominio al body exacto del backend. Los códigos no reconocidos se omiten. */
    fun Preference.toRequest(
        min: Int?,
        max: Int?,
    ): PreferencesPatchBody {
        val currencyValue = budget?.currency?.trim()?.takeIf { it.isNotBlank() } ?: CURRENCY
        val validMin = min?.takeIf { it in BUDGET_MIN..BUDGET_MAX }
        val validMax = max?.takeIf { it in BUDGET_MIN..BUDGET_MAX }
        val validDistance = maxDistanceKm?.takeIf { it in DISTANCE_MIN..DISTANCE_MAX }
        return PreferencesPatchBody(
            budget = if (validMin != null || validMax != null) {
                BudgetPatchBody(min = validMin, max = validMax, currency = currencyValue)
            } else {
                null
            },
            maxDistanceKm = validDistance,
            essentialServices = essentialServices
                ?.mapNotNull(::essentialServicesCode)
                ?.distinct()
                ?.takeIf { it.isNotEmpty() },
            livingWith = livingWith?.let(::livingWithCode),
            placeType = placeType?.let(::placeTypeCode),
            pets = pets?.let(::petsToBoolean),
            transport = transport?.let(::transportCode),
            propertyTypes = propertyTypes
                ?.mapNotNull(::propertyTypeCode)
                ?.distinct()
                ?.takeIf { it.isNotEmpty() },
        )
    }
}

/** Convierte la respuesta del backend a etiquetas humanas para la UI. */
fun PreferencesDto.toDomainDisplay(): Preference = Preference(
    budget = Budget(
        min = budget?.min?.toDouble(),
        max = budget?.max?.toDouble(),
        currency = budget?.currency ?: PreferencesContract.CURRENCY,
    ),
    maxDistanceKm = maxDistanceKm,
    essentialServices = essentialServices?.map(PreferencesContract::codeToLabel),
    livingWith = livingWith?.let(PreferencesContract::codeToLabel),
    placeType = placeType?.let(PreferencesContract::codeToLabel),
    pets = PreferencesContract.booleanToLabel(pets),
    transport = transport?.let(PreferencesContract::codeToLabel),
    propertyTypes = propertyTypes?.map(PreferencesContract::codeToLabel),
    // D19.1: `PreferencesResponse` del Backend NO tiene campo `currency` a nivel
    // raíz; la moneda contractual vive dentro de `budget` (BudgetResponse).
    currency = budget?.currency ?: PreferencesContract.CURRENCY,
)
