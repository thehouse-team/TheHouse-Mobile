package com.thehouse.app.domain.model

import java.time.Instant

enum class NotificationCategory(val api: String) {
    REQUESTS("requests"),
    VISITS("visits"),
    SYSTEM("system");

    companion object {
        fun fromApi(value: String?): NotificationCategory? =
            entries.firstOrNull { it.api.equals(value, ignoreCase = true) }
    }
}

enum class NotificationTargetKind(val api: String) {
    REQUEST("request"),
    CONVERSATION("conversation"),
    VISIT("visit"),
    AGREEMENT("agreement"),
    RENTAL("rental"),
    BUILDING("building"),
    UNKNOWN("unknown");

    companion object {
        fun fromApi(value: String?): NotificationTargetKind =
            entries.firstOrNull { it.api.equals(value, ignoreCase = true) } ?: UNKNOWN
    }
}

data class NotificationTarget(
    val kind: NotificationTargetKind,
    val id: String,
)

data class AppNotification(
    val id: String,
    val type: String,
    val category: NotificationCategory?,
    val title: String,
    val body: String,
    val target: NotificationTarget?,
    val count: Int,
    val read: Boolean,
    val createdAt: Instant?,
    val updatedAt: Instant?,
)

data class NotificationsPage(
    val items: List<AppNotification>,
    val page: Int,
    val pageSize: Int,
    val totalItems: Int,
    val totalPages: Int,
)

data class UnreadCount(
    val unread: Int,
    val requests: Int,
    val visits: Int,
    val system: Int,
)

sealed interface UnreadCountFetch {
    data class Modified(val count: UnreadCount, val etag: String?) : UnreadCountFetch
    data object NotModified : UnreadCountFetch
}
