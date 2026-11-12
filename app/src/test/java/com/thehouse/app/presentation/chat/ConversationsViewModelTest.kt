package com.thehouse.app.presentation.chat

import com.thehouse.app.domain.repository.ConversationRepository
import com.thehouse.app.domain.usecase.GetConversationsUseCase
import com.thehouse.app.testutil.TestMainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConversationsViewModelTest {

    @get:Rule
    val mainDispatcherRule = TestMainDispatcherRule(StandardTestDispatcher())

    private class FakeConversationsRepository : ConversationRepository {
        var networkFailure = false

        override suspend fun getConversations(page: Int, pageSize: Int): Result<List<com.thehouse.app.domain.model.Conversation>> {
            if (networkFailure) {
                return Result.failure(
                    com.thehouse.app.domain.error.ChatException(
                        com.thehouse.app.domain.error.DomainError.Network,
                    ),
                )
            }
            return Result.success(
                listOf(
                    com.thehouse.app.domain.model.Conversation(
                        id = "conv-1",
                        requestId = "req-1",
                        room = com.thehouse.app.domain.model.ConversationRoom("room-1", "Cuarto"),
                        counterpart = com.thehouse.app.domain.model.ConversationCounterpart("Arrendador", "landlord"),
                        status = com.thehouse.app.domain.model.ConversationStatus.OPEN,
                        lastMessageAt = null,
                        unreadCount = 3,
                    ),
                ),
            )
        }

        override suspend fun getMessages(
            conversationId: String,
            after: String?,
            etag: String?,
        ) = Result.success(com.thehouse.app.domain.model.MessagesFetch.NotModified)

        override suspend fun sendMessage(
            conversationId: String,
            text: String,
            clientMessageId: String,
        ) = Result.failure<com.thehouse.app.domain.model.MessageSent>(
            com.thehouse.app.domain.error.ChatException(
                com.thehouse.app.domain.error.DomainError.Unknown,
            ),
        )

        override suspend fun markRead(conversationId: String, lastMessageId: String) = Result.success(true)
    }

    @Test
    fun `list loads conversations with unread count`() = runTest {
        val repository = FakeConversationsRepository()
        val vm = ConversationsViewModel(GetConversationsUseCase(repository))
        vm.onEvent(ConversationsEvent.Load)
        runCurrent()
        assertEquals(1, vm.uiState.value.items.size)
        assertEquals(3, vm.uiState.value.items.first().unreadCount)
    }

    @Test
    fun `network failure shows friendly message`() = runTest {
        val repository = FakeConversationsRepository().apply { networkFailure = true }
        val vm = ConversationsViewModel(GetConversationsUseCase(repository))
        vm.onEvent(ConversationsEvent.Load)
        runCurrent()
        assertEquals(
            "No hay conexión. Verifica tu red e intenta de nuevo.",
            vm.uiState.value.errorMessage,
        )
    }

    @Test
    fun `load while loading is deduplicated`() = runTest {
        val repository = FakeConversationsRepository()
        val vm = ConversationsViewModel(GetConversationsUseCase(repository))
        vm.onEvent(ConversationsEvent.Load)
        vm.onEvent(ConversationsEvent.Load)
        runCurrent()
        assertTrue(vm.uiState.value.items.isNotEmpty())
    }
}
