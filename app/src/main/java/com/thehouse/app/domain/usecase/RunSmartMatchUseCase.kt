package com.thehouse.app.domain.usecase

import com.thehouse.app.domain.model.Preference
import com.thehouse.app.domain.model.Property
import com.thehouse.app.domain.smartmatch.SmartMatchFeatureExtractor
import com.thehouse.app.domain.smartmatch.SmartMatchFactor
import com.thehouse.app.domain.smartmatch.SmartMatchModel
import com.thehouse.app.domain.smartmatch.SmartMatchModelVersions
import com.thehouse.app.domain.smartmatch.SmartMatchResult
import com.thehouse.app.domain.smartmatch.SmartMatchSource
import kotlin.math.roundToInt

/**
 * Ejecuta inferencia del artefacto LOCAL sobre candidatos reales.
 *
 * Se usa SOLO como fallback etiquetado (offline/red/timeout/503) en D16.
 * El score es decimal (sin redondeo a Int) y el origen se marca
 * [SmartMatchSource.LOCAL_ESTIMATE] para que la UI lo distinga del Backend.
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
            val matchScore = (probability * 100.0).roundToInt().coerceIn(0, 100)
            SmartMatchResult(
                roomId = property.id,
                property = property,
                probability = probability,
                matchScore = matchScore,
                // El artefacto local NO provee breakdown logreg; solo etiquetas
                // informativas. maxPoints=0 indica a la UI que no dibuje barra
                // (nunca inventar puntos/maxPoints del modelo productivo).
                factors = SmartMatchFeatureExtractor.factors(preference, property)
                    .map { label -> SmartMatchFactor(factor = label, label = label, points = 0.0, maxPoints = 0.0) },
                source = SmartMatchSource.LOCAL_ESTIMATE,
                modelVersion = SmartMatchModelVersions.LOCAL_MODEL_VERSION,
            )
        }.sortedWith(compareByDescending<SmartMatchResult> { it.matchScore }.thenBy { it.property.id })
    }
}
