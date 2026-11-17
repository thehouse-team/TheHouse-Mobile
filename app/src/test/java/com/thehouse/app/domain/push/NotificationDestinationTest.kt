package com.thehouse.app.domain.push

import com.thehouse.app.data.notifications.push.PushNavigationIntentParser
import com.thehouse.app.data.notifications.push.PushNavigationIntentResult
import com.thehouse.app.domain.model.NotificationTarget
import com.thehouse.app.domain.model.NotificationTargetKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationDestinationTest {

    private fun destination(kind: String, id: String = "target-1") =
        NotificationDestinationResolver.fromTarget(
            NotificationTargetResolver.resolve(kind, id),
        )

    @Test
    fun `request target maps to RequestDetail`() {
        assertEquals(
            NotificationDestination.Request("target-1"),
            destination("request"),
        )
    }

    @Test
    fun `conversation target maps to Conversation`() {
        assertEquals(
            NotificationDestination.Conversation("target-1"),
            destination("conversation"),
        )
    }

    @Test
    fun `visit target maps to VisitDetail`() {
        assertEquals(
            NotificationDestination.Visit("target-1"),
            destination("visit"),
        )
    }

    @Test
    fun `agreement target maps to rental flow without inventing detail route`() {
        assertEquals(NotificationDestination.Agreement, destination("agreement"))
    }

    @Test
    fun `rental target maps to rental flow`() {
        assertEquals(NotificationDestination.Rental, destination("rental"))
    }

    @Test
    fun `building target maps to BuildingDetail`() {
        assertEquals(
            NotificationDestination.Building("target-1"),
            destination("building"),
        )
    }

    @Test
    fun `unknown kind maps to Notification Center`() {
        assertEquals(NotificationDestination.NotificationCenter, destination("payment"))
    }

    @Test
    fun `missing or unsafe id maps to Notification Center`() {
        assertEquals(NotificationDestination.NotificationCenter, destination("request", ""))
        assertEquals(NotificationDestination.NotificationCenter, destination("request", "../etc/passwd"))
    }

    @Test
    fun `null target maps to Notification Center`() {
        assertEquals(NotificationDestination.NotificationCenter, NotificationDestinationResolver.fromTarget(null))
    }

    @Test
    fun `target resolver accepts real backend style ids and rejects unknown kinds`() {
        val target = NotificationTargetResolver.resolve("conversation", "0f8fad5b-d9cb-469f-a165-70867728950e")
        assertEquals(NotificationTargetKind.CONVERSATION, target?.kind)
        assertNull(NotificationTargetResolver.resolve("unknown-kind", "id-1"))
        assertTrue(NotificationTargetResolver.isSafeId("0f8fad5b-d9cb-469f-a165-70867728950e"))
    }
}

class PushNavigationIntentParserTest {

    @Test
    fun `launcher intent without extras is ignored`() {
        assertEquals(PushNavigationIntentResult.Ignore, PushNavigationIntentParser.parse(null, null))
        assertEquals(PushNavigationIntentResult.Ignore, PushNavigationIntentParser.parse("", ""))
    }

    @Test
    fun `known target navigates with validated destination`() {
        val result = PushNavigationIntentParser.parse("visit", "visit-9")
        assertEquals(
            PushNavigationIntentResult.Navigate(NotificationDestination.Visit("visit-9")),
            result,
        )
    }

    @Test
    fun `unknown kind falls back safely`() {
        assertEquals(
            PushNavigationIntentResult.Fallback,
            PushNavigationIntentParser.parse("external-intent", "https://evil.example"),
        )
    }

    @Test
    fun `missing kind but present id falls back`() {
        assertEquals(
            PushNavigationIntentResult.Fallback,
            PushNavigationIntentParser.parse(null, "request-1"),
        )
    }

    @Test
    fun `invalid id falls back safely`() {
        assertEquals(
            PushNavigationIntentResult.Fallback,
            PushNavigationIntentParser.parse("request", "bad id with spaces"),
        )
    }

    @Test
    fun `destination resolver never returns a raw URL kind`() {
        val destination = NotificationDestinationResolver.fromTarget(
            NotificationTarget(kind = NotificationTargetKind.UNKNOWN, id = "https://evil.example"),
        )
        assertEquals(NotificationDestination.NotificationCenter, destination)
    }
}
