package com.thehouse.app.domain.error

enum class AgreementErrorCode {
    REQUEST_STATE,
    ROOM_UNAVAILABLE,
    AGREEMENT_OPEN,
    AGREEMENT_STATE,
    VERSION_CONFLICT,
    LANDLORD_REQUIRED,
    CODE_INVALID,
    CODE_EXPIRED,
    CODE_USED,
    TOO_MANY_ATTEMPTS,
    NO_ACTIVE_RENTAL,
    UNKNOWN;

    companion object {
        fun fromApi(value: String?): AgreementErrorCode = when (value) {
            "request_state" -> REQUEST_STATE
            "room_unavailable" -> ROOM_UNAVAILABLE
            "agreement_open" -> AGREEMENT_OPEN
            "agreement_state" -> AGREEMENT_STATE
            "version_conflict" -> VERSION_CONFLICT
            "landlord_required" -> LANDLORD_REQUIRED
            "code_invalid" -> CODE_INVALID
            "code_expired" -> CODE_EXPIRED
            "code_used" -> CODE_USED
            "too_many_attempts" -> TOO_MANY_ATTEMPTS
            "no_active_rental" -> NO_ACTIVE_RENTAL
            else -> UNKNOWN
        }
    }
}

class AgreementException(
    val domainError: DomainError,
    val code: AgreementErrorCode = AgreementErrorCode.UNKNOWN,
    val retryAfterSeconds: Long? = null,
) : Exception()
