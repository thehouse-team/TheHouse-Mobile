package com.thehouse.app

import android.app.Application
import com.thehouse.app.di.AppContainer

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
        container.notificationChannels.ensureChannels()
        container.appVisibilityTracker.register(this)
        container.observePushSession()
    }
}
