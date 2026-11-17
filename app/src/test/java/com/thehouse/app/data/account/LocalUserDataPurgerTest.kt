package com.thehouse.app.data.account

import com.thehouse.app.data.account.repository.LocalUserDataPurgerImpl
import com.thehouse.app.data.favorites.state.FavoriteIdsStore
import com.thehouse.app.data.notifications.push.PendingNotificationNavigation
import com.thehouse.app.domain.auth.LocalSessionCleaner
import com.thehouse.app.domain.common.DispatcherProvider
import com.thehouse.app.domain.push.LocalPushNotifier
import com.thehouse.app.domain.push.NotificationDestination
import com.thehouse.app.domain.push.PushPayload
import com.thehouse.app.domain.repository.OnboardingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeDispatcherProvider : DispatcherProvider {
    override val io = Dispatchers.Unconfined
    override val default = Dispatchers.Unconfined
}

private class RecordingSessionCleaner(
    private val events: MutableList<String>,
    private val failure: Throwable? = null,
) : LocalSessionCleaner {
    override suspend fun clearLocalSession() {
        events += "session"
        failure?.let { throw it }
    }
}

private class FakeOnboardingRepository(
    private val events: MutableList<String>,
) : OnboardingRepository {

    val clearedKeys = mutableListOf<String>()

    override fun keyFor(userId: String?): String = "onboarding:${userId ?: "local"}"

    override suspend fun isCompleted(key: String): Boolean = false

    override suspend fun setCompleted(key: String, completed: Boolean) = Unit

    override suspend fun clear(key: String) {
        clearedKeys += key
        events += "onboarding"
    }
}

private class FakeLocalPushNotifier(
    private val events: MutableList<String>,
) : LocalPushNotifier {

    var cancelAllCalls = 0
        private set

    override fun show(payload: PushPayload) = Unit

    override fun cancelAll() {
        cancelAllCalls += 1
        events += "notifications"
    }
}

class LocalUserDataPurgerTest {

    private class Fixture(
        val events: MutableList<String> = mutableListOf(),
        sessionFailure: Throwable? = null,
    ) {
        val onboarding = FakeOnboardingRepository(events)
        val favorites = FavoriteIdsStore()
        val pendingNavigation = PendingNotificationNavigation()
        val notifier = FakeLocalPushNotifier(events)
        val cleaner = RecordingSessionCleaner(events, sessionFailure)
        val purger = LocalUserDataPurgerImpl(
            onboardingRepository = onboarding,
            favoritesStore = favorites,
            pendingNotificationNavigation = pendingNavigation,
            localPushNotifier = notifier,
            httpCache = null,
            localSessionCleaner = cleaner,
            dispatchers = FakeDispatcherProvider(),
        )
    }

    @Test
    fun `purges account scoped state with session first`() = runTest {
        val fixture = Fixture()
        fixture.favorites.setAll(setOf("prop-1", "prop-2"))
        fixture.pendingNavigation.offer(NotificationDestination.Request("request-1"))

        fixture.purger.purge("u-15")

        assertEquals(listOf("session", "onboarding", "notifications"), fixture.events)
        assertEquals(listOf("onboarding:u-15"), fixture.onboarding.clearedKeys)
        assertTrue(fixture.favorites.ids.value.isEmpty())
        assertNull(fixture.pendingNavigation.destination.value)
        assertEquals(1, fixture.notifier.cancelAllCalls)
    }

    @Test
    fun `does not clear onboarding for an unknown user`() = runTest {
        val fixture = Fixture()

        fixture.purger.purge(null)

        assertTrue(fixture.onboarding.clearedKeys.isEmpty())
        assertTrue(fixture.favorites.ids.value.isEmpty())
        assertEquals(1, fixture.notifier.cancelAllCalls)
    }

    @Test
    fun `continues cleanup when session clearing fails`() = runTest {
        val fixture = Fixture(sessionFailure = IllegalStateException("storage error"))
        fixture.favorites.setAll(setOf("prop-1"))
        fixture.pendingNavigation.offer(NotificationDestination.Request("request-1"))

        fixture.purger.purge("u-15")

        assertEquals(listOf("session", "onboarding", "notifications"), fixture.events)
        assertTrue(fixture.favorites.ids.value.isEmpty())
        assertNull(fixture.pendingNavigation.destination.value)
        assertEquals(1, fixture.notifier.cancelAllCalls)
    }
}
