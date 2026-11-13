package com.thehouse.app.presentation.reports

import com.thehouse.app.domain.error.DomainError
import com.thehouse.app.domain.error.ReportException
import com.thehouse.app.domain.model.BuildingStatus
import com.thehouse.app.domain.model.ReportCreated
import com.thehouse.app.domain.model.ReportReason
import com.thehouse.app.domain.model.ReportTargetType
import com.thehouse.app.domain.model.VerificationInfo
import com.thehouse.app.domain.repository.ReportsRepository
import com.thehouse.app.domain.usecase.CreateReportUseCase
import com.thehouse.app.testutil.TestMainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

private class FakeReportsRepository(
    private val createResult: Result<ReportCreated> = Result.success(
        ReportCreated(
            id = "rep-1",
            status = "open",
            createdAt = "2026-11-12T12:00:00+00:00",
        ),
    ),
    private val delayMs: Long = 0L,
) : ReportsRepository {

    var createCalls = 0
    var lastTargetType: ReportTargetType? = null
    var lastTargetId: String? = null
    var lastReason: ReportReason? = null
    var lastDetails: String? = null

    override suspend fun createReport(
        targetType: ReportTargetType,
        targetId: String,
        reason: ReportReason,
        details: String?,
    ): Result<ReportCreated> {
        createCalls += 1
        lastTargetType = targetType
        lastTargetId = targetId
        lastReason = reason
        lastDetails = details
        if (delayMs > 0) delay(delayMs)
        return createResult
    }

    override suspend fun getBuildingVerification(buildingId: String): Result<VerificationInfo> =
        Result.success(VerificationInfo(BuildingStatus.VERIFIED, null))
}

@OptIn(ExperimentalCoroutinesApi::class)
class ReportViewModelTest {

    @get:Rule
    val mainDispatcherRule = TestMainDispatcherRule(StandardTestDispatcher())

    private fun viewModel(repository: ReportsRepository) = ReportViewModel(
        targetType = ReportTargetType.ROOM,
        targetId = "room-1",
        createReport = CreateReportUseCase(repository),
    )

    @Test
    fun `details over 500 is blocked locally`() {
        val state = ReportUiState(
            targetType = ReportTargetType.ROOM,
            targetId = "room-1",
            reason = ReportReason.SPAM,
            details = "x".repeat(ReportUiState.MAX_DETAILS + 1),
        )
        assertNotNull(state.detailsError)
        assertFalse(state.isValid)
    }

    @Test
    fun `details change truncates to contract max`() = runTest {
        val repository = FakeReportsRepository()
        val vm = viewModel(repository)
        vm.onEvent(ReportEvent.ReasonChanged(ReportReason.SPAM))
        vm.onEvent(ReportEvent.DetailsChanged("x".repeat(600)))
        advanceUntilIdle()
        val state = vm.uiState.value
        assertEquals(ReportUiState.MAX_DETAILS, state.details.length)
        assertNull(state.detailsError)
        assertTrue(state.isValid)

        vm.onEvent(ReportEvent.Submit)
        advanceUntilIdle()
        assertEquals(1, repository.createCalls)
        assertEquals("x".repeat(ReportUiState.MAX_DETAILS), repository.lastDetails)
    }

    @Test
    fun `submit without reason never calls repository`() = runTest {
        val repository = FakeReportsRepository()
        val vm = viewModel(repository)
        vm.onEvent(ReportEvent.DetailsChanged("Motivo cualquiera"))
        vm.onEvent(ReportEvent.Submit)
        advanceUntilIdle()
        assertEquals(0, repository.createCalls)
        assertFalse(vm.uiState.value.submitted)
    }

    @Test
    fun `submit sends contract fields without userId`() = runTest {
        val repository = FakeReportsRepository()
        val vm = viewModel(repository)
        vm.onEvent(ReportEvent.ReasonChanged(ReportReason.FRAUD))
        vm.onEvent(ReportEvent.DetailsChanged("Precio distinto al publicado"))
        vm.onEvent(ReportEvent.Submit)
        advanceUntilIdle()
        assertEquals(1, repository.createCalls)
        assertEquals(ReportTargetType.ROOM, repository.lastTargetType)
        assertEquals("room-1", repository.lastTargetId)
        assertEquals(ReportReason.FRAUD, repository.lastReason)
        assertEquals("Precio distinto al publicado", repository.lastDetails)
        assertTrue(vm.uiState.value.submitted)
        assertFalse(vm.uiState.value.isSubmitting)
    }

    @Test
    fun `blank details are sent as null`() = runTest {
        val repository = FakeReportsRepository()
        val vm = viewModel(repository)
        vm.onEvent(ReportEvent.ReasonChanged(ReportReason.OTHER))
        vm.onEvent(ReportEvent.DetailsChanged("   "))
        vm.onEvent(ReportEvent.Submit)
        advanceUntilIdle()
        assertEquals(1, repository.createCalls)
        assertNull(repository.lastDetails)
    }

    @Test
    fun `double submit while in flight sends only once`() = runTest {
        val repository = FakeReportsRepository(delayMs = 100)
        val vm = viewModel(repository)
        vm.onEvent(ReportEvent.ReasonChanged(ReportReason.SPAM))
        vm.onEvent(ReportEvent.Submit)
        vm.onEvent(ReportEvent.Submit)
        vm.onEvent(ReportEvent.Submit)
        advanceUntilIdle()
        assertEquals(1, repository.createCalls)
        assertTrue(vm.uiState.value.submitted)
    }

    @Test
    fun `409 report_exists shows friendly message`() = runTest {
        val repository = FakeReportsRepository(
            createResult = Result.failure(
                ReportException(DomainError.Conflict, code = com.thehouse.app.domain.error.ReportErrorCode.REPORT_EXISTS),
            ),
        )
        val vm = viewModel(repository)
        vm.onEvent(ReportEvent.ReasonChanged(ReportReason.SPAM))
        vm.onEvent(ReportEvent.Submit)
        advanceUntilIdle()
        assertEquals(
            "Ya reportaste esto y el reporte sigue en revisión.",
            vm.uiState.value.errorMessage,
        )
        assertFalse(vm.uiState.value.submitted)
    }

    @Test
    fun `account suspended shows suspension message without retry loop`() = runTest {
        val repository = FakeReportsRepository(
            createResult = Result.failure(ReportException(DomainError.AccountSuspended)),
        )
        val vm = viewModel(repository)
        vm.onEvent(ReportEvent.ReasonChanged(ReportReason.SPAM))
        vm.onEvent(ReportEvent.Submit)
        advanceUntilIdle()
        assertEquals("Tu cuenta está suspendida.", vm.uiState.value.errorMessage)
        assertEquals(1, repository.createCalls)
    }
}
