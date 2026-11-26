package com.thehouse.app

import android.app.Application
import com.thehouse.app.di.AppContainer
import com.thehouse.app.perf.Perf

/**
 * Application de TheHouse Mobile.
 *
 * Es el composition root de DI manual. No contiene lógica de negocio.
 *
 * NV-14 Fase B: registra canales de notificación, visibilidad de proceso y
 * observación de sesión para push. Sin `google-services.json`, Firebase no se
 * inicializa y el runtime push queda bloqueado de forma segura.
 */
class TheHouseApplication : Application() {

    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        // Instrumentación DEBUG de rendimiento (PERF <op> durationMs=<n>).
        // En tests JVM permanece desactivado: no se toca android.util.Log.
        Perf.enabled = BuildConfig.DEBUG
        container.notificationChannels.ensureChannels()
        container.appVisibilityTracker.register(this)
        container.observePushSession()
    }
}
