package com.thehouse.app.data.notifications.push

import com.thehouse.app.domain.push.AppVisibility
import com.thehouse.app.domain.push.LocalPushNotifier
import com.thehouse.app.domain.push.PushLogger
import com.thehouse.app.domain.push.PushPayloadParseResult
import com.thehouse.app.domain.push.PushPayloadParser
import com.thehouse.app.domain.push.PushSessionGate
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * Procesa un payload FCM ya recibido.
 *
 * Fase A sigue siendo la fuente del Notification Center. FCM actúa como señal:
 * - Foreground: NO notificación de sistema (evita duplicar la in-app);
 *   solo se emite refresco para que Home/Notifications consulten al Backend.
 * - Background: notificación local con navegación validada.
 * - Sin sesión: se descarta para no exponer datos privados de otra sesión.
 */
class PushMessageHandler(
    private val parser: PushPayloadParser,
    private val sessionGate: PushSessionGate,
    private val visibility: AppVisibility,
    private val localNotifier: LocalPushNotifier,
    private val refreshSignals: MutableSharedFlow<Unit>,
    private val logger: PushLogger,
) {

    suspend fun handle(data: Map<String, String>) {
        when (val parsed = parser.parse(data)) {
            is PushPayloadParseResult.Invalid -> {
                // Solo reason code; nunca claves/valores del payload.
                logger.warn("push_payload_rejected", parsed.rejection.name)
                return
            }

            is PushPayloadParseResult.Valid -> {
                parsed.warnings.forEach { warning ->
                    logger.warn("push_payload_warning", warning.name)
                }

                if (!sessionGate.hasActiveSession()) {
                    logger.warn("push_suppressed_no_session")
                    return
                }

                if (visibility.isForeground()) {
                    logger.info("push_foreground_refresh")
                    refreshSignals.tryEmit(Unit)
                    return
                }

                try {
                    localNotifier.show(parsed.payload)
                    logger.info("push_background_notified")
                } catch (error: Throwable) {
                    logger.warn("push_notification_failed")
                } finally {
                    refreshSignals.tryEmit(Unit)
                }
            }
        }
    }
}
