package com.thehouse.app.presentation.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationPermissionPolicyTest {

    @Test
    fun `android below 13 does not require runtime permission`() {
        assertFalse(NotificationPermissionPolicy.runtimePermissionRequired(32))
        assertTrue(NotificationPermissionPolicy.canPostNotifications(sdkInt = 32, runtimeGranted = false))
        assertEquals(
            NotificationPermissionStatus.GRANTED,
            NotificationPermissionPolicy.resolveStatus(
                sdkInt = 32,
                runtimeGranted = false,
                askedOnce = false,
                showRationale = false,
            ),
        )
    }

    @Test
    fun `android 13 requires runtime permission`() {
        assertTrue(NotificationPermissionPolicy.runtimePermissionRequired(33))
        assertFalse(NotificationPermissionPolicy.canPostNotifications(sdkInt = 33, runtimeGranted = false))
        assertTrue(NotificationPermissionPolicy.canPostNotifications(sdkInt = 33, runtimeGranted = true))
    }

    @Test
    fun `android 13 granted permission is ready`() {
        assertEquals(
            NotificationPermissionStatus.GRANTED,
            NotificationPermissionPolicy.resolveStatus(
                sdkInt = 33,
                runtimeGranted = true,
                askedOnce = true,
                showRationale = false,
            ),
        )
    }

    @Test
    fun `android 13 first denial can be requested again`() {
        assertEquals(
            NotificationPermissionStatus.DENIED,
            NotificationPermissionPolicy.resolveStatus(
                sdkInt = 33,
                runtimeGranted = false,
                askedOnce = false,
                showRationale = false,
            ),
        )
    }

    @Test
    fun `android 13 denial with rationale can be requested again`() {
        assertEquals(
            NotificationPermissionStatus.DENIED,
            NotificationPermissionPolicy.resolveStatus(
                sdkInt = 33,
                runtimeGranted = false,
                askedOnce = true,
                showRationale = true,
            ),
        )
    }

    @Test
    fun `android 13 permanent denial requires system settings`() {
        assertEquals(
            NotificationPermissionStatus.PERMANENTLY_DENIED,
            NotificationPermissionPolicy.resolveStatus(
                sdkInt = 33,
                runtimeGranted = false,
                askedOnce = true,
                showRationale = false,
            ),
        )
    }

    @Test
    fun `denial never breaks app usage`() {
        // La app sigue operativa: el permiso solo condiciona notificaciones.
        val status = NotificationPermissionPolicy.resolveStatus(
            sdkInt = 33,
            runtimeGranted = false,
            askedOnce = true,
            showRationale = false,
        )
        assertEquals(NotificationPermissionStatus.PERMANENTLY_DENIED, status)
        assertFalse(NotificationPermissionPolicy.canPostNotifications(33, false))
    }
}
