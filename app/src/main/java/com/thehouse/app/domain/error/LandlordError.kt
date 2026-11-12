package com.thehouse.app.domain.error

/**
 * Códigos de negocio confirmados del contrato NV-12 (Backend).
 * Nunca se muestran al usuario: Presentation los traduce a texto amigable.
 */
enum class LandlordErrorCode {
    LANDLORD_REQUIRED,
    LANDLORD_EXISTS,
    BUILDINGS_LIMIT,
    ROOMS_LIMIT,
    ROOM_NUMBER_TAKEN,
    BUILDING_IN_REVIEW,
    BUILDING_STATE,
    BUILDING_HAS_RENTED_ROOMS,
    ROOM_STATE,
    ROOM_RENTED,
    PUBLICATION_INCOMPLETE,
    VERSION_CONFLICT,
    VALIDATION,
    UNKNOWN;

    companion object {
        fun fromApi(value: String?): LandlordErrorCode = when (value) {
            "landlord_required" -> LANDLORD_REQUIRED
            "landlord_exists" -> LANDLORD_EXISTS
            "buildings_limit" -> BUILDINGS_LIMIT
            "rooms_limit" -> ROOMS_LIMIT
            "room_number_taken" -> ROOM_NUMBER_TAKEN
            "building_in_review" -> BUILDING_IN_REVIEW
            "building_state" -> BUILDING_STATE
            "building_has_rented_rooms" -> BUILDING_HAS_RENTED_ROOMS
            "room_state" -> ROOM_STATE
            "room_rented" -> ROOM_RENTED
            "publication_incomplete" -> PUBLICATION_INCOMPLETE
            "version_conflict" -> VERSION_CONFLICT
            else -> UNKNOWN
        }
    }
}

class LandlordException(
    val domainError: DomainError,
    val code: LandlordErrorCode = LandlordErrorCode.UNKNOWN,
    val retryAfterSeconds: Long? = null,
) : Exception()
