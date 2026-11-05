package com.thehouse.app.presentation.navigation

/**
 * Destinos tipados de la fase U2 (spec NV-04).
 *
 * Un único AppRoute por destino; la navegación no contiene lógica de negocio.
 */
object AppRoutes {

    val root: AppRoute = object : AppRoute {
        override val route: String = "root"
    }

    val login: AppRoute = object : AppRoute {
        override val route: String = "login"
    }

    val register: AppRoute = object : AppRoute {
        override val route: String = "register"
    }

    val onboarding: AppRoute = object : AppRoute {
        override val route: String = "onboarding"
    }

    val welcome: AppRoute = object : AppRoute {
        override val route: String = "welcome"
    }

    val home: AppRoute = object : AppRoute {
        override val route: String = "home"
    }
}
