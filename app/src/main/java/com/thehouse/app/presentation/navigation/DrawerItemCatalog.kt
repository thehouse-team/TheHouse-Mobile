package com.thehouse.app.presentation.navigation

/**
 * Inventario puro de las entradas del drawer global (D15).
 *
 * No contiene Compose ni Android: permite verificar en tests JVM las reglas
 * guest/auth/arrendador sin instrumentación. La UI solo renderiza este catálogo.
 */
object DrawerItemCatalog {

    data class Item(val label: String, val tag: String, val section: Section)

    enum class Section { Account, Landlord, Session }

    /** Guest: solo acciones públicas; nunca rutas protegidas que terminen en 401. */
    fun guestItems(): List<Item> = listOf(
        Item("Iniciar sesión o registrarse", "drawer_login", Section.Session),
        Item("Privacidad", "drawer_privacy", Section.Session),
    )

    /**
     * Auth: 7 acciones de cuenta, 4 de arrendador en sección visual temporal
     * (sin señal de rol inventada) y logout.
     */
    fun authItems(isLoggingOut: Boolean): List<Item> = listOf(
        Item("Mi perfil", "drawer_profile", Section.Account),
        Item("Preferencias", "drawer_preferences", Section.Account),
        Item("Configuración inicial", "drawer_quick_setup", Section.Account),
        Item("Notificaciones", "drawer_notifications", Section.Account),
        Item("Mis solicitudes", "drawer_requests", Section.Account),
        Item("Conversaciones", "drawer_conversations", Section.Account),
        Item("Mi renta", "drawer_my_rental", Section.Account),
        Item("Introducir código", "drawer_rental_code", Section.Landlord),
        Item("Mis alojamientos", "drawer_landlord_dashboard", Section.Landlord),
        Item("Solicitudes recibidas", "drawer_landlord_requests", Section.Landlord),
        Item("Mis acuerdos", "drawer_agreements", Section.Landlord),
        Item(
            if (isLoggingOut) "Cerrando sesión…" else "Cerrar sesión",
            "drawer_logout",
            Section.Session,
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
