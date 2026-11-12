package com.thehouse.app.domain.usecase

import com.thehouse.app.domain.model.Conversation
import com.thehouse.app.domain.model.MessageSent
import com.thehouse.app.domain.model.MessagesFetch
import com.thehouse.app.domain.repository.ConversationRepository

class GetConversationsUseCase(private val repository: ConversationRepository) {
    suspend operator fun invoke(page: Int = 1, pageSize: Int = 20): Result<List<Conversation>> =
        repository.getConversations(page, pageSize)
}

class GetMessagesUseCase(private val repository: ConversationRepository) {
    suspend operator fun invoke(
        conversationId: String,
        after: String? = null,
        etag: String? = null,
    ): Result<MessagesFetch> = repository.getMessages(conversationId, after, etag)
}

class SendMessageUseCase(private val repository: ConversationRepository) {
    suspend operator fun invoke(
        conversationId: String,
        text: String,
        clientMessageId: String,
    ): Result<MessageSent> = repository.sendMessage(conversationId, text, clientMessageId)
}

class MarkConversationReadUseCase(private val repository: ConversationRepository) {
    suspend operator fun invoke(conversationId: String, lastMessageId: String): Result<Boolean> =
        repository.markRead(conversationId, lastMessageId)
}
