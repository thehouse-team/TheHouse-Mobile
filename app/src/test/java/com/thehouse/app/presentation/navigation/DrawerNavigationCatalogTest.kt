package com.thehouse.app.presentation.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * D15 (B10) — Verificación JVM del cableado real del drawer global.
 *
 * Estos tests NO simulan Compose: verifican el catálogo puro que produce
 * `AppDrawerContent` y que `MainActivity` consume para navegar. Cada tag
 * despacha a una acción real; cada acción navegable tiene una ruta real.
 */
class DrawerNavigationCatalogTest {

    private val allTags = (DrawerItemCatalog.guestItems() + DrawerItemCatalog.authItems(false))
        .map { it.tag }

    @Test
    fun `01 Home account block is not part of drawer nor home content`() {
        // El bloque "Tu cuenta" fue eliminado del body de Home (D15).
        assertFalse(DrawerItemCatalog.authItems(false).any { it.label == "Tu cuenta" })
        assertFalse(DrawerItemCatalog.authItems(false).any { it.label == "Cerrar sesión" && it.tag != "drawer_logout" })
    }

    @Test
    fun `02 hamburger is a shell concern not a drawer item`() {
        // El botón global vive en MainActivity.TheHouseRoot (testTag
        // "home_menu_button"); el catálogo del drawer NO define un hamburger local.
        assertFalse(DrawerItemCatalog.guestItems().any { it.tag.contains("menu") })
        assertFalse(DrawerItemCatalog.authItems(false).any { it.tag.contains("menu") })
    }

    @Test
    fun `03 guest drawer items are exactly login and privacy`() {
        assertEquals(
            listOf("Iniciar sesión o registrarse", "Aviso de privacidad"),
            DrawerItemCatalog.guestItems().map { it.label },
        )
    }

    @Test
    fun `04 auth drawer items include all seven account actions and logout`() {
        val auth = DrawerItemCatalog.authItems(false)
        assertEquals(13, auth.size)
        assertTrue(auth.any { it.tag == "drawer_logout" })
        assertTrue(auth.any { it.tag == "drawer_privacy" })
        assertEquals(7, auth.count { it.section == DrawerItemCatalog.Section.Account })
    }

    @Test
    fun `05 every drawer tag dispatches to a real action`() {
        allTags.forEach { tag ->
            assertNotNull("Tag sin acción real: $tag", DrawerNavigationCatalog.actionFor(tag))
        }
        assertEquals(14, DrawerNavigationCatalog.actionByTag.size)
    }

    @Test
    fun `06 notifications action resolves to notifications route`() {
        assertEquals(
            AppRoutes.notifications.route,
            DrawerNavigationCatalog.routeByAction[DrawerAction.NOTIFICATIONS],
        )
    }

    @Test
    fun `07 profile action resolves to profile route`() {
        assertEquals(AppRoutes.profile.route, DrawerNavigationCatalog.routeByAction[DrawerAction.PROFILE])
    }

    @Test
    fun `08 preferences action resolves to preferences route`() {
        assertEquals(AppRoutes.preferences.route, DrawerNavigationCatalog.routeByAction[DrawerAction.PREFERENCES])
    }

    @Test
    fun `09 quick setup action resolves to quickSetup route`() {
        assertEquals(AppRoutes.quickSetup.route, DrawerNavigationCatalog.routeByAction[DrawerAction.QUICK_SETUP])
    }

    @Test
    fun `10 requests action resolves to requests route`() {
        assertEquals(AppRoutes.requests.route, DrawerNavigationCatalog.routeByAction[DrawerAction.REQUESTS])
    }

    @Test
    fun `11 conversations action resolves to conversations route`() {
        assertEquals(AppRoutes.conversations.route, DrawerNavigationCatalog.routeByAction[DrawerAction.CONVERSATIONS])
    }

    @Test
    fun `12 my rental action resolves to myRental route`() {
        assertEquals(AppRoutes.myRental.route, DrawerNavigationCatalog.routeByAction[DrawerAction.MY_RENTAL])
    }

    @Test
    fun `13 rental code action resolves to rentalCode route`() {
        assertEquals(AppRoutes.rentalCode.route, DrawerNavigationCatalog.routeByAction[DrawerAction.RENTAL_CODE])
    }

    @Test
    fun `14 landlord dashboard action resolves to landlordDashboard route`() {
        assertEquals(
            AppRoutes.landlordDashboard.route,
            DrawerNavigationCatalog.routeByAction[DrawerAction.LANDLORD_DASHBOARD],
        )
    }

    @Test
    fun `15 landlord requests action resolves to landlordRequests route`() {
        assertEquals(
            AppRoutes.landlordRequests.route,
            DrawerNavigationCatalog.routeByAction[DrawerAction.LANDLORD_REQUESTS],
        )
    }

    @Test
    fun `16 agreements action resolves to landlordAgreements route`() {
        assertEquals(
            AppRoutes.landlordAgreements.route,
            DrawerNavigationCatalog.routeByAction[DrawerAction.LANDLORD_AGREEMENTS],
        )
    }

    @Test
    fun `17 privacy action resolves to privacy route`() {
        assertEquals(AppRoutes.privacy.route, DrawerNavigationCatalog.routeByAction[DrawerAction.PRIVACY])
    }

    @Test
    fun `18 logout action is not a navigation target`() {
        assertFalse(DrawerNavigationCatalog.routeByAction.containsKey(DrawerAction.LOGOUT))
        assertEquals(DrawerAction.LOGOUT, DrawerNavigationCatalog.actionFor("drawer_logout"))
    }

    @Test
    fun `19 guest has no landlord or logout actions`() {
        val guestTags = DrawerItemCatalog.guestItems().map { it.tag }
        assertFalse(guestTags.contains("drawer_logout"))
        assertFalse(guestTags.any { DrawerNavigationCatalog.actionFor(it) == DrawerAction.LANDLORD_DASHBOARD })
    }

    @Test
    fun `20 unknown tags never navigate`() {
        assertEquals(null, DrawerNavigationCatalog.actionFor("drawer_unknown"))
    }
}
