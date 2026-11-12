package com.thehouse.app.presentation.chat

import com.thehouse.app.domain.model.ChatMessage
import com.thehouse.app.domain.model.Conversation
import com.thehouse.app.domain.model.ConversationCounterpart
import com.thehouse.app.domain.model.ConversationRoom
import com.thehouse.app.domain.model.ConversationStatus
import com.thehouse.app.domain.model.MessageSent
import com.thehouse.app.domain.model.MessagesFetch
import com.thehouse.app.domain.model.MessagesPage
import com.thehouse.app.domain.repository.ConversationRepository
import com.thehouse.app.testutil.TestMainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

private data class RecordedMessagesCall(val after: String?, val etag: String?)

private class FakeConversationRepository : ConversationRepository {

    val messageResults = ArrayDeque<Result<MessagesFetch>>()
    val recordedCalls = mutableListOf<RecordedMessagesCall>()

    var conversationClosedFailure = false
    val sentCalls = mutableListOf<Pair<String, String>>()
    var sendResult: Result<MessageSent> = Result.success(
        MessageSent(
            message = ChatMessage("m-new", "student", "enviado", null, true),
            replayed = false,
        ),
    )
    val readCalls = mutableListOf<String>()

    override suspend fun getConversations(page: Int, pageSize: Int) = Result.success(
        listOf(
            Conversation(
                id = "conv-1",
                requestId = "req-1",
                room = ConversationRoom("room-1", "Cuarto"),
                counterpart = ConversationCounterpart("Arrendador", "landlord"),
                status = ConversationStatus.OPEN,
                lastMessageAt = null,
                unreadCount = 1,
            ),
        ),
    )

    override suspend fun getMessages(
        conversationId: String,
        after: String?,
        etag: String?,
    ): Result<MessagesFetch> {
        recordedCalls.add(RecordedMessagesCall(after, etag))
        val result = messageResults.removeFirstOrNull()
            ?: Result.success(MessagesFetch.NotModified)
        return result
    }

    override suspend fun sendMessage(
        conversationId: String,
        text: String,
        clientMessageId: String,
    ): Result<MessageSent> {
        sentCalls.add(text to clientMessageId)
        if (conversationClosedFailure) {
            return Result.failure(
                com.thehouse.app.domain.error.ChatException(
                    com.thehouse.app.domain.error.DomainError.Conflict,
                    com.thehouse.app.domain.error.ChatErrorCode.CONVERSATION_CLOSED,
                ),
            )
        }
        return sendResult
    }

    override suspend fun markRead(conversationId: String, lastMessageId: String): Result<Boolean> {
        readCalls.add(lastMessageId)
        return Result.success(true)
    }
}

private val initialMessages = MessagesFetch.Modified(
    page = MessagesPage(
        items = listOf(
            ChatMessage("m-1", "student", "Hola", null, true),
            ChatMessage("m-2", "landlord", "Hola!", null, false),
        ),
        hasMore = false,
        pollAfterSeconds = 10,
    ),
    etag = "\"v1\"",
)

private fun newMessage(id: String, mine: Boolean = false) = MessagesFetch.Modified(
    page = MessagesPage(
        items = listOf(ChatMessage(id, "landlord", "nuevo", null, mine)),
        hasMore = false,
        pollAfterSeconds = 10,
    ),
    etag = "\"v2\"",
)

@OptIn(ExperimentalCoroutinesApi::class)
class ConversationViewModelTest {

    @get:Rule
    val mainDispatcherRule = TestMainDispatcherRule(StandardTestDispatcher())

    private fun createViewModel(
        repository: FakeConversationRepository,
        pollIntervalOverrideMs: Long? = 50L,
    ) = ConversationViewModel(
        conversationId = "conv-1",
        getMessages = com.thehouse.app.domain.usecase.GetMessagesUseCase(repository),
        sendMessage = com.thehouse.app.domain.usecase.SendMessageUseCase(repository),
        markRead = com.thehouse.app.domain.usecase.MarkConversationReadUseCase(repository),
        pollIntervalOverrideMs = pollIntervalOverrideMs,
    )

    @Test
    fun `initial load fetches history and polls with after and etag`() = runTest {
        val repository = FakeConversationRepository().apply {
            messageResults.add(Result.success(initialMessages))
            messageResults.add(Result.success(newMessage("m-3")))
        }
        val vm = createViewModel(repository)
        vm.onEvent(ConversationEvent.Load)
        runCurrent()
        assertEquals(2, vm.uiState.value.messages.size)

        // Primer poll posterior: usa after del último mensaje + If-None-Match.
        advanceTimeBy(50)
        runCurrent()
        val secondCall = repository.recordedCalls.getOrNull(1)
        assertEquals("m-2", secondCall?.after)
        assertEquals("\"v1\"", secondCall?.etag)
        vm.onEvent(ConversationEvent.StopPolling)
        assertEquals(3, vm.uiState.value.messages.size)
    }

    @Test
    fun `304 does not duplicate messages`() = runTest {
        val repository = FakeConversationRepository().apply {
            messageResults.add(Result.success(initialMessages))
            messageResults.add(Result.success(MessagesFetch.NotModified))
        }
        val vm = createViewModel(repository)
        vm.onEvent(ConversationEvent.Load)
        runCurrent()
        advanceTimeBy(50)
        runCurrent()
        vm.onEvent(ConversationEvent.StopPolling)
                assertEquals(2, vm.uiState.value.messages.size)
    }

    @Test
    fun `respects pollAfterSeconds from backend`() = runTest {
        val repository = FakeConversationRepository().apply {
            messageResults.add(
                Result.success(
                    MessagesFetch.Modified(
                        page = MessagesPage(listOf(ChatMessage("m-1", "student", "Hola", null, true)), false, 2),
                        etag = "\"v1\"",
                    ),
                ),
            )
            messageResults.add(Result.success(newMessage("m-2")))
        }
        val vm = createViewModel(repository, pollIntervalOverrideMs = null)
        vm.onEvent(ConversationEvent.Load)
        runCurrent()
        assertEquals(1, repository.recordedCalls.size)
        // pollAfterSeconds=2 → 2000 ms: antes de eso no hay poll nuevo.
        advanceTimeBy(1000)
        runCurrent()
        assertEquals(1, repository.recordedCalls.size)
        advanceTimeBy(1000)
        runCurrent()
        vm.onEvent(ConversationEvent.StopPolling)
                assertEquals(2, repository.recordedCalls.size)
    }

    @Test
    fun `retry preserves same clientMessageId and success clears it`() = runTest {
        val repository = FakeConversationRepository().apply {
            messageResults.add(Result.success(initialMessages))
            sendResult = Result.failure(
                com.thehouse.app.domain.error.ChatException(
                    com.thehouse.app.domain.error.DomainError.Server,
                ),
            )
        }
        val vm = createViewModel(repository)
        vm.onEvent(ConversationEvent.Load)
        runCurrent()
        vm.onEvent(ConversationEvent.ComposerChanged("¿Cuánto es el depósito?"))
        vm.onEvent(ConversationEvent.Send)
        runCurrent()
        val firstId = repository.sentCalls.single().second
        assertFalse(vm.uiState.value.isSending)
        assertEquals(firstId, vm.uiState.value.pendingClientMessageId)

        // Retry del mismo mensaje → mismo UUID.
        repository.sendResult = Result.success(
            MessageSent(
                message = ChatMessage("m-new", "student", "¿Cuánto es el depósito?", null, true),
                replayed = false,
            ),
        )
        vm.onEvent(ConversationEvent.Send)
        runCurrent()
        val secondId = repository.sentCalls.last().second
        assertEquals(firstId, secondId)
        assertNull(vm.uiState.value.pendingClientMessageId)
        assertEquals("", vm.uiState.value.composerText)
        assertTrue(vm.uiState.value.messages.any { it.id == "m-new" })

        // Acción nueva → UUID nuevo.
        vm.onEvent(ConversationEvent.ComposerChanged("Otro mensaje"))
        vm.onEvent(ConversationEvent.Send)
        runCurrent()
        val thirdId = repository.sentCalls.last().second
        vm.onEvent(ConversationEvent.StopPolling)
                assertNotEquals(firstId, thirdId)
    }

    @Test
    fun `double submit is blocked while sending`() = runTest {
        val repository = FakeConversationRepository().apply {
            messageResults.add(Result.success(initialMessages))
        }
        val vm = createViewModel(repository)
        vm.onEvent(ConversationEvent.Load)
        runCurrent()
        vm.onEvent(ConversationEvent.ComposerChanged("Mensaje"))
        vm.onEvent(ConversationEvent.Send)
        // Sin runCurrent: el primer envío sigue en vuelo.
        vm.onEvent(ConversationEvent.Send)
        runCurrent()
        vm.onEvent(ConversationEvent.StopPolling)
                assertEquals(1, repository.sentCalls.size)
    }

    @Test
    fun `conversation_closed disables composer`() = runTest {
        val repository = FakeConversationRepository().apply {
            messageResults.add(Result.success(initialMessages))
            conversationClosedFailure = true
        }
        val vm = createViewModel(repository)
        vm.onEvent(ConversationEvent.Load)
        runCurrent()
        vm.onEvent(ConversationEvent.ComposerChanged("Hola"))
        vm.onEvent(ConversationEvent.Send)
        runCurrent()
        assertTrue(vm.uiState.value.isClosed)
        assertEquals("Esta conversación está cerrada.", vm.uiState.value.errorMessage)
        vm.onEvent(ConversationEvent.ComposerChanged("Otro"))
        vm.onEvent(ConversationEvent.Send)
        runCurrent()
        // El envío está bloqueado: la conversación está cerrada.
        vm.onEvent(ConversationEvent.StopPolling)
                assertEquals(1, repository.sentCalls.size)
    }

    @Test
    fun `read is called after successful message fetch`() = runTest {
        val repository = FakeConversationRepository().apply {
            messageResults.add(Result.success(initialMessages))
        }
        val vm = createViewModel(repository)
        vm.onEvent(ConversationEvent.Load)
        runCurrent()
        vm.onEvent(ConversationEvent.StopPolling)
                assertEquals(listOf("m-2"), repository.readCalls)
    }

    @Test
    fun `polling stops when screen becomes inactive`() = runTest {
        val repository = FakeConversationRepository().apply {
            messageResults.add(Result.success(initialMessages))
        }
        val vm = createViewModel(repository)
        vm.onEvent(ConversationEvent.Load)
        runCurrent()
        val callsAfterLoad = repository.recordedCalls.size
        vm.onEvent(ConversationEvent.StopPolling)
        runCurrent()
        advanceTimeBy(10_000)
        runCurrent()
        assertEquals(callsAfterLoad, repository.recordedCalls.size)
    }
}

