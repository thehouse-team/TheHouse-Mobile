package com.thehouse.app.data.remote.dto.chat

import kotlinx.serialization.Serializable

@Serializable
data class ConversationRoomDto(
    val id: String,
    val title: String,
)

@Serializable
data class ConversationCounterpartDto(
    val name: String,
    val role: String,
)

@Serializable
data class ConversationDto(
    val id: String,
    val requestId: String,
    val room: ConversationRoomDto,
    val counterpart: ConversationCounterpartDto,
    val status: String,
    val lastMessageAt: String? = null,
    val unreadCount: Int = 0,
)

@Serializable
data class ConversationsListResponseDto(
    val items: List<ConversationDto> = emptyList(),
    val page: Int = 1,
    val pageSize: Int = 20,
    val totalItems: Long = 0,
    val totalPages: Int = 0,
)

@Serializable
data class ChatMessageDto(
    val id: String,
    val senderRole: String,
    val text: String,
    val createdAt: String? = null,
    val mine: Boolean,
)

@Serializable
data class MessagesResponseDto(
    val items: List<ChatMessageDto> = emptyList(),
    val hasMore: Boolean = false,
    @kotlinx.serialization.SerialName("pollAfterSeconds") val pollAfterSeconds: Long = 10,
)

@Serializable
data class SendMessageDto(
    val text: String,
    val clientMessageId: String,
)

@Serializable
data class MarkReadDto(
    val lastMessageId: String,
)
