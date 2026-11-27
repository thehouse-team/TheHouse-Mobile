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
    PHOTOS_LIMIT,
    STORAGE_LIMIT,
    UNSUPPORTED_MEDIA,
    IMAGE_TOO_LARGE,
    PAYLOAD_TOO_LARGE,
    /** 422 content_rejected: moderación del Backend rechazó el contenido. */
    CONTENT_REJECTED,
    /** 503 moderation_unavailable: servicio temporalmente no disponible. */
    MODERATION_UNAVAILABLE,
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
            "photos_limit" -> PHOTOS_LIMIT
            "storage_limit" -> STORAGE_LIMIT
            "unsupported_media" -> UNSUPPORTED_MEDIA
            "image_too_large" -> IMAGE_TOO_LARGE
            "content_rejected" -> CONTENT_REJECTED
            "moderation_unavailable" -> MODERATION_UNAVAILABLE
            else -> UNKNOWN
        }
    }
}

class LandlordException(
    val domainError: DomainError,
    val code: LandlordErrorCode = LandlordErrorCode.UNKNOWN,
    val retryAfterSeconds: Long? = null,
) : Exception()
