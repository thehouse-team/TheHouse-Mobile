package com.thehouse.app.domain.validation

import com.thehouse.app.domain.usecase.VisitScheduleOptions
import com.thehouse.app.domain.usecase.VisitValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

class VisitScheduleOptionsTest {

    private val now = Instant.parse("2026-11-27T18:00:00Z") // 12:00 CDMX

    @Test
    fun `date picker millis keep the calendar day without UTC shift`() {
        val picked = LocalDate.of(2026, 11, 30)
        val millis = VisitScheduleOptions.pickerMillisFromDate(picked)
        assertEquals(picked, VisitScheduleOptions.dateFromPickerMillis(millis))
    }

    @Test
    fun `spanish date is human readable and dynamic`() {
        assertEquals(
            "30 de noviembre de 2026",
            VisitScheduleOptions.formatDate(LocalDate.of(2026, 11, 30)),
        )
    }

    @Test
    fun `past date and date beyond 60 days are rejected`() {
        assertFalse(VisitScheduleOptions.isDateAllowed(LocalDate.of(2026, 11, 26), now))
        assertTrue(VisitScheduleOptions.isDateAllowed(LocalDate.of(2026, 11, 27), now))
        assertFalse(VisitScheduleOptions.isDateAllowed(LocalDate.of(2027, 1, 27), now))
    }

    @Test
    fun `nonexistent date cannot be formed from picker millis`() {
        // 2026-11-31 no existe: el DatePicker nunca expone ese día, y un
        // millis inventado se interpreta como el día real siguiente.
        val millis = VisitScheduleOptions.pickerMillisFromDate(LocalDate.of(2026, 12, 1))
        assertEquals(LocalDate.of(2026, 12, 1), VisitScheduleOptions.dateFromPickerMillis(millis))
        // 31 de noviembre no puede construirse: Java lo rechaza, por lo que
        // ningún selector puede producir una fecha inexistente.
        assertTrue(
            runCatching { LocalDate.of(2026, 11, 31) }.isFailure,
        )
    }

    @Test
    fun `slots are quarter hour and include both boundaries`() {
        val slots = VisitScheduleOptions.timeSlots()
        assertEquals(49, slots.size)
        assertEquals(LocalTime.of(8, 0), slots.first())
        assertEquals(LocalTime.of(20, 0), slots.last())
        assertTrue(slots.all { it.minute % 15 == 0 })
    }

    @Test
    fun `today slots exclude times before two hour lead`() {
        val today = VisitScheduleOptions.earliestDate(now) // 27 nov, 12:00 CDMX
        val allowed = VisitScheduleOptions.allowedTimesFor(today, now)
        assertFalse(allowed.contains(LocalTime.of(13, 45)))
        assertTrue(allowed.contains(LocalTime.of(14, 0)))
        assertTrue(allowed.contains(LocalTime.of(20, 0)))
    }

    @Test
    fun `last day slots exclude times beyond 60 days`() {
        val last = VisitScheduleOptions.latestDate(now)
        val allowed = VisitScheduleOptions.allowedTimesFor(last, now)
        // 60 días desde las 12:00 CDMX son las 12:00 del último día.
        assertTrue(allowed.contains(LocalTime.of(12, 0)))
        assertFalse(allowed.contains(LocalTime.of(12, 15)))
    }

    @Test
    fun `changing date invalidates incompatible time`() {
        val late = LocalTime.of(20, 0)
        assertEquals(
            late,
            VisitScheduleOptions.revalidateTime(LocalDate.of(2026, 12, 1), late, now),
        )
        assertEquals(
            null,
            VisitScheduleOptions.revalidateTime(VisitScheduleOptions.latestDate(now), late, now),
        )
    }

    @Test
    fun `scheduledAt keeps Mexico City offset not device zone`() {
        val zoned = java.time.ZonedDateTime.of(
            LocalDate.of(2026, 11, 30),
            LocalTime.of(15, 0),
            VisitValidator.TIME_ZONE,
        )
        val api = VisitValidator.formatForApi(zoned)
        assertTrue(api.startsWith("2026-11-30T15:00"))
        assertTrue(api.endsWith("-06:00"))
    }
}
