package com.thehouse.app.presentation.requests

import com.thehouse.app.domain.model.PagedResult
import com.thehouse.app.domain.model.QuickQuestion
import com.thehouse.app.domain.model.RequestStatus
import com.thehouse.app.domain.model.RequestVisit
import com.thehouse.app.domain.model.RequestRoom
import com.thehouse.app.domain.model.RoomRequest
import com.thehouse.app.domain.repository.RequestsRepository
import com.thehouse.app.domain.usecase.CancelRequestUseCase
import com.thehouse.app.domain.usecase.CreateRequestUseCase
import com.thehouse.app.domain.usecase.GetUserRequestsUseCase
import com.thehouse.app.testutil.TestMainDispatcherRule
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

private val sampleRequest = RoomRequest(
    id = "req-1",
    status = RequestStatus.PENDING,
    room = RequestRoom("room-1", "b-1", "Cuarto", "Tulancingo"),
    quickQuestions = listOf(QuickQuestion.AVAILABLE_NOW),
    message = "Hola",
    student = null,
    landlord = com.thehouse.app.domain.model.RequestLandlord("Arrendador"),
    visit = RequestVisit("v-1", Instant.EPOCH, "requested"),
    conversationId = "conv-1",
    rejection = null,
    roomAvailable = true,
    createdAt = null,
    updatedAt = null,
)

private class FakeRequestsRepository(
    private val createResult: Result<RoomRequest> = Result.success(sampleRequest),
) : RequestsRepository {
    var createdRoomId: String? = null
    var createdQuestions: List<QuickQuestion> = emptyList()
    var createdMessage: String? = null

    override suspend fun createRequest(
        roomId: String,
        quickQuestions: List<QuickQuestion>,
        message: String?,
    ): Result<RoomRequest> {
        createdRoomId = roomId
        createdQuestions = quickQuestions
        createdMessage = message
        return createResult
    }

    override suspend fun getRequests(status: RequestStatus?, page: Int, pageSize: Int) =
        Result.success(PagedResult(listOf(sampleRequest), 1, 20, 1, 1))

    override suspend fun getRequest(id: String) = Result.success(sampleRequest)

    override suspend fun cancelRequest(id: String) =
        Result.success(sampleRequest.copy(status = RequestStatus.CANCELLED))

    override suspend fun getLandlordRequests(
        status: RequestStatus?,
        roomId: String?,
        page: Int,
        pageSize: Int,
    ) = Result.success(PagedResult(listOf(sampleRequest), 1, 20, 1, 1))

    override suspend fun getLandlordRequest(id: String) = Result.success(sampleRequest)

    override suspend fun acceptRequest(id: String) =
        Result.success(sampleRequest.copy(status = RequestStatus.ACCEPTED))

    override suspend fun rejectRequest(id: String, reason: String, note: String?) =
        Result.success(sampleRequest.copy(status = RequestStatus.REJECTED))
}

@OptIn(ExperimentalCoroutinesApi::class)
class RequestsViewModelTest {

    @get:Rule
    val mainDispatcherRule = TestMainDispatcherRule(StandardTestDispatcher())

    private fun createRequestViewModel(repository: FakeRequestsRepository) =
        CreateRequestViewModel(
            roomId = "room-1",
            createRequest = CreateRequestUseCase(repository),
        )

    @Test
    fun `create request with quick question only`() = runTest {
        val repository = FakeRequestsRepository()
        val vm = createRequestViewModel(repository)
        vm.onEvent(CreateRequestEvent.QuickQuestionToggled(QuickQuestion.AVAILABLE_NOW))
        vm.onEvent(CreateRequestEvent.Send)
        runCurrent()
        assertEquals("room-1", repository.createdRoomId)
        assertEquals(listOf(QuickQuestion.AVAILABLE_NOW), repository.createdQuestions)
        assertNullMessage(repository)
        assertFalse(vm.uiState.value.isSending)
    }

    @Test
    fun `create request blocked without quickQuestions and message`() = runTest {
        val repository = FakeRequestsRepository()
        val vm = createRequestViewModel(repository)
        vm.onEvent(CreateRequestEvent.Send)
        runCurrent()
        // No se llamó al repositorio: falta quickQuestion y message.
        assertTrue(repository.createdRoomId == null)
        assertTrue(vm.uiState.value.errorMessage != null)
    }

    @Test
    fun `message change is capped at 500 chars`() = runTest {
        val repository = FakeRequestsRepository()
        val vm = createRequestViewModel(repository)
        vm.onEvent(CreateRequestEvent.MessageChanged("a".repeat(600)))
        assertEquals(500, vm.uiState.value.message.length)
        vm.onEvent(CreateRequestEvent.Send)
        runCurrent()
        assertEquals(500, repository.createdMessage?.length)
    }

    @Test
    fun `active_request_exists shows friendly message`() = runTest {
        val repository = FakeRequestsRepository(
            createResult = Result.failure(
                com.thehouse.app.domain.error.RequestException(
                    com.thehouse.app.domain.error.DomainError.Conflict,
                    com.thehouse.app.domain.error.RequestErrorCode.ACTIVE_REQUEST_EXISTS,
                ),
            ),
        )
        val vm = createRequestViewModel(repository)
        vm.onEvent(CreateRequestEvent.QuickQuestionToggled(QuickQuestion.DEPOSIT))
        vm.onEvent(CreateRequestEvent.Send)
        runCurrent()
        assertEquals(
            "Ya tienes una solicitud activa para este cuarto.",
            vm.uiState.value.errorMessage,
        )
        assertEquals(
            com.thehouse.app.domain.error.RequestErrorCode.ACTIVE_REQUEST_EXISTS,
            vm.uiState.value.errorCode,
        )
    }

    @Test
    fun `list loads student requests and cancel updates status`() = runTest {
        val repository = FakeRequestsRepository()
        val vm = com.thehouse.app.presentation.requests.RequestsViewModel(
            getRequests = GetUserRequestsUseCase(repository),
            cancelRequest = CancelRequestUseCase(repository),
        )
        runCurrent()
        assertEquals(1, vm.uiState.value.items.size)
        vm.onEvent(com.thehouse.app.presentation.requests.RequestsEvent.Cancel("req-1"))
        runCurrent()
        assertEquals(RequestStatus.CANCELLED, vm.uiState.value.items.first().status)
    }

    private fun assertNullMessage(repository: FakeRequestsRepository) {
        assertTrue(repository.createdMessage == null)
    }
}
