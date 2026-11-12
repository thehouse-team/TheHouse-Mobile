package com.thehouse.app.domain.error

/**
 * Códigos de negocio confirmados del contrato de solicitudes.
 * Nunca se muestran al usuario: Presentation los traduce a texto amigable.
 */
enum class RequestErrorCode {
    ACTIVE_REQUEST_EXISTS,
    ACTIVE_REQUESTS_LIMIT,
    PARTICIPANT_SUSPENDED,
    OWN_ROOM,
    REQUEST_STATE,
    ROOM_UNAVAILABLE,
    LANDLORD_REQUIRED,
    UNKNOWN;

    companion object {
        fun fromApi(value: String?): RequestErrorCode = when (value) {
            "active_request_exists" -> ACTIVE_REQUEST_EXISTS
            "active_requests_limit" -> ACTIVE_REQUESTS_LIMIT
            "participant_suspended" -> PARTICIPANT_SUSPENDED
            "own_room" -> OWN_ROOM
            "request_state" -> REQUEST_STATE
            "room_unavailable" -> ROOM_UNAVAILABLE
            "landlord_required" -> LANDLORD_REQUIRED
            else -> UNKNOWN
        }
    }
}

class RequestException(
    val domainError: DomainError,
    val code: RequestErrorCode = RequestErrorCode.UNKNOWN,
    val retryAfterSeconds: Long? = null,
) : Exception()
