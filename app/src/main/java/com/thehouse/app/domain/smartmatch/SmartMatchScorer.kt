package com.thehouse.app.domain.smartmatch

import com.thehouse.app.domain.model.Preference
import com.thehouse.app.domain.model.Property
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Extracción de features reales Preference↔Property y factores de compatibilidad.
 * No usa PII, IDs como señal predictiva, ni datos inventados.
 */
object SmartMatchFeatureExtractor {

    fun extract(preference: Preference, property: Property): FeatureVector {
        val budgetFit = budgetFit(preference, property)
        val servicesOverlap = servicesOverlap(preference, property)
        val priceAffinity = priceAffinity(preference, property)
        val currencyMatch = currencyMatch(preference, property)
        return FeatureVector(doubleArrayOf(budgetFit, servicesOverlap, priceAffinity, currencyMatch))
    }

    fun factors(preference: Preference, property: Property): List<String> {
        val out = mutableListOf<String>()
        val minB = preference.budget?.min
        val maxB = preference.budget?.max
        if (minB != null || maxB != null) {
            val price = property.price
            val inRange = (minB == null || price >= minB) && (maxB == null || price <= maxB)
            if (inRange) out += "Dentro de tu presupuesto" else out += "Fuera de tu presupuesto preferido"
        }
        val prefServices = preference.essentialServices.orEmpty().map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()
        if (prefServices.isNotEmpty()) {
            val prop = property.amenities.map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()
            val overlap = prefServices.intersect(prop).size
            if (overlap > 0) out += "Coincide con $overlap servicio(s) esencial(es)"
            else out += "Sin coincidencias claras en servicios esenciales"
        }
        if (currencyMatch(preference, property) >= 1.0) {
            out += "Moneda compatible"
        } else if (preference.budget?.currency != null || preference.currency != null) {
            out += "Moneda distinta a tu preferencia"
        }
        return out
    }

    private fun budgetFit(preference: Preference, property: Property): Double {
        val minB = preference.budget?.min
        val maxB = preference.budget?.max
        val price = property.price
        if (minB == null && maxB == null) return 0.5 // sin señal de presupuesto
        val lower = minB ?: 0.0
        val upper = maxB ?: Double.MAX_VALUE
        if (price in lower..upper) return 1.0
        val dist = when {
            maxB != null && price > maxB -> (price - maxB) / max(maxB, 1.0)
            minB != null && price < minB -> (minB - price) / max(minB, 1.0)
            else -> 1.0
        }
        return max(0.0, 1.0 - min(dist, 1.0))
    }

    private fun servicesOverlap(preference: Preference, property: Property): Double {
        val pref = preference.essentialServices.orEmpty().map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()
        if (pref.isEmpty()) return 0.5 // sin preferencia explícita
        val prop = property.amenities.map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()
        if (prop.isEmpty()) return 0.0
        val inter = pref.intersect(prop).size.toDouble()
        return inter / pref.size.toDouble()
    }

    private fun priceAffinity(preference: Preference, property: Property): Double {
        val maxB = preference.budget?.max
        val minB = preference.budget?.min
        val price = property.price
        return when {
            maxB != null && maxB > 0 -> {
                val ratio = price / maxB
                when {
                    ratio <= 1.0 -> (0.6 + 0.4 * (1.0 - (ratio.coerceAtLeast(0.0)))) // mejor si usa bien el presupuesto sin pasarse
                    else -> max(0.0, 1.0 - (ratio - 1.0))
                }.coerceIn(0.0, 1.0)
            }
            minB != null && minB > 0 -> {
                val diff = abs(price - minB) / minB
                max(0.0, 1.0 - min(diff, 1.0))
            }
            else -> 0.5
        }
    }

    private fun currencyMatch(preference: Preference, property: Property): Double {
        val prefCurrency = preference.budget?.currency ?: preference.currency
        if (prefCurrency.isNullOrBlank()) return 1.0 // sin restricción explícita
        return if (prefCurrency.equals(property.currency, ignoreCase = true)) 1.0 else 0.0
    }
}

/** Contrato de inferencia ML: aplica parámetros aprendidos del artefacto. */
interface SmartMatchModel {
    val modelVersion: String
    val featureVersion: String
    val artifactHash: String
    fun predictProbability(vector: FeatureVector): Double
}
