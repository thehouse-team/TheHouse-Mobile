package com.thehouse.app.domain.model

import java.time.Instant

enum class ConversationStatus {
    OPEN,
    CLOSED;

    companion object {
        fun fromApi(value: String): ConversationStatus =
            if (value.lowercase() == "closed") CLOSED else OPEN
    }
}

data class ConversationRoom(
    val id: String,
    val title: String,
)

data class ConversationCounterpart(
    val name: String,
    val role: String,
)

data class Conversation(
    val id: String,
    val requestId: String,
    val room: ConversationRoom,
    val counterpart: ConversationCounterpart,
    val status: ConversationStatus,
    val lastMessageAt: Instant?,
    val unreadCount: Int,
)

data class ChatMessage(
    val id: String,
    val senderRole: String,
    val text: String,
    val createdAt: Instant?,
    val mine: Boolean,
)

data class MessagesPage(
    val items: List<ChatMessage>,
    val hasMore: Boolean,
    /** Segundos que el backend pide esperar antes del siguiente poll (default 10). */
    val pollAfterSeconds: Long,
)

/** Resultado de un fetch de mensajes con ETag: puede ser 304 Not Modified. */
sealed interface MessagesFetch {
    data class Modified(val page: MessagesPage, val etag: String?) : MessagesFetch
    data object NotModified : MessagesFetch
}

data class MessageSent(
    val message: ChatMessage,
    /** true cuando el backend respondió 200 (replay del mismo clientMessageId). */
    val replayed: Boolean,
)
