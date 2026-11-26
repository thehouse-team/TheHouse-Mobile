package com.thehouse.app.presentation.navigation

/**
 * D15 — Catálogo puro (sin Compose/Android) de los eventos del drawer global.
 *
 * `AppDrawerContent` despacha cada `tag` a exactamente uno de estos eventos.
 * Permite verificar en tests JVM que TODAS las entradas están cableadas y
 * que ninguna acción queda vacía (B6/B10).
 */
enum class DrawerAction {
    REQUIRE_LOGIN,
    PROFILE,
    PREFERENCES,
    QUICK_SETUP,
    NOTIFICATIONS,
    REQUESTS,
    CONVERSATIONS,
    MY_RENTAL,
    RENTAL_CODE,
    LANDLORD_DASHBOARD,
    LANDLORD_REQUESTS,
    LANDLORD_AGREEMENTS,
    PRIVACY,
    LOGOUT,
}

object DrawerNavigationCatalog {

    /** Mapping único y exhaustivo de tags reales del drawer. */
    val actionByTag: Map<String, DrawerAction> = mapOf(
        "drawer_login" to DrawerAction.REQUIRE_LOGIN,
        "drawer_profile" to DrawerAction.PROFILE,
        "drawer_preferences" to DrawerAction.PREFERENCES,
        "drawer_quick_setup" to DrawerAction.QUICK_SETUP,
        "drawer_notifications" to DrawerAction.NOTIFICATIONS,
        "drawer_requests" to DrawerAction.REQUESTS,
        "drawer_conversations" to DrawerAction.CONVERSATIONS,
        "drawer_my_rental" to DrawerAction.MY_RENTAL,
        "drawer_rental_code" to DrawerAction.RENTAL_CODE,
        "drawer_landlord_dashboard" to DrawerAction.LANDLORD_DASHBOARD,
        "drawer_landlord_requests" to DrawerAction.LANDLORD_REQUESTS,
        "drawer_agreements" to DrawerAction.LANDLORD_AGREEMENTS,
        "drawer_privacy" to DrawerAction.PRIVACY,
        "drawer_logout" to DrawerAction.LOGOUT,
    )

    /** Ruta real del shell para cada acción navegable; logout no navega. */
    val routeByAction: Map<DrawerAction, String> = mapOf(
        DrawerAction.REQUIRE_LOGIN to AppRoutes.login.route,
        DrawerAction.PROFILE to AppRoutes.profile.route,
        DrawerAction.PREFERENCES to AppRoutes.preferences.route,
        DrawerAction.QUICK_SETUP to AppRoutes.quickSetup.route,
        DrawerAction.NOTIFICATIONS to AppRoutes.notifications.route,
        DrawerAction.REQUESTS to AppRoutes.requests.route,
        DrawerAction.CONVERSATIONS to AppRoutes.conversations.route,
        DrawerAction.MY_RENTAL to AppRoutes.myRental.route,
        DrawerAction.RENTAL_CODE to AppRoutes.rentalCode.route,
        DrawerAction.LANDLORD_DASHBOARD to AppRoutes.landlordDashboard.route,
        DrawerAction.LANDLORD_REQUESTS to AppRoutes.landlordRequests.route,
        DrawerAction.LANDLORD_AGREEMENTS to AppRoutes.landlordAgreements.route,
        DrawerAction.PRIVACY to AppRoutes.privacy.route,
    )

    fun actionFor(tag: String): DrawerAction? = actionByTag[tag]
}
