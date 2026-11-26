package com.thehouse.app.perf

import android.util.Log
import java.util.Locale

/**
 * Instrumentación DEBUG de rendimiento (hardening de runtime).
 *
 * Emite líneas con formato exacto:
 *
 * ```
 * PERF <operation> durationMs=<n>
 * ```
 *
 * Reglas:
 * - Sin PII: solo nombre de operación y duración; nunca payloads, URLs,
 *   headers, identificadores de usuario ni valores de estado.
 * - Deshabilitado por defecto ([enabled] = false) para que ningún test JVM
 *   llegue a `android.util.Log` (no mockeado). Se activa en runtime desde
 *   `TheHouseApplication` cuando el build es DEBUG.
 * - Costo fuera de DEBUG: una lectura de [enabled] antes de medir.
 *
 * Objetivo: separar en logs NETWORK / MAPPING / STATE / RENDERING sin asumir
 * de antemano dónde está la latencia.
 */
object Perf {

    const val TAG = "Perf"

    @Volatile
    var enabled: Boolean = false

    /** Inyectable para tests. Solo se invoca con [enabled] = true. */
    @Volatile
    var logger: (String) -> Unit = { message -> Log.d(TAG, message) }

    /** Mide una operación síncrona y emite `PERF <operation> durationMs=<n>`. */
    fun <T> trace(operation: String, block: () -> T): T {
        if (!enabled) return block()
        val startNanos = System.nanoTime()
        try {
            return block()
        } finally {
            emit(operation, startNanos)
        }
    }

    /** Mide una operación suspendida (red, caché, mapping con I/O). */
    suspend fun <T> traceSuspend(operation: String, block: suspend () -> T): T {
        if (!enabled) return block()
        val startNanos = System.nanoTime()
        try {
            return block()
        } finally {
            emit(operation, startNanos)
        }
    }

    /**
     * Emite la duración transcurrida desde [startNanos] (tomado con
     * `System.nanoTime()` al iniciar la operación). Para operaciones cuyo
     * inicio no puede envolverse, p. ej. la carga de una imagen en Compose.
     */
    fun since(operation: String, startNanos: Long) {
        if (!enabled) return
        emit(operation, startNanos)
    }

    private fun emit(operation: String, startNanos: Long) {
        if (!enabled) return
        val durationMs = (System.nanoTime() - startNanos) / 1_000_000
        logger(String.format(Locale.US, "PERF %s durationMs=%d", operation, durationMs))
    }
}
