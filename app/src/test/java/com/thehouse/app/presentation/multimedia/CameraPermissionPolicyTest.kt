package com.thehouse.app.presentation.multimedia

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraPermissionPolicyTest {

    @Test
    fun `camera requires runtime permission but must only be requested by real flow`() {
        assertTrue(CameraPermissionPolicy.requiresRuntimePermission())
    }

    @Test
    fun `granted denied and permanently denied resolution`() {
        assertEquals(
            CameraPermissionPolicy.Status.GRANTED,
            CameraPermissionPolicy.resolveStatus(granted = true, askedOnce = false, showRationale = false),
        )
        assertEquals(
            CameraPermissionPolicy.Status.DENIED,
            CameraPermissionPolicy.resolveStatus(granted = false, askedOnce = false, showRationale = false),
        )
        assertEquals(
            CameraPermissionPolicy.Status.DENIED,
            CameraPermissionPolicy.resolveStatus(granted = false, askedOnce = true, showRationale = true),
        )
        assertEquals(
            CameraPermissionPolicy.Status.PERMANENTLY_DENIED,
            CameraPermissionPolicy.resolveStatus(granted = false, askedOnce = true, showRationale = false),
        )
    }

    @Test
    fun `picker cancelled maps to cancelled result`() {
        val result = MultimediaContracts.fromPickerResult(null)
        assertEquals(MultimediaContracts.SelectionResult.Cancelled, result)
    }
}
