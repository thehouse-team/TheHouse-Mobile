package com.thehouse.app.presentation.visits

import com.thehouse.app.domain.error.DomainError
import com.thehouse.app.domain.error.VisitException
import com.thehouse.app.domain.model.PagedResult
import com.thehouse.app.domain.model.Visit
import com.thehouse.app.domain.model.VisitCreation
import com.thehouse.app.domain.model.VisitProperty
import com.thehouse.app.domain.model.VisitStatus
import com.thehouse.app.domain.repository.VisitsRepository
import com.thehouse.app.domain.usecase.CreateVisitUseCase
import com.thehouse.app.testutil.TestMainDispatcherRule
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test

private val visit = Visit(
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

private class FakeVisitsRepository : VisitsRepository {
    val keys = mutableListOf<String>()
    var result: Result<VisitCreation> = Result.success(VisitCreation(visit, false))

    override suspend fun createVisit(
        propertyId: String,
        scheduledAt: String,
        message: String?,
        idempotencyKey: String,
    ): Result<VisitCreation> {
        keys += idempotencyKey
        return result
    }

    override suspend fun getVisits(status: VisitStatus?, page: Int, pageSize: Int) =
        Result.success(PagedResult<Visit>(emptyList(), 1, 20, 0, 0))

    override suspend fun getVisit(id: String) = Result.success(visit)

    override suspend fun cancelVisit(id: String, reason: String?) = Result.success(visit)
}

@OptIn(ExperimentalCoroutinesApi::class)
class ScheduleVisitViewModelTest {

    @get:Rule
    val mainDispatcherRule = TestMainDispatcherRule(StandardTestDispatcher())

    private fun vm(repo: FakeVisitsRepository, keys: Iterator<String> = generateSequence(1) { it + 1 }
        .map { "key-$it" }.iterator()): ScheduleVisitViewModel =
        ScheduleVisitViewModel(
            propertyId = "prop-1",
            createVisit = CreateVisitUseCase(repo),
            now = { Instant.parse("2026-11-20T12:00:00Z") },
            newKey = { keys.next() },
        ).apply {
            onEvent(ScheduleVisitEvent.DateSelected(LocalDate.of(2026, 11, 20)))
            onEvent(ScheduleVisitEvent.TimeSelected(LocalTime.of(15, 0)))
        }

    @Test
    fun `retry same intent reuses key`() = runTest {
        val repo = FakeVisitsRepository()
        repo.result = Result.failure(VisitException(DomainError.Network))
        val model = vm(repo)
        model.onEvent(ScheduleVisitEvent.Submit)
        runCurrent()
        model.onEvent(ScheduleVisitEvent.Submit)
        runCurrent()
        assertEquals(2, repo.keys.size)
        assertEquals(repo.keys[0], repo.keys[1])
    }

    @Test
    fun `new intent generates new key`() = runTest {
        val repo = FakeVisitsRepository()
        repo.result = Result.failure(VisitException(DomainError.Network))
        val model = vm(repo)
        model.onEvent(ScheduleVisitEvent.Submit)
        runCurrent()
        model.onEvent(ScheduleVisitEvent.TimeSelected(LocalTime.of(16, 0)))
        model.onEvent(ScheduleVisitEvent.Submit)
        runCurrent()
        assertEquals(2, repo.keys.size)
        assertNotEquals(repo.keys[0], repo.keys[1])
    }

    @Test
    fun `double submit is blocked`() = runTest {
        val repo = FakeVisitsRepository()
        val model = vm(repo)
        model.onEvent(ScheduleVisitEvent.Submit)
        model.onEvent(ScheduleVisitEvent.Submit)
        runCurrent()
        assertEquals(1, repo.keys.size)
    }
}