package com.thehouse.app.data.notifications.push

import com.thehouse.app.domain.model.NotificationCategory

/**
 * IDs estables de canales. No crear canales fuera de estas tres categorías.
 */
object PushChannels {
    const val REQUESTS_ID = "thehouse_requests"
    const val VISITS_ID = "thehouse_visits"
    const val SYSTEM_ID = "thehouse_system"

    fun idFor(category: NotificationCategory?): String = when (category) {
        NotificationCategory.REQUESTS -> REQUESTS_ID
        NotificationCategory.VISITS -> VISITS_ID
        NotificationCategory.SYSTEM, null -> SYSTEM_ID
    }
}
