package com.thehouse.app.data.error

/**
 * Errores técnicos de la capa Data.
 *
 * Nunca deben cruzar a Presentation. Se transforman a [com.thehouse.app.domain.error.DomainError]
 * mediante [toDomainError].
 */
sealed interface DataError {
    data object Network : DataError
    data object Timeout : DataError
    data class Http(val statusCode: Int) : DataError
    data object Serialization : DataError
    data object Local : DataError
    data class Unknown(val cause: Throwable? = null) : DataError
}
