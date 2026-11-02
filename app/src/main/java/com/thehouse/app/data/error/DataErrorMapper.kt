package com.thehouse.app.data.error

import com.thehouse.app.domain.error.DomainError

/**
 * Transforma errores técnicos de Data a errores semánticos de Domain.
 *
 * La dirección de dependencia es Data → Domain.
 */
fun DataError.toDomainError(): DomainError = when (this) {
    DataError.Network -> DomainError.Network
    DataError.Timeout -> DomainError.Timeout
    DataError.Serialization -> DomainError.Unknown
    DataError.Local -> DomainError.Unknown
    is DataError.Unknown -> DomainError.Unknown
    is DataError.Http -> when (statusCode) {
        401, 403 -> DomainError.Unauthorized
        404 -> DomainError.NotFound
        408 -> DomainError.Timeout
        409 -> DomainError.Conflict
        429 -> DomainError.RateLimited
        in 500..599 -> DomainError.Server
        else -> DomainError.Unknown
    }
}
