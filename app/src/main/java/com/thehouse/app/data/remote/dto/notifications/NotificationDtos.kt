package com.thehouse.app.data.remote.dto.notifications

import kotlinx.serialization.Serializable

@Serializable
data class NotificationTargetDto(
    val kind: String? = null,
    val id: String? = null,
)

@Serializable
data class NotificationItemDto(
    val id: String,
    val type: String = "",
    val category: String? = null,
    val title: String = "",
    val body: String = "",
    val target: NotificationTargetDto? = null,
    val count: Int = 0,
    val read: Boolean = false,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

@Serializable
data class NotificationsListResponseDto(
    val items: List<NotificationItemDto> = emptyList(),
    val page: Int = 1,
    val pageSize: Int = 20,
    val totalItems: Int = 0,
    val totalPages: Int = 0,
)

@Serializable
data class UnreadCountResponseDto(
    val unread: Int = 0,
    val requests: Int = 0,
    val visits: Int = 0,
    val system: Int = 0,
)

@Serializable
data class ReadAllNotificationsDto(
    val category: String? = null,
)
