package com.thehouse.app.domain.push

import com.thehouse.app.domain.model.NotificationTarget
import com.thehouse.app.domain.model.NotificationTargetKind

/**
 * Destino real de navegación de una notificación.
 *
 * `Agreement` y `Rental` no tienen ruta de detalle propia en el cliente
 * estudiante: ambos usan "Mi renta" (decisión Fase A, sin rutas inventadas).
 */
sealed interface NotificationDestination {
    data class Request(val id: String) : NotificationDestination
    data class Conversation(val id: String) : NotificationDestination
    data class Visit(val id: String) : NotificationDestination
    data object Agreement : NotificationDestination
    data object Rental : NotificationDestination
    data class Building(val id: String) : NotificationDestination

    /** Fallback seguro para target desconocido, inválido o ausente. */
    data object NotificationCenter : NotificationDestination
}

/**
 * Único mapeo target → destino, compartido por Fase A (in-app) y Fase B (FCM).
 */
object NotificationDestinationResolver {

    fun fromTarget(target: NotificationTarget?): NotificationDestination = when (target?.kind) {
        NotificationTargetKind.REQUEST -> NotificationDestination.Request(target.id)
        NotificationTargetKind.CONVERSATION -> NotificationDestination.Conversation(target.id)
        NotificationTargetKind.VISIT -> NotificationDestination.Visit(target.id)
        NotificationTargetKind.AGREEMENT -> NotificationDestination.Agreement
        NotificationTargetKind.RENTAL -> NotificationDestination.Rental
        NotificationTargetKind.BUILDING -> NotificationDestination.Building(target.id)
        NotificationTargetKind.UNKNOWN, null -> NotificationDestination.NotificationCenter
    }
}
