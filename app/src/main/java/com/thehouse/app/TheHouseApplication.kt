package com.thehouse.app

import android.app.Application
import com.thehouse.app.di.AppContainer

/**
 * Application de TheHouse Mobile.
 *
 * Es el composition root de DI manual. No contiene lógica de negocio.
 */
class TheHouseApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
