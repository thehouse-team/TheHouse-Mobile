package com.thehouse.app.presentation.common

/**
 * Error seguro para Presentation.
 *
 * No contiene excepciones crudas, códigos HTTP, DTO, Room ni detalles de infraestructura.
 */
sealed interface UiError {
    data object Network : UiError
    data object Timeout : UiError
    data object BadRequest : UiError
    data object Unauthorized : UiError
    data object NotFound : UiError
    data object Conflict : UiError
    data object RateLimited : UiError
    data object Server : UiError
    data object Unknown : UiError
}
