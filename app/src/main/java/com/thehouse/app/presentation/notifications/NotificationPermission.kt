package com.thehouse.app.presentation.notifications

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

enum class NotificationPermissionStatus {
    GRANTED,
    /** Puede solicitarse de nuevo (primera vez o con rationale). */
    DENIED,
    /** Rechazada definitivamente: solo Ajustes del sistema. */
    PERMANENTLY_DENIED,
}

/** Lógica pura de permiso para tests JVM (sin Android runtime). */
object NotificationPermissionPolicy {

    const val RUNTIME_PERMISSION_SDK_INT = 33

    fun runtimePermissionRequired(sdkInt: Int): Boolean =
        sdkInt >= RUNTIME_PERMISSION_SDK_INT

    fun canPostNotifications(sdkInt: Int, runtimeGranted: Boolean): Boolean =
        !runtimePermissionRequired(sdkInt) || runtimeGranted

    fun resolveStatus(
        sdkInt: Int,
        runtimeGranted: Boolean,
        askedOnce: Boolean,
        showRationale: Boolean,
    ): NotificationPermissionStatus {
        if (!runtimePermissionRequired(sdkInt) || runtimeGranted) {
            return NotificationPermissionStatus.GRANTED
        }
        if (!askedOnce) return NotificationPermissionStatus.DENIED
        return if (showRationale) {
            NotificationPermissionStatus.DENIED
        } else {
            NotificationPermissionStatus.PERMANENTLY_DENIED
        }
    }
}

internal data class NotificationPermissionUi(
    val status: NotificationPermissionStatus,
    val onEnable: () -> Unit,
)

/**
 * Permiso POST_NOTIFICATIONS solicitado solo por acción del usuario desde el
 * Notification Center (nunca al startup). En Android < 13 no hay runtime
 * permission y la tarjeta no se muestra.
 */
@Suppress("InlinedApi") // POST_NOTIFICATIONS solo se usa tras comprobar SDK >= 33.
@Composable
internal fun rememberNotificationPermissionUi(): NotificationPermissionUi {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    var granted by remember { mutableStateOf(isNotificationPermissionGranted(context)) }
    var askedOnce by rememberSaveable { mutableStateOf(false) }
    var showRationale by remember { mutableStateOf(shouldShowRationale(activity)) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { result ->
        askedOnce = true
        granted = result
        showRationale = shouldShowRationale(activity)
    }

    // Refresca al volver de Ajustes del sistema.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        granted = isNotificationPermissionGranted(context)
        if (granted) showRationale = false
    }

    val status = NotificationPermissionPolicy.resolveStatus(
        sdkInt = Build.VERSION.SDK_INT,
        runtimeGranted = granted,
        askedOnce = askedOnce,
        showRationale = showRationale,
    )

    val onEnable: () -> Unit = {
        if (status == NotificationPermissionStatus.PERMANENTLY_DENIED) {
            openAppNotificationSettings(context)
        } else {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    return NotificationPermissionUi(status = status, onEnable = onEnable)
}

@Suppress("InlinedApi") // POST_NOTIFICATIONS solo se usa tras comprobar SDK >= 33.
private fun isNotificationPermissionGranted(context: Context): Boolean {
    if (!NotificationPermissionPolicy.runtimePermissionRequired(Build.VERSION.SDK_INT)) {
        return true
    }
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS,
    ) == PackageManager.PERMISSION_GRANTED
}

@Suppress("InlinedApi") // POST_NOTIFICATIONS solo se usa tras comprobar SDK >= 33.
private fun shouldShowRationale(activity: Activity?): Boolean {
    if (activity == null) return false
    if (!NotificationPermissionPolicy.runtimePermissionRequired(Build.VERSION.SDK_INT)) return false
    return ActivityCompat.shouldShowRequestPermissionRationale(
        activity,
        Manifest.permission.POST_NOTIFICATIONS,
    )
}

private fun openAppNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(intent) }
}

private fun Context.findActivity(): Activity? {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return current as? Activity
}
