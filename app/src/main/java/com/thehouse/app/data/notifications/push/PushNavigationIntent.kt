package com.thehouse.app.data.notifications.push

import android.content.Intent
import com.thehouse.app.domain.push.NotificationDestination
import com.thehouse.app.domain.push.NotificationDestinationResolver
import com.thehouse.app.domain.push.NotificationTargetResolver
import com.thehouse.app.domain.push.PushLogger

sealed interface PushNavigationIntentResult {
    /** Apertura normal de la app: no hay target. */
    data object Ignore : PushNavigationIntentResult

    /** Target desconocido/inválido: Notification Center seguro. */
    data object Fallback : PushNavigationIntentResult

    data class Navigate(val destination: NotificationDestination) : PushNavigationIntentResult
}

/**
 * Parser puro de extras de navegación. Valida con las mismas reglas que FCM.
 */
object PushNavigationIntentParser {

    fun parse(kind: String?, id: String?): PushNavigationIntentResult {
        if (kind.isNullOrBlank() && id.isNullOrBlank()) return PushNavigationIntentResult.Ignore
        if (kind.isNullOrBlank()) return PushNavigationIntentResult.Fallback
        val target = NotificationTargetResolver.resolve(kind, id)
            ?: return PushNavigationIntentResult.Fallback
        return PushNavigationIntentResult.Navigate(
            NotificationDestinationResolver.fromTarget(target),
        )
    }
}

/** Extrae extras del Intent del launcher y publica el destino pendiente. */
class AndroidPushNavigationIntentHandler(
    private val pendingNavigation: PendingNotificationNavigation,
    private val logger: PushLogger,
) {

    fun handle(intent: Intent?) {
        if (intent == null) return
        val kind = intent.getStringExtra(PushIntentExtras.TARGET_KIND)
        val id = intent.getStringExtra(PushIntentExtras.TARGET_ID)
        when (val result = PushNavigationIntentParser.parse(kind, id)) {
            PushNavigationIntentResult.Ignore -> Unit
            PushNavigationIntentResult.Fallback -> {
                logger.warn("push_navigation_fallback")
                pendingNavigation.offer(NotificationDestination.NotificationCenter)
            }

            is PushNavigationIntentResult.Navigate -> {
                pendingNavigation.offer(result.destination)
            }
        }
    }
}
