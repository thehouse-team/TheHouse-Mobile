package com.thehouse.app.presentation.visits

import com.thehouse.app.domain.error.DomainError
import com.thehouse.app.domain.error.VisitErrorCode

object VisitErrorMessages {

    fun map(code: VisitErrorCode?, domainError: DomainError? = null): String = when (code) {
        VisitErrorCode.ACTIVE_VISIT_FOR_PROPERTY -> "Ya tienes una visita activa para esta propiedad."
        VisitErrorCode.SCHEDULE_OVERLAP -> "Ya tienes otra visita en ese horario."
        VisitErrorCode.ACTIVE_VISITS_LIMIT -> "Llegaste al límite de visitas activas."
        VisitErrorCode.VISIT_IN_PROGRESS -> "Esta visita ya está en curso."
        VisitErrorCode.TOO_SOON -> "Agenda con al menos 2 horas de anticipación."
        VisitErrorCode.TOO_FAR -> "Solo puedes agendar hasta 60 días."
        VisitErrorCode.OUTSIDE_HOURS -> "Horario disponible: 08:00–20:00."
        VisitErrorCode.NOT_QUARTER_HOUR -> "Selecciona intervalos de 15 minutos."
        VisitErrorCode.VISIT_IN_PAST -> "No puedes cancelar una visita pasada."
        VisitErrorCode.IDEMPOTENCY_KEY_REUSED -> "La solicitud cambió. Intenta enviarla de nuevo."
        VisitErrorCode.UNKNOWN -> when (domainError) {
            DomainError.Network -> "No hay conexión. Verifica tu red e intenta de nuevo."
            DomainError.Timeout -> "La conexión tardó demasiado. Intenta de nuevo."
            DomainError.RateLimited -> "Demasiadas solicitudes. Espera un momento e intenta de nuevo."
            DomainError.Server -> "El servicio no está disponible ahora. Intenta más tarde."
            DomainError.Unauthorized -> "Tu sesión expiró. Inicia sesión de nuevo."
            else -> "Ocurrió un error inesperado. Intenta de nuevo."
        }
        null -> "Ocurrió un error inesperado. Intenta de nuevo."
    }
}