package com.thehouse.app.domain.multimedia

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaValidationTest {

    @Test
    fun `valid jpeg image passes`() {
        val result = MediaValidation.validateImage(
            mimeType = "image/jpeg",
            sizeBytes = 1024L,
        )
        assertTrue(result is MediaValidationResult.Valid)
        val valid = result as MediaValidationResult.Valid
        assertEquals("image/jpeg", valid.normalizedMimeType)
        assertEquals(1024L, valid.sizeBytes)
    }

    @Test
    fun `mime normalization and allowed types`() {
        val png = MediaValidation.validateImage(" Image/PNG ", 10L)
        assertTrue(png is MediaValidationResult.Valid)
        val bad = MediaValidation.validateImage("image/gif", 10L)
        assertTrue(bad is MediaValidationResult.Invalid)
        assertEquals(
            MediaValidationError.UnsupportedMimeType("image/gif"),
            (bad as MediaValidationResult.Invalid).error,
        )
    }

    @Test
    fun `missing unknown or empty size fails safely`() {
        val missing = MediaValidation.validateImage(null, 10L)
        assertEquals(MediaValidationError.MissingMimeType, (missing as MediaValidationResult.Invalid).error)

        val unknown = MediaValidation.validateImage("image/jpeg", null)
        assertEquals(MediaValidationError.UnknownSize, (unknown as MediaValidationResult.Invalid).error)

        val empty = MediaValidation.validateImage("image/jpeg", 0L)
        assertEquals(MediaValidationError.EmptyFile, (empty as MediaValidationResult.Invalid).error)
    }

    @Test
    fun `oversized image is rejected`() {
        val result = MediaValidation.validateImage("image/webp", 11L, maxBytes = 10L)
        assertTrue(result is MediaValidationResult.Invalid)
        assertEquals(
            MediaValidationError.FileTooLarge(actualBytes = 11L, maxBytes = 10L),
            (result as MediaValidationResult.Invalid).error,
        )
    }

    @Test
    fun `text validation rejects empty whitespace too long and control chars`() {
        val empty = MediaValidation.validateTextInput("   ")
        assertEquals(TextValidationError.EmptyOrWhitespace, (empty as TextValidationResult.Invalid).error)

        val long = MediaValidation.validateTextInput("abcd", maxLength = 3)
        assertEquals(TextValidationError.TooLong(4, 3), (long as TextValidationResult.Invalid).error)

        val control = MediaValidation.validateTextInput("bad\u0007text")
        assertEquals(
            TextValidationError.InvalidControlCharacters,
            (control as TextValidationResult.Invalid).error,
        )

        val ok = MediaValidation.validateTextInput("hola\nmundo")
        assertTrue(ok is TextValidationResult.Valid)
    }

    @Test
    fun `extension hint is secondary and normalized`() {
        assertEquals("jpg", MediaValidation.sanitizeExtensionHint("photo.JPG"))
        assertEquals(null, MediaValidation.sanitizeExtensionHint("noext"))
        assertEquals(null, MediaValidation.sanitizeExtensionHint(null))
    }
}
