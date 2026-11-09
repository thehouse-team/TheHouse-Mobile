package com.thehouse.app.domain.usecase

import com.thehouse.app.domain.error.VisitErrorCode
import java.time.Instant
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

data class VisitValidationResult(
    val isValid: Boolean,
    val errorCode: VisitErrorCode? = null,
)

object VisitValidator {
    const val MAX_MESSAGE_LENGTH = 300
    const val MAX_CANCEL_REASON_LENGTH = 200
    val TIME_ZONE: ZoneId = ZoneId.of("America/Mexico_City")
    val OPENING_TIME: LocalTime = LocalTime.of(8, 0)
    val CLOSING_TIME: LocalTime = LocalTime.of(20, 0)

    fun validateSchedule(
        scheduledAt: OffsetDateTime,
        now: Instant = Instant.now(),
    ): VisitValidationResult {
        val instant = scheduledAt.toInstant()
        val local = instant.atZone(TIME_ZONE)

        if (instant.isBefore(now.plusSeconds(2 * 60 * 60))) {
            return VisitValidationResult(false, VisitErrorCode.TOO_SOON)
        }
        if (instant.isAfter(now.plusSeconds(60L * 24 * 60 * 60))) {
            return VisitValidationResult(false, VisitErrorCode.TOO_FAR)
        }
        val time = local.toLocalTime()
        if (time.isBefore(OPENING_TIME) || time.isAfter(CLOSING_TIME)) {
            return VisitValidationResult(false, VisitErrorCode.OUTSIDE_HOURS)
        }
        if (local.minute % 15 != 0 || local.second != 0 || local.nano != 0) {
            return VisitValidationResult(false, VisitErrorCode.NOT_QUARTER_HOUR)
        }
        return VisitValidationResult(true)
    }

    fun validateMessage(message: String?): VisitValidationResult =
        if ((message?.length ?: 0) <= MAX_MESSAGE_LENGTH) {
            VisitValidationResult(true)
        } else {
            VisitValidationResult(false, VisitErrorCode.UNKNOWN)
        }

    fun validateCancelReason(reason: String?): VisitValidationResult =
        if ((reason?.length ?: 0) <= MAX_CANCEL_REASON_LENGTH) {
            VisitValidationResult(true)
        } else {
            VisitValidationResult(false, VisitErrorCode.UNKNOWN)
        }

    fun formatForApi(scheduledAt: ZonedDateTime): String =
        scheduledAt.withZoneSameInstant(TIME_ZONE).toOffsetDateTime().toString()

    fun toMexicoCity(instant: Instant): ZonedDateTime = instant.atZone(TIME_ZONE)
}
