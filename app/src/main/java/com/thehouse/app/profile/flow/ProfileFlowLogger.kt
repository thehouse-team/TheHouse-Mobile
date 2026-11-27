package com.thehouse.app.profile.flow

import android.util.Log
import com.thehouse.app.BuildConfig

/**
 * Instrumentación DEBUG del flujo de Perfil (D19.1).
 *
 * Formato:
 * ```
 * ProfileFlow enter_screen
 * ProfileFlow load_start
 * ProfileFlow request_start
 * ProfileFlow deserialization_result=<success|error>
 * ProfileFlow mapping_result=<success|error>
 * ProfileFlow http_status=<code>
 * ProfileFlow state=<Loading|Content|RequiresAuth|Error>
 * ProfileFlow exception=<SimpleName> operation=<operation>
 * ```
 *
 * Según M-MOB-040 / M-MOB-056: sin PII, tokens, contraseñas, bodies ni
 * stacktraces. Solo nombre simple de la excepción, status y operación.
 * Deshabilitado por defecto para tests JVM.
 */
object ProfileFlowLogger {

    const val TAG = "ProfileFlow"

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
    fun httpStatus(code: Int) = emit("http_status=$code")
    fun deserializationResult(result: String) = emit("deserialization_result=$result")
    fun mappingResult(result: String) = emit("mapping_result=$result")
    fun state(state: String) = emit("state=$state")

    fun exception(throwable: Throwable, operation: String) {
        emit("exception=${throwable::class.java.simpleName} operation=$operation")
    }

    private fun emit(message: String) {
        if (enabled) logger(message)
    }
}
