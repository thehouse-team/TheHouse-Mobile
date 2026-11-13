package com.thehouse.app.domain.error

/**
 * Errores de dominio independientes de infraestructura.
 *
 * No deben contener tipos de Retrofit, OkHttp, Room, DataStore ni Android.
 */
sealed interface DomainError {
    data object Network : DomainError
    data object Timeout : DomainError
    data object Unauthorized : DomainError
    data object AccountSuspended : DomainError
    data object NotFound : DomainError
    data object Conflict : DomainError
    data object Gone : DomainError
    data object RateLimited : DomainError
    data object Server : DomainError
    data object Unknown : DomainError
}
