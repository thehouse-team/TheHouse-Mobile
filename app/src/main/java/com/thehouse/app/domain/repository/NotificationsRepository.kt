package com.thehouse.app.domain.repository

import com.thehouse.app.domain.model.NotificationCategory
import com.thehouse.app.domain.model.NotificationsPage
import com.thehouse.app.domain.model.UnreadCountFetch

interface NotificationsRepository {

    suspend fun getNotifications(
        unread: Boolean?,
        category: NotificationCategory?,
        page: Int,
        pageSize: Int,
    ): Result<NotificationsPage>

    /**
     * Unread count con soporte ETag.
     * - [etag]: último ETag conocido; se envía como If-None-Match.
     * - 304 → [UnreadCountFetch.NotModified] (NO modificar estado).
     */
    suspend fun getUnreadCount(etag: String?): Result<UnreadCountFetch>

    /** Marca una notificación como leída. 204 → éxito. 404 → inexistente/ajena. */
    suspend fun markRead(id: String): Result<Unit>

    /** Marca todas como leídas. [category] opcional (requests|visits|system). 204 → éxito. */
    suspend fun markAllRead(category: NotificationCategory?): Result<Unit>
}
