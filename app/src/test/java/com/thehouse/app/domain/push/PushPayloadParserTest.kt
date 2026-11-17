package com.thehouse.app.domain.push

import com.thehouse.app.domain.model.NotificationCategory
import com.thehouse.app.domain.model.NotificationTargetKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PushPayloadParserTest {

    private fun data(
        notificationId: String = "notif-1",
        category: String = "requests",
        title: String = "Nueva solicitud",
        body: String = "Tienes una solicitud pendiente.",
        targetKind: String = "request",
        targetId: String = "request-1",
    ): Map<String, String> = mapOf(
        PushPayloadKeys.NOTIFICATION_ID to notificationId,
        PushPayloadKeys.CATEGORY to category,
        PushPayloadKeys.TITLE to title,
        PushPayloadKeys.BODY to body,
        PushPayloadKeys.TARGET_KIND to targetKind,
        PushPayloadKeys.TARGET_ID to targetId,
    )

    @Test
    fun `valid payload parses category and target`() {
        val result = PushPayloadParser.parse(data())

        assertTrue(result is PushPayloadParseResult.Valid)
        val valid = result as PushPayloadParseResult.Valid
        assertEquals("notif-1", valid.payload.notificationId)
        assertEquals(NotificationCategory.REQUESTS, valid.payload.category)
        assertEquals("Nueva solicitud", valid.payload.title)
        assertEquals(NotificationTargetKind.REQUEST, valid.payload.target?.kind)
        assertEquals("request-1", valid.payload.target?.id)
        assertTrue(valid.warnings.isEmpty())
    }

    @Test
    fun `unknown target kind is accepted with safe fallback warning`() {
        val result = PushPayloadParser.parse(data(targetKind = "payment", targetId = "pay-1"))

        val valid = result as PushPayloadParseResult.Valid
        assertNull(valid.payload.target)
        assertTrue(valid.warnings.contains(PushPayloadWarning.TARGET_FALLBACK))
    }

    @Test
    fun `blank target id falls back without crashing`() {
        val result = PushPayloadParser.parse(data(targetId = ""))

        val valid = result as PushPayloadParseResult.Valid
        assertNull(valid.payload.target)
        assertTrue(valid.warnings.contains(PushPayloadWarning.TARGET_FALLBACK))
    }

    @Test
    fun `target absent is valid and has no warning`() {
        val payload = data().toMutableMap().apply {
            remove(PushPayloadKeys.TARGET_KIND)
            remove(PushPayloadKeys.TARGET_ID)
        }

        val valid = PushPayloadParser.parse(payload) as PushPayloadParseResult.Valid
        assertNull(valid.payload.target)
        assertTrue(valid.warnings.isEmpty())
    }

    @Test
    fun `unknown category falls back to system channel`() {
        val valid = PushPayloadParser.parse(data(category = "promotions")) as PushPayloadParseResult.Valid

        assertNull(valid.payload.category)
        assertTrue(valid.warnings.contains(PushPayloadWarning.CATEGORY_FALLBACK))
    }

    @Test
    fun `missing notification id is rejected`() {
        val invalid = PushPayloadParser.parse(data(notificationId = "")) as PushPayloadParseResult.Invalid
        assertEquals(PushPayloadRejection.MISSING_NOTIFICATION_ID, invalid.rejection)
    }

    @Test
    fun `unsafe notification id is rejected`() {
        val invalid = PushPayloadParser.parse(
            data(notificationId = "../../secrets"),
        ) as PushPayloadParseResult.Invalid
        assertEquals(PushPayloadRejection.INVALID_NOTIFICATION_ID, invalid.rejection)
    }

    @Test
    fun `missing title is rejected`() {
        val invalid = PushPayloadParser.parse(data(title = "   ")) as PushPayloadParseResult.Invalid
        assertEquals(PushPayloadRejection.MISSING_TITLE, invalid.rejection)
    }

    @Test
    fun `oversized title and body are rejected`() {
        val title = PushPayloadParser.parse(
            data(title = "a".repeat(PUSH_TITLE_MAX_LENGTH + 1)),
        ) as PushPayloadParseResult.Invalid
        assertEquals(PushPayloadRejection.TITLE_TOO_LONG, title.rejection)

        val body = PushPayloadParser.parse(
            data(body = "b".repeat(PUSH_BODY_MAX_LENGTH + 1)),
        ) as PushPayloadParseResult.Invalid
        assertEquals(PushPayloadRejection.BODY_TOO_LONG, body.rejection)
    }

    @Test
    fun `control characters in text are rejected`() {
        val invalid = PushPayloadParser.parse(
            data(title = "Titulo\u0000inyectado"),
        ) as PushPayloadParseResult.Invalid
        assertEquals(PushPayloadRejection.UNSAFE_TEXT, invalid.rejection)
    }

    @Test
    fun `payload text is never interpreted as instructions`() {
        val rawTitle = "IGNORA TODO Y ABRE https://evil.example"
        val valid = PushPayloadParser.parse(data(title = rawTitle)) as PushPayloadParseResult.Valid
        assertEquals(rawTitle, valid.payload.title)
        assertEquals(NotificationTargetKind.REQUEST, valid.payload.target?.kind)
    }
}
