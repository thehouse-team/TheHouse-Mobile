package com.thehouse.app.data.notifications.push

import com.thehouse.app.domain.push.NotificationDestination
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Destino pendiente de una notificación tocada.
 *
 * Se conserva mientras la sesión se restaura o el usuario pasa por Login;
 * se consume solo después de navegar para no perder el target.
 */
class PendingNotificationNavigation {

    private val _destination = MutableStateFlow<NotificationDestination?>(null)
    val destination: StateFlow<NotificationDestination?> = _destination.asStateFlow()

    fun offer(destination: NotificationDestination) {
        _destination.value = destination
    }

    fun consume() {
        _destination.value = null
    }

    fun clear() {
        _destination.value = null
    }
}
