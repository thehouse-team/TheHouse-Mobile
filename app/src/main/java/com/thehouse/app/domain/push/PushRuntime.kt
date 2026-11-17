package com.thehouse.app.domain.push

/**
 * Verifica si existe una sesión válida antes de mostrar o navegar por un push.
 *
 * Un push recibido sin sesión no debe exponer contenido privado de la sesión
 * anterior; se descarta de forma segura.
 */
interface PushSessionGate {
    suspend fun hasActiveSession(): Boolean
}

/** Visibilidad de la app (foreground/background) sin depender de Android en Domain. */
interface AppVisibility {
    fun isForeground(): Boolean
}

/**
 * Presentación local de una notificación push.
 *
 * Fase B mantiene Fase A: en foreground NO se muestra notificación de sistema
 * para no duplicar el Notification Center; solo se emite una señal de refresco.
 */
interface LocalPushNotifier {
    fun show(payload: PushPayload)
    fun cancelAll()
}

/** Logger de eventos de push. Nunca recibe tokens, payloads ni datos privados. */
interface PushLogger {
    fun info(event: String)
    fun warn(event: String, reason: String? = null)
}
