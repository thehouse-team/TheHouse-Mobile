package com.thehouse.app.data.notifications.push

import com.thehouse.app.domain.push.AppVisibility
import com.thehouse.app.domain.push.LocalPushNotifier
import com.thehouse.app.domain.push.PushLogger
import com.thehouse.app.domain.push.PushPayload
import com.thehouse.app.domain.push.PushPayloadKeys
import com.thehouse.app.domain.push.PushPayloadParser
import com.thehouse.app.domain.push.PushSessionGate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PushMessageHandlerTest {

    private class RecordingLogger : PushLogger {
        val events = mutableListOf<String>()

        override fun info(event: String) {
            events.add(event)
        }

        override fun warn(event: String, reason: String?) {
            events.add(if (reason == null) event else "$event:$reason")
        }
    }

    private class RecordingNotifier(
        private val shouldThrow: Boolean = false,
    ) : LocalPushNotifier {
        val shown = mutableListOf<PushPayload>()
        var cancelled = false

        override fun show(payload: PushPayload) {
            if (shouldThrow) throw IllegalStateException("notify failed")
            shown.add(payload)
        }

        override fun cancelAll() {
            cancelled = true
        }
    }

    private fun payload(
        notificationId: String = "notif-1",
        title: String = "Nueva solicitud",
        body: String = "Tienes una solicitud pendiente.",
        targetKind: String = "request",
        targetId: String = "req-1",
    ): Map<String, String> = mapOf(
        PushPayloadKeys.NOTIFICATION_ID to notificationId,
        PushPayloadKeys.CATEGORY to "requests",
        PushPayloadKeys.TITLE to title,
        PushPayloadKeys.BODY to body,
        PushPayloadKeys.TARGET_KIND to targetKind,
        PushPayloadKeys.TARGET_ID to targetId,
    )

    private fun handler(
        logger: RecordingLogger,
        notifier: RecordingNotifier,
        signals: MutableSharedFlow<Unit>,
        activeSession: Boolean = true,
        foreground: Boolean = true,
    ): PushMessageHandler = PushMessageHandler(
        parser = PushPayloadParser,
        sessionGate = object : PushSessionGate {
            override suspend fun hasActiveSession(): Boolean = activeSession
        },
        visibility = object : AppVisibility {
            override fun isForeground(): Boolean = foreground
        },
        localNotifier = notifier,
        refreshSignals = signals,
        logger = logger,
    )

    @Test
    fun `background push posts one local notification and emits refresh`() = runTest {
        val logger = RecordingLogger()
        val notifier = RecordingNotifier()
        val signals = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        var refreshes = 0
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            signals.collect { refreshes += 1 }
        }

        handler(logger, notifier, signals, foreground = false).handle(payload())

        assertEquals(1, notifier.shown.size)
        assertEquals("notif-1", notifier.shown.first().notificationId)
        assertEquals(1, refreshes)
        assertTrue(logger.events.contains("push_background_notified"))
    }

    @Test
    fun `foreground push does not duplicate in-app notifications`() = runTest {
        val logger = RecordingLogger()
        val notifier = RecordingNotifier()
        val signals = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        var refreshes = 0
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            signals.collect { refreshes += 1 }
        }

        handler(logger, notifier, signals, foreground = true).handle(payload())

        assertTrue(notifier.shown.isEmpty())
        assertEquals(1, refreshes)
        assertTrue(logger.events.contains("push_foreground_refresh"))
    }

    @Test
    fun `invalid payload is rejected without notification`() = runTest {
        val logger = RecordingLogger()
        val notifier = RecordingNotifier()
        val signals = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

        handler(logger, notifier, signals).handle(payload(notificationId = ""))

        assertTrue(notifier.shown.isEmpty())
        assertTrue(logger.events.any { it.startsWith("push_payload_rejected") })
    }

    @Test
    fun `push without valid session is suppressed`() = runTest {
        val logger = RecordingLogger()
        val notifier = RecordingNotifier()
        val signals = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

        handler(logger, notifier, signals, activeSession = false).handle(payload())

        assertTrue(notifier.shown.isEmpty())
        assertTrue(logger.events.contains("push_suppressed_no_session"))
    }

    @Test
    fun `unknown target is shown but falls back safely`() = runTest {
        val logger = RecordingLogger()
        val notifier = RecordingNotifier()
        val signals = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

        handler(logger, notifier, signals, foreground = false)
            .handle(payload(targetKind = "external-intent", targetId = "https://evil.example"))

        assertEquals(1, notifier.shown.size)
        assertTrue(notifier.shown.first().target == null)
        assertTrue(logger.events.any { it.contains("TARGET_FALLBACK") })
    }

    @Test
    fun `notification failure is recovered and still refreshes state`() = runTest {
        val logger = RecordingLogger()
        val notifier = RecordingNotifier(shouldThrow = true)
        val signals = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        var refreshes = 0
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            signals.collect { refreshes += 1 }
        }

        handler(logger, notifier, signals, foreground = false).handle(payload())

        assertEquals(1, refreshes)
        assertTrue(logger.events.contains("push_notification_failed"))
    }

    @Test
    fun `payload values are never logged`() = runTest {
        val logger = RecordingLogger()
        val notifier = RecordingNotifier()
        val signals = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

        handler(logger, notifier, signals, foreground = false).handle(
            payload(
                title = "Mensaje privado de chat",
                body = "Código de acuerdo SECRET-123",
            ),
        )

        val log = logger.events.joinToString(" ")
        assertFalse(log.contains("Mensaje privado"))
        assertFalse(log.contains("SECRET-123"))
        assertFalse(log.contains("notif-1"))
        assertFalse(log.contains("req-1"))
    }
}
