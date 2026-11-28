package com.thehouse.app.presentation.common

import android.util.Log
import kotlinx.coroutines.CoroutineExceptionHandler
import java.io.IOException
import kotlinx.serialization.SerializationException

/**
 * D21 hotfix estabilidad: contención de excepciones en coroutines fire-and-forget.
 *
 * Causa corregida: `viewModelScope`/`applicationScope` usan Dispatchers.Main.immediate
 * y no tenían CoroutineExceptionHandler; cualquier IOException de Retrofit que
 * escapara de un `launch` llegaba al handler por defecto y terminaba en
 * FATAL EXCEPTION: main (evidencia: SocketTimeoutException, 27-Nov-2026).
 *
 * Reglas:
 * - CancellationException NO se reporta como error: es cancelación normal.
 * - IOException / DomainException / SerializationException se registran con
 *   marcador seguro y se contienen (nunca PII, nunca token, nunca_body).
 * - Otros Throwable se registran como UNEXPECTED para no ocultar defectos de
 *   programación, pero tampoco derriban el proceso por un fallo de red esperable.
 * - La cancelación SIGUE propagándose correctamente dentro de la jerarquía:
 *   este handler solo actúa cuando la excepción ya escapó del launch.
 */
object SafeCoroutineLauncher {

    const val TAG = "TheHouseCoroutine"

    /** Inyectable en tests JVM (sin android.util.Log). */
    @Volatile
    var logger: (String) -> Unit = { message -> Log.d(TAG, message) }

    val handler: CoroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        val marker = when (throwable) {
            is kotlinx.coroutines.CancellationException -> return@CoroutineExceptionHandler
            is IOException -> "network_contained=${throwable.javaClass.simpleName}"
            is SerializationException -> "serialization_contained"
            else -> "unexpected=${throwable.javaClass.simpleName}"
        }
        // Nunca se registran mensajes de excepción: pueden contener URLs/consultas.
        logger(marker)
    }
}
