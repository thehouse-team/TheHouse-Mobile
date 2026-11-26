package com.thehouse.app.presentation.smartmatch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifica que el logging DEBUG del flujo Smart Match sea seguro (sin PII)
 * y cubra los eventos exigidos por el runbook.
 */
class SmartMatchFlowLoggerTest {

    @Test
    fun `flow events have no PII and cover all control points`() {
        val events = mutableListOf<String>()
        val previous = SmartMatchFlowLogger.sink
        SmartMatchFlowLogger.sink = events::add
        try {
            SmartMatchFlowLogger.screenEnter()
            SmartMatchFlowLogger.viewModelCreated()
            SmartMatchFlowLogger.emit("load_start")
            SmartMatchFlowLogger.emit("session=Anonymous")
            SmartMatchFlowLogger.emit("preferences_start")
            SmartMatchFlowLogger.preferences("present")
            SmartMatchFlowLogger.emit("guest_query_start")
            SmartMatchFlowLogger.emit("auth_query_start")
            SmartMatchFlowLogger.emit("candidates_start")
            SmartMatchFlowLogger.candidates(3)
            SmartMatchFlowLogger.emit("ranking_start")
            SmartMatchFlowLogger.state(SmartMatchUiState.Loading)
            SmartMatchFlowLogger.exception("fetchCandidates", IllegalStateException("boom"))
        } finally {
            SmartMatchFlowLogger.sink = previous
        }

        val joined = events.joinToString("\n")
        assertTrue(joined.contains("enter_screen"))
        assertTrue(joined.contains("viewmodel_created"))
        assertTrue(joined.contains("load_start"))
        assertTrue(joined.contains("session=Anonymous"))
        assertTrue(joined.contains("preferences_start"))
        assertTrue(joined.contains("preferences_result=present"))
        assertTrue(joined.contains("guest_query_start"))
        assertTrue(joined.contains("auth_query_start"))
        assertTrue(joined.contains("candidates_result count=3"))
        assertTrue(joined.contains("ranking_start"))
        assertTrue(joined.contains("state=Loading"))
        assertTrue(joined.contains("exception=IllegalStateException operation=fetchCandidates"))
        // Seguridad: el mensaje de la excepción y cualquier PII quedan fuera.
        assertTrue(!joined.contains("boom"))
        assertTrue(!joined.contains("@"))
    }

    @Test
    fun `state logs only type names without payload`() {
        val events = mutableListOf<String>()
        val previous = SmartMatchFlowLogger.sink
        SmartMatchFlowLogger.sink = events::add
        try {
            SmartMatchUiState.NoPreferences.let(SmartMatchFlowLogger::state)
            SmartMatchUiState.Empty.let(SmartMatchFlowLogger::state)
            SmartMatchUiState.RequiresAuth.let(SmartMatchFlowLogger::state)
            SmartMatchUiState.Error(com.thehouse.app.presentation.common.UiError.Network)
                .let(SmartMatchFlowLogger::state)
        } finally {
            SmartMatchFlowLogger.sink = previous
        }
        assertEquals(
            listOf("state=NoPreferences", "state=Empty", "state=RequiresAuth", "state=Error"),
            events,
        )
    }
}
