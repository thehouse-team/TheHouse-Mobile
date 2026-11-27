package com.thehouse.app.domain.push

import com.thehouse.app.domain.model.NotificationTarget
import com.thehouse.app.domain.model.NotificationTargetKind

/**
 * Destino real de navegación de una notificación.
 *
 * `Agreement` y `Rental` no tienen ruta de detalle propia en el cliente
 * estudiante: ambos usan "Mi renta" (decisión Fase A, sin rutas inventadas).
 * NV-20 v1.3.0 agrega `Room` (GET /rooms/{id}) y mantiene el bandeja como
 * fallback seguro.
 */
sealed interface NotificationDestination {
    data class Request(val id: String) : NotificationDestination
    data class Conversation(val id: String) : NotificationDestination
    data class Visit(val id: String) : NotificationDestination
    data object Agreement : NotificationDestination
    data object Rental : NotificationDestination
    data class Building(val id: String) : NotificationDestination
    /** NV-20 v1.3.0: review_received / review_hidden / favorite_unavailable. */
    data class Room(val id: String) : NotificationDestination

    /** Fallback seguro para target desconocido, inválido o ausente. */
    data object NotificationCenter : NotificationDestination
}

/**
 * Único resolver de destinos, compartido por notificaciones internas (in-app)
 * y por notificaciones push. Evita duplicación de reglas.
 *
 * Reglas NV-20 v1.3.0 §9.1:
 * - `room` + targetId válido → RoomDetailRoute(targetId); inválido → bandeja.
 * - `none` → bandeja, sin exigir targetId.
 * - kind desconocido → bandeja.
 * - type desconocido → bandeja (notificación genérica).
 * - Nunca se interpreta kind/id como ruta arbitraria: solo destinos internos.
 */
object NotificationDestinationResolver {

    private val SAFE_ID = Regex("^[A-Za-z0-9][A-Za-z0-9._:-]{0,127}$")

    fun isValidTargetId(value: String?): Boolean =
        value != null && SAFE_ID.matches(value.trim())

    fun fromTarget(target: NotificationTarget?): NotificationDestination =
        from(target?.kind?.api, target?.id)

    fun from(kindValue: String?, targetId: String?): NotificationDestination {
        val kind = NotificationTargetKind.fromApi(kindValue)
        return when (kind) {
            NotificationTargetKind.ROOM -> when {
                isValidTargetId(targetId) -> NotificationDestination.Room(targetId!!.trim())
                else -> NotificationDestination.NotificationCenter
            }
            NotificationTargetKind.NONE,
            NotificationTargetKind.UNKNOWN -> NotificationDestination.NotificationCenter
            NotificationTargetKind.REQUEST,
            NotificationTargetKind.CONVERSATION,
            NotificationTargetKind.VISIT,
            NotificationTargetKind.BUILDING -> when {
                isValidTargetId(targetId) -> when (kind) {
                    NotificationTargetKind.REQUEST -> NotificationDestination.Request(targetId!!.trim())
                    NotificationTargetKind.CONVERSATION -> NotificationDestination.Conversation(targetId!!.trim())
                    NotificationTargetKind.VISIT -> NotificationDestination.Visit(targetId!!.trim())
                    else -> NotificationDestination.Building(targetId!!.trim())
                }
                else -> NotificationDestination.NotificationCenter
            }
            NotificationTargetKind.AGREEMENT -> NotificationDestination.Agreement
            NotificationTargetKind.RENTAL -> NotificationDestination.Rental
        }
    }
}
