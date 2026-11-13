package com.thehouse.app.presentation.requests

import com.thehouse.app.domain.error.ChatErrorCode
import com.thehouse.app.domain.error.DomainError
import com.thehouse.app.domain.error.RequestErrorCode

/**
 * Mensajes amigables del contrato D9. Nunca se muestran códigos técnicos.
 */
object RequestErrorMessages {

    fun map(code: RequestErrorCode?, domainError: DomainError? = null): String = when (code) {
        RequestErrorCode.ACTIVE_REQUEST_EXISTS ->
            "Ya tienes una solicitud activa para este cuarto."
        RequestErrorCode.ACTIVE_REQUESTS_LIMIT ->
            "Llegaste al límite de solicitudes activas."
        RequestErrorCode.PARTICIPANT_SUSPENDED ->
            "No es posible realizar esta acción porque una de las cuentas está suspendida."
        RequestErrorCode.OWN_ROOM ->
            "No puedes enviar una solicitud a tu propio cuarto."
        RequestErrorCode.REQUEST_STATE ->
            "La solicitud ya no puede realizar esta acción."
        RequestErrorCode.ROOM_UNAVAILABLE ->
            "El cuarto ya no está disponible."
        RequestErrorCode.LANDLORD_REQUIRED ->
            "Necesitas activar el modo arrendador."
        RequestErrorCode.UNKNOWN -> fallback(domainError)
        null -> fallback(domainError)
    }

    fun mapChat(code: ChatErrorCode?, domainError: DomainError? = null): String = when (code) {
        ChatErrorCode.CONVERSATION_CLOSED -> "Esta conversación está cerrada."
        ChatErrorCode.UNKNOWN -> fallback(domainError)
        null -> fallback(domainError)
    }

    private fun fallback(domainError: DomainError?): String = when (domainError) {
        DomainError.Network -> "No hay conexión. Verifica tu red e intenta de nuevo."
        DomainError.Timeout -> "La conexión tardó demasiado. Intenta de nuevo."
        DomainError.RateLimited -> "Demasiadas solicitudes. Espera un momento e intenta de nuevo."
        DomainError.Server -> "El servicio no está disponible ahora. Intenta más tarde."
        DomainError.Unauthorized -> "Tu sesión expiró. Inicia sesión de nuevo."
        DomainError.AccountSuspended -> "Tu cuenta está suspendida."
        DomainError.NotFound -> "No se encontró lo solicitado."
        else -> "Ocurrió un error inesperado. Intenta de nuevo."
    }

    fun statusLabel(status: com.thehouse.app.domain.model.RequestStatus): String = when (status) {
        com.thehouse.app.domain.model.RequestStatus.PENDING -> "Pendiente"
        com.thehouse.app.domain.model.RequestStatus.ACCEPTED -> "Aceptada"
        com.thehouse.app.domain.model.RequestStatus.REJECTED -> "Rechazada"
        com.thehouse.app.domain.model.RequestStatus.CANCELLED -> "Cancelada"
    }
}
