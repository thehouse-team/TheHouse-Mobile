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

/** Resultado de inferencia ML para un candidato real. */
data class SmartMatchResult(
    val property: Property,
    val probability: Double,
    val score: Int,
    val factors: List<String>,
)

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
