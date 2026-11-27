package com.thehouse.app

import android.app.Application
import com.thehouse.app.di.AppContainer
import com.thehouse.app.perf.Perf
import com.thehouse.app.preferences.flow.PreferencesFlowLogger

/**
 * Application de TheHouse Mobile.
 *
 * Es el composition root de DI manual. No contiene lógica de negocio.
 *
 * NV-14 / D21: registra canales de notificación, visibilidad de proceso,
 * observación de sesión para push y sincronización de dispositivos con Backend.
 * Firebase se inicializa desde `google-services.json`; si falla, las funciones
 * principales siguen funcionando y el registro de dispositivo se omite.
 */
class TheHouseApplication : Application() {

    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        // Instrumentación DEBUG de rendimiento (PERF <op> durationMs=<n>).
        // En tests JVM permanece desactivado: no se toca android.util.Log.
        Perf.enabled = BuildConfig.DEBUG
        PreferencesFlowLogger.enabled = BuildConfig.DEBUG
        container.notificationChannels.ensureChannels()
        container.appVisibilityTracker.register(this)
        container.observePushSession()
    }
}
