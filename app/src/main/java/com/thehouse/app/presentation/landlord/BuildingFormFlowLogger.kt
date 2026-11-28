package com.thehouse.app.presentation.landlord

import android.util.Log
import com.thehouse.app.BuildConfig

/**
 * Logging DEBUG dirigido del formulario Nuevo edificio.
 *
 * Solo emite nombres de campos y contadores. NUNCA registra nombre, dirección,
 * coordenadas exactas, tokens ni datos personales. Deshabilitado en release.
 */
object BuildingFormFlowLogger {
    private const val TAG = "BuildingFormFlow"

    private fun log(event: String, fields: String) {
        if (!BuildConfig.DEBUG) return
        // Los tests JVM no tienen android.util.Log implementado: el logger es
        // puramente diagnóstico y nunca altera el comportamiento.
        runCatching { Log.d(TAG, if (fields.isBlank()) event else "$event $fields") }
    }

    /** Ejemplo: `validation_failed fields=postalCode,university`. */
    fun validationFailed(fieldNames: List<String>) {
        log("validation_failed", "fields=${fieldNames.joinToString(",")}")
    }

    fun universityPreselected(source: String) {
        log("university_preselected", "source=$source")
    }

    fun universitySelectionPreserved() {
        log("university_selection_preserved", "")
    }
}
