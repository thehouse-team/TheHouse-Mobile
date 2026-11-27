package com.thehouse.app.presentation.visits

import com.thehouse.app.domain.error.DomainError
import com.thehouse.app.domain.error.VisitException
import com.thehouse.app.domain.model.PagedResult
import com.thehouse.app.domain.model.Visit
import com.thehouse.app.domain.model.VisitCreation
import com.thehouse.app.domain.model.VisitProperty
import com.thehouse.app.domain.model.VisitStatus
import com.thehouse.app.domain.repository.VisitsRepository
import com.thehouse.app.domain.usecase.CreateRoomVisitUseCase
import com.thehouse.app.domain.usecase.CreateVisitUseCase
import com.thehouse.app.testutil.TestMainDispatcherRule
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

private class RecordingVisitsRepository : VisitsRepository {
    val roomCalls = mutableListOf<Triple<String, String, String>>()
    var key: String = "uuid-key"
    var scheduledAt: String = ""
    var message: String? = null
    var failWith: VisitException? = null

    override suspend fun createRoomVisit(
        roomId: String,
        scheduledAt: String,
        message: String?,
        idempotencyKey: String,
    ): Result<VisitCreation> {
        roomCalls += Triple(roomId, scheduledAt, idempotencyKey)
        this.scheduledAt = scheduledAt
        this.message = message
        failWith?.let { return Result.failure(it) }
        return Result.success(VisitCreation(sampleVisit(), false))
    }

    override suspend fun createVisit(
        propertyId: String,
        scheduledAt: String,
        message: String?,
        idempotencyKey: String,
    ): Result<VisitCreation> = Result.success(VisitCreation(sampleVisit(), false))

    override suspend fun getVisits(status: VisitStatus?, page: Int, pageSize: Int) =
        Result.success(PagedResult<Visit>(emptyList(), 1, 20, 0, 0))

    override suspend fun getVisit(id: String) = Result.success(sampleVisit())
    override suspend fun cancelVisit(id: String, reason: String?) = Result.success(sampleVisit())
}

private fun sampleVisit() = Visit(
    id = "visit-1",
    property = VisitProperty("prop-1", "Estudio", "Tulancingo"),
    scheduledAt = Instant.parse("2026-11-30T21:00:00Z"),
    timeZone = "America/Mexico_City",
    status = VisitStatus.REQUESTED,
    message = null,
    createdAt = null,
    updatedAt = null,
    cancelledAt = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class ScheduleVisitValidationTest {

    @get:Rule
    val dispatcherRule = TestMainDispatcherRule(StandardTestDispatcher())

    private val now = { Instant.parse("2026-11-27T18:00:00Z") } // 12:00 CDMX

    private fun roomVm(repo: RecordingVisitsRepository) = ScheduleVisitViewModel(
        propertyId = "",
        createVisit = CreateVisitUseCase(repo),
        now = now,
        newKey = { repo.key },
        roomId = "room-666f1a2b3c4d5e6f7a8b9c0d",
        createRoomVisit = CreateRoomVisitUseCase(repo),
    )

    @Test
    fun `submit without date shows error instead of silent return`() = runTest {
        val vm = roomVm(RecordingVisitsRepository())
        vm.onEvent(ScheduleVisitEvent.Submit)
        advanceUntilIdle()
        assertEquals(ScheduleVisitStatus.ERROR, vm.uiState.value.status)
        assertNotNull(vm.uiState.value.formError)
    }

    @Test
    fun `changing date clears incompatible time`() = runTest {
        val vm = roomVm(RecordingVisitsRepository())
        vm.onEvent(ScheduleVisitEvent.DateSelected(LocalDate.of(2026, 11, 30)))
        vm.onEvent(ScheduleVisitEvent.TimeSelected(LocalTime.of(20, 0)))
        // Último día (60 d) con hora 20:00 => fuera de ventana
        vm.onEvent(ScheduleVisitEvent.DateSelected(LocalDate.of(2027, 1, 26)))
        assertNull(vm.uiState.value.time)
        assertNotNull(vm.uiState.value.formError)
    }

    @Test
    fun `room visit posts real roomId with UUID key and Mexico offset`() = runTest {
        val repo = RecordingVisitsRepository()
        val vm = roomVm(repo)
        vm.onEvent(ScheduleVisitEvent.DateSelected(LocalDate.of(2026, 11, 30)))
        vm.onEvent(ScheduleVisitEvent.TimeSelected(LocalTime.of(15, 0)))
        vm.onEvent(ScheduleVisitEvent.MessageChanged("Hola"))
        vm.onEvent(ScheduleVisitEvent.Submit)
        advanceUntilIdle()

        assertEquals(1, repo.roomCalls.size)
        val (roomId, scheduledAt, key) = repo.roomCalls.single()
        assertEquals("room-666f1a2b3c4d5e6f7a8b9c0d", roomId)
        assertTrue(scheduledAt.startsWith("2026-11-30T15:00"))
        assertTrue(scheduledAt.endsWith("-06:00"))
        assertEquals("uuid-key", key)
        assertEquals("Hola", repo.message)
        assertEquals(ScheduleVisitStatus.SUCCESS, vm.uiState.value.status)
    }

    @Test
    fun `double submit guard prevents two requests`() = runTest {
        val repo = RecordingVisitsRepository()
        val vm = roomVm(repo)
        vm.onEvent(ScheduleVisitEvent.DateSelected(LocalDate.of(2026, 11, 30)))
        vm.onEvent(ScheduleVisitEvent.TimeSelected(LocalTime.of(15, 0)))
        vm.onEvent(ScheduleVisitEvent.Submit)
        vm.onEvent(ScheduleVisitEvent.Submit)
        advanceUntilIdle()
        assertEquals(1, repo.roomCalls.size)
    }

    @Test
    fun `http failure is surfaced with code and domain error`() = runTest {
        val repo = RecordingVisitsRepository()
        repo.failWith = VisitException(DomainError.Conflict, com.thehouse.app.domain.error.VisitErrorCode.SCHEDULE_OVERLAP)
        val vm = roomVm(repo)
        vm.onEvent(ScheduleVisitEvent.DateSelected(LocalDate.of(2026, 11, 30)))
        vm.onEvent(ScheduleVisitEvent.TimeSelected(LocalTime.of(15, 0)))
        vm.onEvent(ScheduleVisitEvent.Submit)
        advanceUntilIdle()

        assertEquals(ScheduleVisitStatus.ERROR, vm.uiState.value.status)
        assertEquals(com.thehouse.app.domain.error.VisitErrorCode.SCHEDULE_OVERLAP, vm.uiState.value.errorCode)
        assertEquals(DomainError.Conflict, vm.uiState.value.domainError)
        assertTrue(VisitErrorMessages.map(vm.uiState.value.errorCode).contains("horario"))
    }
}
