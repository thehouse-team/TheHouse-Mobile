package com.thehouse.app.domain.push

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PushTokenCoordinatorTest {

    private class FakeProvider(var token: String? = null) : PushTokenProvider {
        var failure: Throwable? = null

        override suspend fun currentToken(): String? {
            failure?.let { throw it }
            return token
        }
    }

    private class RecordingLogger : PushLogger {
        val events = mutableListOf<String>()

        override fun info(event: String) {
            events.add(event)
        }

        override fun warn(event: String, reason: String?) {
            events.add(if (reason == null) event else "$event:$reason")
        }
    }

    private val token = "fcm_token_abcdefghijklmnopqrstuvwxyz0123456789ABCDEF"

    @Test
    fun `refresh exposes only a truncated fingerprint`() = runTest {
        val logger = RecordingLogger()
        val coordinator = PushTokenCoordinator(FakeProvider(token), logger)

        coordinator.refresh()

        val state = coordinator.state.value as PushTokenState.Available
        assertEquals(PushTokenFingerprint.of(token), state.fingerprint)
        assertFalse(state.fingerprint.contains(token))
        assertNotNull(coordinator.currentTokenForBackendSync())
        assertEquals(token, coordinator.currentTokenForBackendSync())
        assertFalse(logger.events.joinToString(" ").contains(token))
    }

    @Test
    fun `onNewToken updates state without logging the token`() = runTest {
        val logger = RecordingLogger()
        val coordinator = PushTokenCoordinator(FakeProvider(null), logger)

        coordinator.onNewToken(token)

        assertTrue(coordinator.state.value is PushTokenState.Available)
        assertFalse(logger.events.joinToString(" ").contains(token))
        assertTrue(logger.events.any { it.contains("on_new_token") })
    }

    @Test
    fun `invalid tokens are rejected`() = runTest {
        val logger = RecordingLogger()
        val coordinator = PushTokenCoordinator(FakeProvider(null), logger)

        coordinator.onNewToken("short")
        assertEquals(PushTokenState.Unavailable, coordinator.state.value)

        coordinator.onNewToken("token with spaces should be rejected")
        assertEquals(PushTokenState.Unavailable, coordinator.state.value)
    }

    @Test
    fun `provider null means firebase not configured`() = runTest {
        val logger = RecordingLogger()
        val coordinator = PushTokenCoordinator(FakeProvider(null), logger)

        coordinator.refresh()

        assertEquals(PushTokenState.Unavailable, coordinator.state.value)
        assertTrue(logger.events.any { it.contains("unavailable") })
    }

    @Test
    fun `provider failure is recoverable`() = runTest {
        val provider = FakeProvider().apply { failure = IllegalStateException("no firebase") }
        val logger = RecordingLogger()
        val coordinator = PushTokenCoordinator(provider, logger)

        coordinator.refresh()

        assertEquals(PushTokenState.Unavailable, coordinator.state.value)
        assertTrue(logger.events.any { it.contains("fcm_token_unavailable") })
        assertFalse(logger.events.joinToString(" ").contains("no firebase"))
    }
}
