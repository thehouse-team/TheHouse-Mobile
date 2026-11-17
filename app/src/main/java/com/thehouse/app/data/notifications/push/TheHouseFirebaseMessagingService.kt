package com.thehouse.app.data.notifications.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.thehouse.app.TheHouseApplication
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * Servicio FCM de NV-14 Fase B.
 *
 * Solo procesa mensajes **data-only**. Un mensaje con bloque `notification`
 * puede ser mostrado automáticamente por el SDK en background; el sender debe
 * usar data-only para que la app aplique validación y navegación propias.
 */
class TheHouseFirebaseMessagingService : FirebaseMessagingService() {

    private val container
        get() = (application as TheHouseApplication).container

    @Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
    override fun onNewToken(token: String) {
        // PushTokenCoordinator valida y nunca loguea el token completo.
        container.pushTokenCoordinator.onNewToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        if (data.isEmpty()) {
            container.pushLogger.warn("push_empty_payload")
            return
        }

        container.applicationScope.launch {
            try {
                container.pushMessageHandler.handle(data)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                // Sin mensaje de excepción: podría contener datos del payload.
                container.pushLogger.warn("push_handle_failed")
            }
        }
    }
}
