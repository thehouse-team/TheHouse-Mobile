package com.thehouse.app.presentation.common

import com.thehouse.app.domain.error.DomainError

/**
 * Transforma errores de Domain a un error seguro para Presentation.
 *
 * La UI nunca recibe excepciones crudas ni detalles de infraestructura.
 */
fun DomainError.toUiError(): UiError = when (this) {
    DomainError.Network -> UiError.Network
    DomainError.Timeout -> UiError.Timeout
    DomainError.Unauthorized -> UiError.Unauthorized
    DomainError.AccountSuspended -> UiError.AccountSuspended
    DomainError.NotFound -> UiError.NotFound
    DomainError.Conflict -> UiError.Conflict
    DomainError.Gone -> UiError.Gone
    DomainError.RateLimited -> UiError.RateLimited
    DomainError.Server -> UiError.Server
    DomainError.Unknown -> UiError.Unknown
}
