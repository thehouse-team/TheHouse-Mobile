package com.thehouse.app.presentation.navigation

/**
 * Inventario puro de las entradas del drawer global (D15).
 *
 * No contiene Compose ni Android: permite verificar en tests JVM las reglas
 * guest/auth/arrendador sin instrumentación. La UI solo renderiza este catálogo.
 */
object DrawerItemCatalog {

    /** Clave de icono abstracta: la UI la resuelve a un ImageVector Material 3. */
    enum class IconKey {
        Login, Privacy, Profile, Preferences, QuickSetup, Notifications,
        Requests, Conversations, Rental, Code, Landlord, LandlordRequests,
        Agreements, Logout,
    }

    data class Item(
        val label: String,
        val tag: String,
        val section: Section,
        val icon: IconKey = IconKey.Login,
    )

    enum class Section { Account, Landlord, Session }

    /** Guest: solo acciones públicas; nunca rutas protegidas que terminen en 401. */
    fun guestItems(): List<Item> = listOf(
        Item("Iniciar sesión o registrarse", "drawer_login", Section.Session, IconKey.Login),
        Item("Aviso de privacidad", "drawer_privacy", Section.Session, IconKey.Privacy),
    )

    /**
     * Auth: 7 acciones de cuenta, 4 de arrendador en sección visual temporal
     * (sin señal de rol inventada) y logout.
     */
    fun authItems(isLoggingOut: Boolean): List<Item> = listOf(
        Item("Mi perfil", "drawer_profile", Section.Account, IconKey.Profile),
        Item("Preferencias", "drawer_preferences", Section.Account, IconKey.Preferences),
        Item("Configuración inicial", "drawer_quick_setup", Section.Account, IconKey.QuickSetup),
        Item("Notificaciones", "drawer_notifications", Section.Account, IconKey.Notifications),
        Item("Mis solicitudes", "drawer_requests", Section.Account, IconKey.Requests),
        Item("Conversaciones", "drawer_conversations", Section.Account, IconKey.Conversations),
        Item("Mi renta", "drawer_my_rental", Section.Account, IconKey.Rental),
        Item("Introducir código", "drawer_rental_code", Section.Landlord, IconKey.Code),
        Item("Mis alojamientos", "drawer_landlord_dashboard", Section.Landlord, IconKey.Landlord),
        Item("Solicitudes recibidas", "drawer_landlord_requests", Section.Landlord, IconKey.LandlordRequests),
        Item("Mis acuerdos", "drawer_agreements", Section.Landlord, IconKey.Agreements),
        // D19: el aviso de privacidad también debe estar accesible al usuario
        // autenticado (decisión UX definitiva).
        Item("Aviso de privacidad", "drawer_privacy", Section.Session, IconKey.Privacy),
        Item(
            if (isLoggingOut) "Cerrando sesión…" else "Cerrar sesión",
            "drawer_logout",
            Section.Session,
            IconKey.Logout,
        ),
    )

    /** Rutas reales que debe navegar cada opción; ninguna es dummy. */
    val routeByTag: Map<String, String> = mapOf(
        "drawer_login" to AppRoutes.login.route,
        "drawer_profile" to AppRoutes.profile.route,
        "drawer_preferences" to AppRoutes.preferences.route,
        "drawer_quick_setup" to AppRoutes.quickSetup.route,
        "drawer_notifications" to AppRoutes.notifications.route,
        "drawer_requests" to AppRoutes.requests.route,
        "drawer_conversations" to AppRoutes.conversations.route,
        "drawer_my_rental" to AppRoutes.myRental.route,
        "drawer_rental_code" to AppRoutes.rentalCode.route,
        "drawer_landlord_dashboard" to AppRoutes.landlordDashboard.route,
        "drawer_landlord_requests" to AppRoutes.landlordRequests.route,
        "drawer_agreements" to AppRoutes.landlordAgreements.route,
        "drawer_privacy" to AppRoutes.privacy.route,
    )
}
