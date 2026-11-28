package com.thehouse.app.domain.location

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CoordinateParserTest {

    @Test
    fun `valid latitude and longitude from user report`() {
        assertEquals(19.9185, CoordinateParser.latitudeOrNull("19.9185")!!, 0.0)
        assertEquals(-99.3645, CoordinateParser.longitudeOrNull("-99.3645")!!, 0.0)
    }

    @Test
    fun `decimal comma is normalized`() {
        assertEquals(19.9185, CoordinateParser.latitudeOrNull("19,9185")!!, 0.0)
        assertEquals(-99.3645, CoordinateParser.longitudeOrNull("-99,3645")!!, 0.0)
    }

    @Test
    fun `unicode minus sign is normalized`() {
        assertEquals(-99.3645, CoordinateParser.longitudeOrNull("\u221299.3645")!!, 0.0)
    }

    @Test
    fun `en dash used as leading sign is normalized`() {
        assertEquals(-99.3645, CoordinateParser.longitudeOrNull("\u201399.3645")!!, 0.0)
    }

    @Test
    fun `outer whitespace and invisible paste characters are normalized`() {
        assertEquals(19.9185, CoordinateParser.latitudeOrNull("  \u200B19.9185\uFEFF  ")!!, 0.0)
        assertEquals(-99.3645, CoordinateParser.longitudeOrNull("\u200C-99.3645\u2060")!!, 0.0)
    }

    @Test
    fun `extended decimals are preserved without rounding`() {
        val raw = "19.918512345678901"
        assertEquals(raw.toDouble(), CoordinateParser.latitudeOrNull(raw)!!, 0.0)
    }

    @Test
    fun `inverted coordinates are rejected by contractual ranges`() {
        assertNull(CoordinateParser.latitudeOrNull("-99.3645"))
        assertNull(CoordinateParser.longitudeOrNull("19.9185"))
    }

    @Test
    fun `positive longitude outside Mexico is rejected`() {
        assertNull(CoordinateParser.longitudeOrNull("99.3645"))
    }

    @Test
    fun `nan infinity and malformed numbers are rejected`() {
        assertNull(CoordinateParser.latitudeOrNull("NaN"))
        assertNull(CoordinateParser.latitudeOrNull("Infinity"))
        assertNull(CoordinateParser.latitudeOrNull("-Infinity"))
        assertNull(CoordinateParser.longitudeOrNull("1e2"))
        assertNull(CoordinateParser.longitudeOrNull("--99.3645"))
        assertNull(CoordinateParser.longitudeOrNull("99.3645.12"))
        assertNull(CoordinateParser.latitudeOrNull("19 9185"))
    }

    @Test
    fun `contractual boundaries are inclusive`() {
        assertEquals(14.0, CoordinateParser.latitudeOrNull("14")!!, 0.0)
        assertEquals(33.0, CoordinateParser.latitudeOrNull("33")!!, 0.0)
        assertEquals(-118.5, CoordinateParser.longitudeOrNull("-118.5")!!, 0.0)
        assertEquals(-86.0, CoordinateParser.longitudeOrNull("-86")!!, 0.0)
        assertNull(CoordinateParser.latitudeOrNull("13.9999"))
        assertNull(CoordinateParser.latitudeOrNull("33.0001"))
        assertNull(CoordinateParser.longitudeOrNull("-118.5001"))
        assertNull(CoordinateParser.longitudeOrNull("-85.9999"))
    }

    @Test
    fun `partial input is allowed while editing`() {
        assertTrue(CoordinateParser.parse("") is CoordinateParser.Result.Partial)
        assertTrue(CoordinateParser.parse("-") is CoordinateParser.Result.Partial)
        assertTrue(CoordinateParser.parse("19,") is CoordinateParser.Result.Partial)
        assertTrue(CoordinateParser.parse("+") is CoordinateParser.Result.Partial)
        assertFalse(CoordinateParser.isValidLatitude(""))
        assertFalse(CoordinateParser.isValidLongitude("-"))
    }

    @Test
    fun `an en dash inside digits is ambiguous and rejected`() {
        assertTrue(CoordinateParser.parse("19\u20139185") is CoordinateParser.Result.Invalid)
    }
}
