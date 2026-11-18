package com.thehouse.app.domain.multimedia

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MultimediaStateTest {

    @Test
    fun `default moderation state is blocked and never claims moderated or uploaded`() {
        val state = ModerationUiState()
        assertEquals(ModerationContractStatus.BLOCKED_NO_BACKEND_CONTRACT, state.contractStatus)
        assertFalse(state.canClaimModerated)
        assertFalse(state.canClaimUploaded)
        assertTrue(state.notice.contains("MULTIMEDIA-MODERATION-BACKEND-BLOCKED"))
    }

    @Test
    fun `available contract still requires real validation before claiming`() {
        val state = ModerationUiState(contractStatus = ModerationContractStatus.AVAILABLE)
        assertFalse(state.canClaimModerated)
        assertFalse(state.canClaimUploaded)
    }

    @Test
    fun `processing stages cover required honest states`() {
        val required = setOf(
            MultimediaProcessingStage.SELECTING,
            MultimediaProcessingStage.PROCESSING,
            MultimediaProcessingStage.READY,
            MultimediaProcessingStage.VALIDATION_ERROR,
            MultimediaProcessingStage.MODERATION_PENDING,
            MultimediaProcessingStage.MODERATION_REJECTED,
            MultimediaProcessingStage.UPLOAD_PENDING,
        )
        assertEquals(required, MultimediaProcessingStage.entries.toSet())
    }
}
