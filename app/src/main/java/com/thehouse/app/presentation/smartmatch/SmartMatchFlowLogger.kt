package com.thehouse.app.presentation.smartmatch

import android.util.Log
import com.thehouse.app.BuildConfig

/**
 * Logging DEBUG del flujo Smart Match (runbook "no HTTP request").
 *
 * Registra SOLO tokens de control y tipos de excepción:
 * - sin tokens/JWT, correo, nombre ni identifiers de usuario;
 * - sin preferencias completas, bodies, paths sensibles ni stacktraces;
 * - deshabilitado en release (BuildConfig.DEBUG).
 *
 * Inyectable para tests JVM: [sink] permite verificar los eventos sin
 * depender de `android.util.Log`.
 */
object SmartMatchFlowLogger {

    const val TAG = "SmartMatchFlow"

    @Volatile
    var sink: (String) -> Unit = { message -> defaultLog(message) }

    /** En JVM (tests) no hay `android.util.Log` mockeado: no-op seguro. */
    private fun defaultLog(message: String) {
        runCatching { Log.d(TAG, message) }
    }

    @JvmStatic
    fun emit(event: String) {
        if (!BuildConfig.DEBUG) return
        sink(event)
    }

    /** Nombre simple de la excepción y operación, sin mensaje ni causa. */
    fun exception(operation: String, throwable: Throwable) {
        emit("exception=${throwable.javaClass.simpleName} operation=$operation")
    }

    /** Estado de UI devuelto por el ViewModel (tipo sin payload). */
    fun state(state: SmartMatchUiState) {
        val name = when (state) {
            SmartMatchUiState.Loading -> "Loading"
            SmartMatchUiState.RequiresAuth -> "RequiresAuth"
            SmartMatchUiState.NoPreferences -> "NoPreferences"
            SmartMatchUiState.Empty -> "Empty"
            is SmartMatchUiState.Error -> "Error"
            is SmartMatchUiState.Content -> "Content"
        }
        emit("state=$name")
    }

    /** Resultado de preferencias sin valores: presencia y completitud. */
    fun preferences(result: String) {
        emit("preferences_result=$result")
    }

    /** Resultado de candidatos sin identificadores ni PII. */
    fun candidates(count: Int) {
        emit("candidates_result count=$count")
    }

    /** Resultado de la query AUTH (NV-23), solo el número de items. */
    fun authQueryResult(count: Int) {
        emit("auth_query_result count=$count")
    }

    /** Inicio del fallback ML local; motivo sin PII: network o 503. */
    fun fallbackStart(reason: String) {
        emit("fallback_start reason=$reason")
    }

    /** Entrada a la pantalla y creación del ViewModel (sin parámetros). */
    fun screenEnter() = emit("enter_screen")

    fun viewModelCreated() = emit("viewmodel_created")
}

/** Helper invocado desde el Composable sin lógica de negocio. */
internal object SmartMatchScreenExitLogger {
    fun logEnter() {
        SmartMatchFlowLogger.screenEnter()
        SmartMatchFlowLogger.viewModelCreated()
    }
}
