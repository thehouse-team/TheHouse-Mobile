package com.thehouse.app.domain.smartmatch

import com.thehouse.app.domain.model.Property

/** Vector de características normalizado para inferencia (solo datos reales/no sensibles). */
data class FeatureVector(
    val values: DoubleArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FeatureVector) return false
        return values.contentEquals(other.values)
    }

    override fun hashCode(): Int = values.contentHashCode()
}

/** Origen del score: Backend (autoridad online) o estimación local etiquetada. */
enum class SmartMatchSource {
    BACKEND,
    LOCAL_ESTIMATE,
}

/** Factor real del desglose del Backend. NO convertir a List<String>. */
data class SmartMatchFactor(
    val factor: String,
    val label: String,
    val points: Double,
    val maxPoints: Double,
)

/**
 * Resultado de Smart Match para un candidato real.
 *
 * `matchScore` es decimal (D16): el Backend lo reporta con un decimal
 * (ej. 73.5). NUNCA se trunca ni se convierte silenciosamente a Int.
 */
data class SmartMatchResult(
    val property: Property,
    val probability: Double,
    val matchScore: Double,
    val factors: List<SmartMatchFactor>,
    val source: SmartMatchSource = SmartMatchSource.BACKEND,
    val modelVersion: String = SmartMatchModelVersions.SERVER_MODEL_VERSION,
)

/** Versiones: el artefacto local NO se declara equivalente al modelo productivo. */
object SmartMatchModelVersions {
    const val LOCAL_MODEL_VERSION = "1.0.0"
    const val SERVER_MODEL_VERSION = "logreg-1.0.0"
    const val LOCAL_EQUIVALENCE = "UNKNOWN"
}

object SmartMatchFeatures {
    const val BUDGET_FIT = "budget_fit"
    const val SERVICES_OVERLAP = "services_overlap"
    const val PRICE_AFFINITY = "price_affinity"
    const val CURRENCY_MATCH = "currency_match"

    val ORDERED: List<String> = listOf(
        BUDGET_FIT,
        SERVICES_OVERLAP,
        PRICE_AFFINITY,
        CURRENCY_MATCH,
    )
}
