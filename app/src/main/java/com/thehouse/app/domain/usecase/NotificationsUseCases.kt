package com.thehouse.app.domain.usecase

import com.thehouse.app.domain.model.NotificationCategory
import com.thehouse.app.domain.model.NotificationsPage
import com.thehouse.app.domain.model.UnreadCountFetch
import com.thehouse.app.domain.repository.NotificationsRepository

class GetNotificationsUseCase(private val repository: NotificationsRepository) {
    suspend operator fun invoke(
        unread: Boolean? = null,
        category: NotificationCategory? = null,
        page: Int = 1,
        pageSize: Int = 20,
    ): Result<NotificationsPage> = repository.getNotifications(unread, category, page, pageSize)
}

class GetUnreadNotificationsCountUseCase(private val repository: NotificationsRepository) {
    suspend operator fun invoke(etag: String? = null): Result<UnreadCountFetch> =
        repository.getUnreadCount(etag)
}

class MarkNotificationReadUseCase(private val repository: NotificationsRepository) {
    suspend operator fun invoke(id: String): Result<Unit> = repository.markRead(id)
}

class MarkAllNotificationsReadUseCase(private val repository: NotificationsRepository) {
    suspend operator fun invoke(category: NotificationCategory? = null): Result<Unit> =
        repository.markAllRead(category)
}
