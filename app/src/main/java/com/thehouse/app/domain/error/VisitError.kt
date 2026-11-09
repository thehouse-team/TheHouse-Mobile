package com.thehouse.app.domain.error

/**
 * Códigos de negocio confirmados del contrato de visitas.
 * Nunca se muestran al usuario: Presentation los traduce a texto amigable.
 */
enum class VisitErrorCode {
    IDEMPOTENCY_KEY_REUSED,
    ACTIVE_VISIT_FOR_PROPERTY,
    SCHEDULE_OVERLAP,
    ACTIVE_VISITS_LIMIT,
    VISIT_IN_PROGRESS,
    TOO_SOON,
    TOO_FAR,
    OUTSIDE_HOURS,
    NOT_QUARTER_HOUR,
    VISIT_IN_PAST,
    UNKNOWN;

    companion object {
        fun fromApi(value: String?): VisitErrorCode = when (value) {
            "idempotency_key_reused" -> IDEMPOTENCY_KEY_REUSED
            "active_visit_for_property" -> ACTIVE_VISIT_FOR_PROPERTY
            "schedule_overlap" -> SCHEDULE_OVERLAP
            "active_visits_limit" -> ACTIVE_VISITS_LIMIT
            "visit_in_progress" -> VISIT_IN_PROGRESS
            "too_soon" -> TOO_SOON
            "too_far" -> TOO_FAR
            "outside_hours" -> OUTSIDE_HOURS
            "not_quarter_hour" -> NOT_QUARTER_HOUR
            "visit_in_past" -> VISIT_IN_PAST
            else -> UNKNOWN
        }
    }
}

class VisitException(
    val domainError: DomainError,
    val code: VisitErrorCode = VisitErrorCode.UNKNOWN,
    val retryAfterSeconds: Long? = null,
) : Exception()
