package com.thehouse.app.preferences.flow

import android.util.Log
import com.thehouse.app.BuildConfig

/**
 * Instrumentación DEBUG del flujo de Preferencias (D14).
 *
 * Emite lines con formato exacto:
 * ```
 * PreferencesFlow enter_screen
 * PreferencesFlow load_start
 * PreferencesFlow request_start
 * PreferencesFlow request_result=<success|not_found|unauthorized|error>
 * PreferencesFlow state=<Loading|Content|Empty|RequiresAuth|Error>
 * PreferencesFlow exception=<SimpleName> operation=<operation>
 * ```
 *
 * Reglas (M-MOB-040 / M-MOB-056):
 * - Sin PII, tokens, bodies, paths con identificadores ni stacktraces.
 * - Solo el nombre simple de la excepción y la operación instrumentada.
 * - Deshabilitado por defecto para que los tests JVM no lean `android.util.Log`.
 * - `TheHouseHttp` ya registra método, ruta relativa, status y duración.
 */
object PreferencesFlowLogger {

    const val TAG = "PreferencesFlow"

    /** Activo en DEBUG (sin PII); los tests pueden forzarlo o silenciarlo. */
    @Volatile
    var enabled: Boolean = BuildConfig.DEBUG

    @Volatile
    var logger: (String) -> Unit = { message ->
        runCatching { Log.d(TAG, message) }
    }

    fun enterScreen() = emit("enter_screen")
    fun loadStart() = emit("load_start")
    fun requestStart() = emit("request_start")

    fun requestSuccess() = requestResult("success")
    fun requestNotFound() = requestResult("not_found")
    fun requestUnauthorized() = requestResult("unauthorized")
    fun requestError() = requestResult("error")

    fun state(state: String) = emit("state=$state")

    fun exception(throwable: Throwable, operation: String) {
        emit("exception=${throwable::class.java.simpleName} operation=$operation")
    }

    private fun requestResult(result: String) = emit("request_result=$result")

    private fun emit(message: String) {
        if (enabled) logger(message)
    }
}
