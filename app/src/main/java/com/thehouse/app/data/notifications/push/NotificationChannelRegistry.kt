package com.thehouse.app.data.notifications.push

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

/**
 * Registra los canales de notificación de NV-14. Sin sonidos personalizados.
 * Operación idempotente: Android conserva la configuración elegida por el usuario.
 */
class NotificationChannelRegistry(private val context: Context) {

    fun ensureChannels() {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            channel(
                id = PushChannels.REQUESTS_ID,
                name = "Solicitudes",
                description = "Solicitudes de alquiler y respuestas del arrendador.",
                importance = NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
        manager.createNotificationChannel(
            channel(
                id = PushChannels.VISITS_ID,
                name = "Visitas",
                description = "Confirmaciones, recordatorios y cambios de visitas.",
                importance = NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
        manager.createNotificationChannel(
            channel(
                id = PushChannels.SYSTEM_ID,
                name = "Sistema",
                description = "Avisos generales de tu cuenta y de TheHouse.",
                importance = NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
    }

    private fun channel(
        id: String,
        name: String,
        description: String,
        importance: Int,
    ): NotificationChannel = NotificationChannel(id, name, importance).apply {
        this.description = description
        setShowBadge(true)
        lockscreenVisibility = Notification.VISIBILITY_PRIVATE
    }
}
