package com.thehouse.app.domain.usecase

import com.thehouse.app.domain.model.Preference
import com.thehouse.app.domain.model.Property
import com.thehouse.app.domain.smartmatch.SmartMatchFeatureExtractor
import com.thehouse.app.domain.smartmatch.SmartMatchModel
import com.thehouse.app.domain.smartmatch.SmartMatchResult
import kotlin.math.roundToInt

/**
 * Ejecuta inferencia ML (parámetros aprendidos del artefacto) sobre candidatos reales.
 * Score 0–100 derivado matemáticamente de la probabilidad del modelo.
 */
class RunSmartMatchUseCase(
    private val model: SmartMatchModel,
) {
    fun execute(
        preference: Preference,
        candidates: List<Property>,
    ): List<SmartMatchResult> {
        return candidates.map { property ->
            val vector = SmartMatchFeatureExtractor.extract(preference, property)
            val probability = model.predictProbability(vector).coerceIn(0.0, 1.0)
            val score = (probability * 100.0).roundToInt().coerceIn(0, 100)
            SmartMatchResult(
                property = property,
                probability = probability,
                score = score,
                factors = SmartMatchFeatureExtractor.factors(preference, property),
            )
        }.sortedWith(compareByDescending<SmartMatchResult> { it.probability }.thenBy { it.property.id })
    }
}
