package com.thehouse.app.data.notifications.push

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.thehouse.app.R
import com.thehouse.app.domain.model.NotificationCategory
import com.thehouse.app.domain.push.LocalPushNotifier
import com.thehouse.app.domain.push.PushLogger
import com.thehouse.app.domain.push.PushPayload

/** Claves de navegación seguras que viajan en el PendingIntent local. */
object PushIntentExtras {
    const val TARGET_KIND = "com.thehouse.push.TARGET_KIND"
    const val TARGET_ID = "com.thehouse.push.TARGET_ID"
    const val NOTIFICATION_ID = "com.thehouse.push.NOTIFICATION_ID"
}

/**
 * Muestra notificaciones locales de pushes recibidos en background.
 *
 * - Reutiliza el launcher existente (sin acoplar Data a MainActivity).
 * - Visibilidad privada + versión pública sin contenido sensible en lockscreen.
 * - Un id estable por evento evita notificaciones duplicadas del mismo push.
 */
class AndroidLocalPushNotifier(
    private val context: Context,
    private val channels: NotificationChannelRegistry,
    private val logger: PushLogger,
) : LocalPushNotifier {

    override fun show(payload: PushPayload) {
        if (!canPostNotifications()) {
            logger.info("push_notification_permission_missing")
            return
        }
        channels.ensureChannels()

        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        if (launchIntent == null) {
            logger.warn("push_notification_launch_intent_missing")
            return
        }
        launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
            Intent.FLAG_ACTIVITY_CLEAR_TOP or
            Intent.FLAG_ACTIVITY_SINGLE_TOP
        launchIntent.putExtra(PushIntentExtras.TARGET_KIND, payload.target?.kind?.api.orEmpty())
        launchIntent.putExtra(PushIntentExtras.TARGET_ID, payload.target?.id.orEmpty())
        launchIntent.putExtra(PushIntentExtras.NOTIFICATION_ID, payload.notificationId)

        val requestCode = stableId(payload.notificationId)
        val contentIntent = PendingIntent.getActivity(
            context,
            requestCode,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val channelId = PushChannels.idFor(payload.category)

        val publicVersion = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("TheHouse")
            .setContentText("Tienes una nueva notificación.")
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(payload.title)
            .setCategory(categoryFor(payload.category))
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicVersion)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setOnlyAlertOnce(false)
            .apply {
                if (payload.body.isNotBlank()) {
                    setContentText(payload.body)
                    setStyle(NotificationCompat.BigTextStyle().bigText(payload.body))
                }
            }

        // Lint MissingPermission exige comprobar el permiso inmediatamente antes
        // de notify(); el guard de show() no es visible para el análisis estático.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            logger.info("push_notification_permission_missing")
            return
        }
        try {
            NotificationManagerCompat.from(context).notify(requestCode, builder.build())
        } catch (error: SecurityException) {
            // Permiso revocado entre la comprobación y la publicación.
            logger.warn("push_notification_permission_revoked", error.javaClass.simpleName)
        }
    }

    override fun cancelAll() {
        NotificationManagerCompat.from(context).cancelAll()
    }

    private fun canPostNotifications(): Boolean {
        val runtimeGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        return runtimeGranted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    private fun categoryFor(category: NotificationCategory?): String = when (category) {
        NotificationCategory.REQUESTS -> NotificationCompat.CATEGORY_MESSAGE
        NotificationCategory.VISITS -> NotificationCompat.CATEGORY_EVENT
        NotificationCategory.SYSTEM, null -> NotificationCompat.CATEGORY_STATUS
    }

    private fun stableId(notificationId: String): Int =
        notificationId.hashCode() and 0x7FFF_FFFF
}
