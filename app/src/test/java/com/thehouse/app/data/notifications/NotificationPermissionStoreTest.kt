package com.thehouse.app.data.notifications

import com.thehouse.app.testutil.InMemoryPreferencesStore
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationPermissionStoreTest {

    @Test
    fun `asked decision survives a new store instance`() = runTest {
        val prefs = InMemoryPreferencesStore()
        val first = DataStoreNotificationPermissionStore(prefs)
        assertFalse(first.wasAsked())
        first.markAsked()

        // Simula muerte de proceso: nueva instancia sobre el mismo DataStore.
        val afterProcessDeath = DataStoreNotificationPermissionStore(prefs)
        assertTrue(afterProcessDeath.wasAsked())
    }
}
