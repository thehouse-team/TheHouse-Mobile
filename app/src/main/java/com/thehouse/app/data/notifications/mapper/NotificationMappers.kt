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
    // NV-20 v1.3.0: `none` no exige targetId; se conserva el envelope con id
    // vacío y el resolver global decide abrir la bandeja. Para el resto, un id
    // ausente/inválido también degrada a bandeja en el resolver (sin petición).
    val targetId = id?.trim().orEmpty()
    if (targetId.isEmpty() && kind != NotificationTargetKind.NONE) {
        return NotificationTarget(kind = kind, id = "")
    }
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
