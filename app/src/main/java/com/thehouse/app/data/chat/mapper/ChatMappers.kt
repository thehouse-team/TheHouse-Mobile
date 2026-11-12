package com.thehouse.app.data.chat.mapper

import com.thehouse.app.data.remote.dto.chat.ChatMessageDto
import com.thehouse.app.data.remote.dto.chat.ConversationDto
import com.thehouse.app.domain.model.ChatMessage
import com.thehouse.app.domain.model.Conversation
import com.thehouse.app.domain.model.ConversationCounterpart
import com.thehouse.app.domain.model.ConversationRoom
import com.thehouse.app.domain.model.ConversationStatus
import com.thehouse.app.domain.model.MessagesPage
import com.thehouse.app.data.remote.dto.chat.MessagesResponseDto
import com.thehouse.app.data.requests.mapper.toInstantOrNull
import java.time.Instant

fun ConversationDto.toDomain(): Conversation = Conversation(
    id = id,
    requestId = requestId,
    room = ConversationRoom(id = room.id, title = room.title),
    counterpart = ConversationCounterpart(name = counterpart.name, role = counterpart.role),
    status = ConversationStatus.fromApi(status),
    lastMessageAt = lastMessageAt?.toInstantOrNull(),
    unreadCount = unreadCount,
)

fun ChatMessageDto.toDomain(): ChatMessage = ChatMessage(
    id = id,
    senderRole = senderRole,
    text = text,
    createdAt = createdAt?.toInstantOrNull(),
    mine = mine,
)

fun MessagesResponseDto.toDomain(): MessagesPage = MessagesPage(
    items = items.map { it.toDomain() },
    hasMore = hasMore,
    pollAfterSeconds = pollAfterSeconds.coerceAtLeast(1),
)
