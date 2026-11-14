package com.thehouse.app.presentation.landlord

import com.thehouse.app.domain.error.DomainError
import com.thehouse.app.domain.error.LandlordErrorCode
import com.thehouse.app.domain.error.LandlordException

/**
 * Mensajes de error seguros (español) para D12.
 * Nunca expone status HTTP, códigos internos ni ProblemDetails crudo.
 */
object LandlordErrorMessages {

    fun map(exception: LandlordException?): String {
        if (exception == null) return "Ocurrió un error inesperado. Intenta de nuevo."
        return when {
            exception.domainError == DomainError.AccountSuspended -> "Tu cuenta está suspendida."
            exception.code == LandlordErrorCode.LANDLORD_REQUIRED ->
                "Necesitas activar el modo arrendador para realizar esta acción."
            exception.code == LandlordErrorCode.VERSION_CONFLICT ->
                "Esta información fue actualizada en otro lugar. Actualiza los datos e inténtalo de nuevo."
            exception.code == LandlordErrorCode.PUBLICATION_INCOMPLETE ->
                "Tu publicación aún no cumple los requisitos. Completa los pasos pendientes."
            exception.code == LandlordErrorCode.BUILDINGS_LIMIT ->
                "Alcanzaste el límite de edificios por arrendador."
            exception.code == LandlordErrorCode.ROOMS_LIMIT ->
                "Alcanzaste el límite de cuartos por edificio."
            exception.code == LandlordErrorCode.ROOM_NUMBER_TAKEN ->
                "Ya existe un cuarto con ese número en el edificio."
            exception.code == LandlordErrorCode.BUILDING_IN_REVIEW ->
                "El edificio está en revisión y no admite cambios de cuartos ahora."
            exception.code == LandlordErrorCode.ROOM_STATE ->
                "El cuarto no está en un estado que permita esta acción."
            exception.code == LandlordErrorCode.ROOM_RENTED ->
                "No se puede modificar un cuarto rentado."
            exception.code == LandlordErrorCode.BUILDING_HAS_RENTED_ROOMS ->
                "El edificio tiene cuartos rentados y no puede eliminarse."
            exception.code == LandlordErrorCode.VALIDATION ->
                "Revisa los datos: hay campos inválidos o incompletos."
            exception.code == LandlordErrorCode.LANDLORD_EXISTS ->
                "Tu cuenta ya tiene el modo arrendador activado."
            else -> mapDomain(exception.domainError)
        }
    }

    fun mapDomain(domainError: DomainError): String = when (domainError) {
        DomainError.Network -> "No hay conexión. Verifica tu red e intenta de nuevo."
        DomainError.Timeout -> "La conexión tardó demasiado. Intenta de nuevo."
        DomainError.Unauthorized -> "Inicia sesión para continuar."
        DomainError.AccountSuspended -> "Tu cuenta está suspendida."
        DomainError.NotFound -> "No se encontró lo solicitado."
        DomainError.Conflict -> "La operación entró en conflicto. Actualiza e intenta de nuevo."
        DomainError.Gone -> "El recurso ya no está disponible."
        DomainError.RateLimited -> "Demasiadas solicitudes. Espera un momento e intenta de nuevo."
        DomainError.Server -> "El servicio no está disponible ahora. Intenta más tarde."
        DomainError.Unknown -> "Ocurrió un error inesperado. Intenta de nuevo."
    }

    fun statusLabel(api: String): String = when (api) {
        "draft" -> "Borrador"
        "in_review" -> "En revisión"
        "verified" -> "Verificado"
        "rejected" -> "Rechazado"
        "suspended" -> "Suspendido"
        "setup" -> "En configuración"
        "ready" -> "Listo"
        "available" -> "Disponible"
        "paused" -> "Pausado"
        "rented" -> "Rentado"
        else -> api
    }
}
