package com.thehouse.app.presentation.landlord

import com.thehouse.app.domain.error.AgreementErrorCode
import com.thehouse.app.domain.error.AgreementException
import com.thehouse.app.domain.error.DomainError

object AgreementErrorMessages {

    fun map(exception: AgreementException?): String = map(exception?.code, exception?.domainError)

    fun map(code: AgreementErrorCode?, domainError: DomainError?): String = when (code) {
        AgreementErrorCode.CODE_INVALID -> "El código no es válido."
        AgreementErrorCode.CODE_EXPIRED -> "El código expiró. Solicita uno nuevo al arrendador."
        AgreementErrorCode.CODE_USED -> "Este código ya fue utilizado."
        AgreementErrorCode.TOO_MANY_ATTEMPTS -> "Realizaste demasiados intentos. Inténtalo más tarde."
        AgreementErrorCode.NO_ACTIVE_RENTAL -> "No tienes una renta activa."
        AgreementErrorCode.REQUEST_STATE -> "La solicitud ya no puede realizar esta acción."
        AgreementErrorCode.ROOM_UNAVAILABLE -> "El cuarto ya no está disponible."
        AgreementErrorCode.AGREEMENT_OPEN -> "El cuarto ya tiene un convenio pendiente o activo."
        AgreementErrorCode.AGREEMENT_STATE -> "El convenio no se puede cambiar en su estado actual."
        AgreementErrorCode.VERSION_CONFLICT -> "Los datos cambiaron desde la última lectura; vuelve a cargarlos."
        AgreementErrorCode.LANDLORD_REQUIRED -> "Necesitas activar el modo arrendador para realizar esta acción."
        else -> when (domainError) {
            DomainError.AccountSuspended -> "Tu cuenta está suspendida."
            DomainError.Unauthorized -> "Tu sesión expiró. Inicia sesión de nuevo."
            DomainError.NotFound -> "No se encontró lo solicitado."
            DomainError.Timeout -> "La conexión tardó demasiado. Intenta de nuevo."
            DomainError.Network -> "No hay conexión. Verifica tu red e intenta de nuevo."
            DomainError.RateLimited -> "Demasiadas solicitudes. Espera un momento e intenta de nuevo."
            DomainError.Server -> "El servicio no está disponible ahora. Intenta más tarde."
            DomainError.Conflict -> "No se pudo completar la acción. Intenta de nuevo."
            DomainError.Gone -> "El recurso ya no está disponible."
            else -> "Revisa los datos ingresados."
        }
    }

    fun retryHint(retryAfterSeconds: Long?): String? {
        if (retryAfterSeconds == null || retryAfterSeconds <= 0) return null
        val minutes = (retryAfterSeconds + 59) / 60
        return if (minutes <= 1) "Espera aproximadamente 1 minuto."
        else "Espera aproximadamente $minutes minutos."
    }
}
