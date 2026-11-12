package com.thehouse.app.presentation.landlord

import com.thehouse.app.domain.error.DomainError
import com.thehouse.app.domain.error.RequestErrorCode
import com.thehouse.app.domain.error.RequestException
import com.thehouse.app.domain.model.PagedResult
import com.thehouse.app.domain.model.RequestStatus
import com.thehouse.app.domain.model.RequestStudent
import com.thehouse.app.domain.model.RequestRoom
import com.thehouse.app.domain.model.RoomRequest
import com.thehouse.app.domain.repository.RequestsRepository
import com.thehouse.app.domain.usecase.AcceptRequestUseCase
import com.thehouse.app.domain.usecase.GetLandlordRequestsUseCase
import com.thehouse.app.domain.usecase.RejectRequestUseCase
import com.thehouse.app.testutil.TestMainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

private val landlordRequest = RoomRequest(
    id = "req-1",
    status = RequestStatus.PENDING,
    room = RequestRoom("room-1", "b-1", "Cuarto", "Tulancingo"),
    quickQuestions = emptyList(),
    message = "Hola",
    student = RequestStudent("Estudiante", "UAEH"),
    landlord = null,
    visit = null,
    conversationId = "conv-1",
    rejection = null,
    roomAvailable = true,
    createdAt = null,
    updatedAt = null,
)

private class FakeLandlordRepository(
    private val acceptResult: Result<RoomRequest> =
        Result.success(landlordRequest.copy(status = RequestStatus.ACCEPTED)),
    private val rejectResult: Result<RoomRequest> =
        Result.success(landlordRequest.copy(status = RequestStatus.REJECTED)),
) : RequestsRepository {
    var rejectedReason: String? = null
    var rejectedNote: String? = null

    override suspend fun createRequest(
        roomId: String,
        quickQuestions: List<com.thehouse.app.domain.model.QuickQuestion>,
        message: String?,
    ) = Result.success(landlordRequest)

    override suspend fun getRequests(status: RequestStatus?, page: Int, pageSize: Int) =
        Result.success(PagedResult(listOf(landlordRequest), 1, 20, 1, 1))

    override suspend fun getRequest(id: String) = Result.success(landlordRequest)

    override suspend fun cancelRequest(id: String) = Result.success(landlordRequest)

    override suspend fun getLandlordRequests(
        status: RequestStatus?,
        roomId: String?,
        page: Int,
        pageSize: Int,
    ): Result<PagedResult<RoomRequest>> = if (landlordRequired) {
        Result.failure(
            RequestException(DomainError.Unauthorized, RequestErrorCode.LANDLORD_REQUIRED),
        )
    } else {
        Result.success(PagedResult(listOf(landlordRequest.copy(status = currentStatus)), 1, 20, 1, 1))
    }

    var landlordRequired = false
    private var currentStatus = RequestStatus.PENDING

    override suspend fun getLandlordRequest(id: String) = Result.success(landlordRequest.copy(status = currentStatus))

    override suspend fun acceptRequest(id: String): Result<RoomRequest> {
        currentStatus = RequestStatus.ACCEPTED
        return acceptResult
    }

    override suspend fun rejectRequest(id: String, reason: String, note: String?): Result<RoomRequest> {
        rejectedReason = reason
        rejectedNote = note
        currentStatus = RequestStatus.REJECTED
        return rejectResult
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class LandlordRequestsViewModelTest {

    @get:Rule
    val mainDispatcherRule = TestMainDispatcherRule(StandardTestDispatcher())

    private fun createViewModel(repository: FakeLandlordRepository) =
        LandlordRequestsViewModel(
            getLandlordRequests = GetLandlordRequestsUseCase(repository),
            acceptRequest = AcceptRequestUseCase(repository),
            rejectRequest = RejectRequestUseCase(repository),
        )

    @Test
    fun `list loads landlord requests with student info`() = runTest {
        val vm = createViewModel(FakeLandlordRepository())
        runCurrent()
        assertEquals(1, vm.uiState.value.items.size)
        assertEquals("Estudiante", vm.uiState.value.items.first().student?.name)
    }

    @Test
    fun `landlord_required shows friendly message`() = runTest {
        val repository = FakeLandlordRepository().apply { landlordRequired = true }
        val vm = createViewModel(repository)
        runCurrent()
        assertEquals(
            "Necesitas activar el modo arrendador.",
            vm.uiState.value.errorMessage,
        )
    }

    @Test
    fun `accept updates request state`() = runTest {
        val vm = createViewModel(FakeLandlordRepository())
        runCurrent()
        vm.onEvent(LandlordRequestsEvent.Accept("req-1"))
        runCurrent()
        assertEquals(RequestStatus.ACCEPTED, vm.uiState.value.items.first().status)
    }

    @Test
    fun `room_unavailable on accept shows friendly message`() = runTest {
        val repository = FakeLandlordRepository(
            acceptResult = Result.failure(
                RequestException(DomainError.Conflict, RequestErrorCode.ROOM_UNAVAILABLE),
            ),
        )
        val vm = createViewModel(repository)
        runCurrent()
        vm.onEvent(LandlordRequestsEvent.Accept("req-1"))
        runCurrent()
        assertEquals("El cuarto ya no está disponible.", vm.uiState.value.errorMessage)
    }

    @Test
    fun `reject sends reason and optional note`() = runTest {
        val repository = FakeLandlordRepository()
        val vm = createViewModel(repository)
        runCurrent()
        vm.onEvent(LandlordRequestsEvent.Reject("req-1", "not_available", "Ocupado"))
        runCurrent()
        assertEquals("not_available", repository.rejectedReason)
        assertEquals("Ocupado", repository.rejectedNote)
        assertEquals(RequestStatus.REJECTED, vm.uiState.value.items.first().status)
    }

    @Test
    fun `reject other without note surfaces backend validation`() = runTest {
        val repository = FakeLandlordRepository(
            rejectResult = Result.failure(
                RequestException(DomainError.Unknown, RequestErrorCode.UNKNOWN),
            ),
        )
        val vm = createViewModel(repository)
        runCurrent()
        vm.onEvent(LandlordRequestsEvent.Reject("req-1", "other", null))
        runCurrent()
        assertEquals(RequestErrorCode.UNKNOWN, vm.uiState.value.errorCode)
        assertTrue(vm.uiState.value.errorMessage != null)
        assertEquals("other", repository.rejectedReason)
        assertEquals(null, repository.rejectedNote)
    }
}
