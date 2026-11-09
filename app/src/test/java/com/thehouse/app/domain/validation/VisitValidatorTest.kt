package com.thehouse.app.domain.validation

import com.thehouse.app.domain.error.VisitErrorCode
import com.thehouse.app.domain.usecase.VisitValidator
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VisitValidatorTest {

    private val now: Instant = Instant.parse("2026-11-20T12:00:00Z")

    private fun at(value: String): OffsetDateTime = OffsetDateTime.parse(value)

    @Test
    fun `valid schedule within rules`() {
        val result = VisitValidator.validateSchedule(at("2026-11-20T15:00:00-06:00"), now)
        assertTrue(result.isValid)
    }

    @Test
    fun `less than two hours is too soon`() {
        val result = VisitValidator.validateSchedule(at("2026-11-20T07:00:00-06:00"), now)
        assertFalse(result.isValid)
        assertEquals(VisitErrorCode.TOO_SOON, result.errorCode)
    }

    @Test
    fun `more than sixty days is too far`() {
        val result = VisitValidator.validateSchedule(at("2027-01-25T09:00:00-06:00"), now)
        assertFalse(result.isValid)
        assertEquals(VisitErrorCode.TOO_FAR, result.errorCode)
    }

    @Test
    fun `outside opening hours is rejected`() {
        val result = VisitValidator.validateSchedule(at("2026-11-20T21:00:00-06:00"), now)
        assertFalse(result.isValid)
        assertEquals(VisitErrorCode.OUTSIDE_HOURS, result.errorCode)
    }

    @Test
    fun `not quarter hour is rejected`() {
        val result = VisitValidator.validateSchedule(at("2026-11-20T15:07:00-06:00"), now)
        assertFalse(result.isValid)
        assertEquals(VisitErrorCode.NOT_QUARTER_HOUR, result.errorCode)
    }

    @Test
    fun `api format keeps offset`() {
        val zoned = ZonedDateTime.of(2026, 11, 20, 11, 0, 0, 0, ZoneId.of("America/Mexico_City"))
        assertTrue(VisitValidator.formatForApi(zoned).endsWith("-06:00"))
    }

    @Test
    fun `message over 300 is invalid`() {
        assertFalse(VisitValidator.validateMessage("x".repeat(301)).isValid)
        assertTrue(VisitValidator.validateMessage("x".repeat(300)).isValid)
    }
}