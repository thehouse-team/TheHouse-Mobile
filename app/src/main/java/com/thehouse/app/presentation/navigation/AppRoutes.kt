package com.thehouse.app.presentation.navigation

/**
 * Destinos tipados (U2 + D4 Home/Guest).
 *
 * - Un único AppRoute por destino; la navegación no contiene lógica de negocio.
 * - D4: shell con bottom nav (inicio/explorar/smart_match/favoritos).
 *   Guest y Auth comparten destinos; el contenido de Favoritos y las
 *   acciones protegidas aplican el guard de acceso.
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

    /** Inicio del shell (tab Inicio). */
    val home: AppRoute = object : AppRoute {
        override val route: String = "inicio"
    }

    val explore: AppRoute = object : AppRoute {
        override val route: String = "explorar"
    }

    val smartMatch: AppRoute = object : AppRoute {
        override val route: String = "smart_match"
    }

    val favorites: AppRoute = object : AppRoute {
        override val route: String = "favoritos"
    }

    val visits: AppRoute = object : AppRoute {
        override val route: String = "visitas"
    }

    /** D9: solicitudes del estudiante. */
    val requests: AppRoute = object : AppRoute {
        override val route: String = "solicitudes"
    }

    /** D9: solicitudes del arrendador. */
    val landlordRequests: AppRoute = object : AppRoute {
        override val route: String = "solicitudes_arrendador"
    }

    /** D9: lista de conversaciones. */
    val conversations: AppRoute = object : AppRoute {
        override val route: String = "conversaciones"
    }

    val bottomTabs: List<AppRoute> = listOf(home, explore, smartMatch, favorites)

    fun isBottomTab(route: String?): Boolean = bottomTabs.any { it.route == route }
}
