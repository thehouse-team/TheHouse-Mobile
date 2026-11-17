package com.thehouse.app.data.notifications.push

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.thehouse.app.domain.push.AppVisibility

/**
 * Visibilidad de proceso basada en actividades iniciadas/detenidas.
 * Evita agregar lifecycle-process solo para decidir foreground/background.
 */
class AppVisibilityTracker : AppVisibility, Application.ActivityLifecycleCallbacks {

    @Volatile
    private var startedActivities: Int = 0

    @Volatile
    private var registered: Boolean = false

    override fun isForeground(): Boolean = startedActivities > 0

    fun register(application: Application) {
        if (registered) return
        registered = true
        application.registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityStarted(activity: Activity) {
        startedActivities += 1
    }

    override fun onActivityStopped(activity: Activity) {
        startedActivities = (startedActivities - 1).coerceAtLeast(0)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityResumed(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
}
