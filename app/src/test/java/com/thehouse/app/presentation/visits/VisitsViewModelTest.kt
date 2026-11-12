package com.thehouse.app.presentation.visits

import com.thehouse.app.domain.model.PagedResult
import com.thehouse.app.domain.model.Visit
import com.thehouse.app.domain.model.VisitCreation
import com.thehouse.app.domain.model.VisitProperty
import com.thehouse.app.domain.model.VisitStatus
import com.thehouse.app.domain.repository.VisitsRepository
import com.thehouse.app.domain.usecase.CancelVisitUseCase
import com.thehouse.app.domain.usecase.GetVisitsUseCase
import com.thehouse.app.testutil.TestMainDispatcherRule
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

private val requestedVisit = Visit(
    id = "visit-1",
    property = VisitProperty("prop-1", "Estudio", "Tulancingo"),
    scheduledAt = Instant.parse("2026-11-20T21:00:00Z"),
    timeZone = "America/Mexico_City",
    status = VisitStatus.REQUESTED,
    message = null,
    createdAt = null,
    updatedAt = null,
    cancelledAt = null,
)

private class ListVisitsRepository : VisitsRepository {
    override suspend fun createVisit(
        propertyId: String,
        scheduledAt: String,
        message: String?,
        idempotencyKey: String,
    ) = Result.success(VisitCreation(requestedVisit, false))

    override suspend fun getVisits(status: VisitStatus?, page: Int, pageSize: Int) =
        Result.success(PagedResult(listOf(requestedVisit), 1, 20, 1, 1))

    override suspend fun getVisit(id: String) = Result.success(requestedVisit)

    override suspend fun cancelVisit(id: String, reason: String?) =
        Result.success(requestedVisit.copy(status = VisitStatus.CANCELLED))
}

@OptIn(ExperimentalCoroutinesApi::class)
class VisitsViewModelTest {

    @get:Rule
    val mainDispatcherRule = TestMainDispatcherRule(StandardTestDispatcher())

    @Test
    fun `list loads requested visits`() = runTest {
        val vm = VisitsViewModel(
            getVisits = GetVisitsUseCase(ListVisitsRepository()),
            cancelVisit = CancelVisitUseCase(ListVisitsRepository()),
        )
        runCurrent()
        assertEquals(1, vm.uiState.value.items.size)
        assertEquals(VisitStatus.REQUESTED, vm.uiState.value.items.first().status)
    }

    @Test
    fun `cancel updates item in list`() = runTest {
        val vm = VisitsViewModel(
            getVisits = GetVisitsUseCase(ListVisitsRepository()),
            cancelVisit = CancelVisitUseCase(ListVisitsRepository()),
        )
        runCurrent()
        vm.onEvent(VisitsEvent.Cancel("visit-1"))
        runCurrent()
        assertEquals(VisitStatus.CANCELLED, vm.uiState.value.items.first().status)
    }
}