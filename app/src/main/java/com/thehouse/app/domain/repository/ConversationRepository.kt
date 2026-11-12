package com.thehouse.app.domain.repository

import com.thehouse.app.domain.model.Conversation
import com.thehouse.app.domain.model.MessageSent
import com.thehouse.app.domain.model.MessagesFetch

interface ConversationRepository {

    suspend fun getConversations(page: Int, pageSize: Int): Result<List<Conversation>>

    /**
     * Mensajes de una conversación.
     *
     * - [after]: id del último mensaje conocido (incremental).
     * - [etag]: último ETag conocido; se envía como If-None-Match.
     * - 304 → [com.thehouse.app.domain.model.MessagesFetch.NotModified].
     */
    suspend fun getMessages(
        conversationId: String,
        after: String?,
        etag: String?,
    ): Result<MessagesFetch>

    /** Envío idempotente: reenviar el mismo [clientMessageId] es un replay (200). */
    suspend fun sendMessage(
        conversationId: String,
        text: String,
        clientMessageId: String,
    ): Result<MessageSent>

    /** Marca como leído hasta [lastMessageId]. 204 → true. */
    suspend fun markRead(conversationId: String, lastMessageId: String): Result<Boolean>
}
