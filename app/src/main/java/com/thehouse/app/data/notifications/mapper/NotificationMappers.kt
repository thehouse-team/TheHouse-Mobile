package com.thehouse.app.data.notifications.mapper

import com.thehouse.app.data.remote.dto.notifications.NotificationItemDto
import com.thehouse.app.data.remote.dto.notifications.NotificationTargetDto
import com.thehouse.app.data.remote.dto.notifications.NotificationsListResponseDto
import com.thehouse.app.data.remote.dto.notifications.UnreadCountResponseDto
import com.thehouse.app.data.requests.mapper.toInstantOrNull
import com.thehouse.app.domain.model.AppNotification
import com.thehouse.app.domain.model.NotificationCategory
import com.thehouse.app.domain.model.NotificationTarget
import com.thehouse.app.domain.model.NotificationTargetKind
import com.thehouse.app.domain.model.NotificationsPage
import com.thehouse.app.domain.model.UnreadCount

fun NotificationItemDto.toDomain(): AppNotification = AppNotification(
    id = id,
    type = type,
    category = NotificationCategory.fromApi(category),
    title = title,
    body = body,
    target = target?.toDomain(),
    count = count,
    read = read,
    createdAt = createdAt?.toInstantOrNull(),
    updatedAt = updatedAt?.toInstantOrNull(),
)

fun NotificationTargetDto.toDomain(): NotificationTarget? {
    val kind = NotificationTargetKind.fromApi(kind)
    val targetId = id?.takeIf { it.isNotBlank() } ?: return null
    return NotificationTarget(kind = kind, id = targetId)
}

fun NotificationsListResponseDto.toDomain(): NotificationsPage = NotificationsPage(
    items = items.map { it.toDomain() },
    page = page,
    pageSize = pageSize,
    totalItems = totalItems,
    totalPages = totalPages,
)

fun UnreadCountResponseDto.toDomain(): UnreadCount = UnreadCount(
    unread = unread,
    requests = requests,
    visits = visits,
    system = system,
)
