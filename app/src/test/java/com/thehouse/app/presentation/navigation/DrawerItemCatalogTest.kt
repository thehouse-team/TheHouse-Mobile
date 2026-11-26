package com.thehouse.app.presentation.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** D15 — reglas guest/auth/arrendador e inventario de rutas reales. */
class DrawerItemCatalogTest {

    @Test
    fun `guest sees only public actions`() {
        val items = DrawerItemCatalog.guestItems()
        assertEquals(listOf("drawer_login", "drawer_privacy"), items.map { it.tag })
        assertFalse(items.any { it.section == DrawerItemCatalog.Section.Landlord })
        assertFalse(items.any { it.tag == "drawer_logout" })
    }

    @Test
    fun `guest sees no protected routes`() {
        val protected = setOf(
            "drawer_profile", "drawer_preferences", "drawer_quick_setup",
            "drawer_notifications", "drawer_requests", "drawer_conversations",
            "drawer_my_rental", "drawer_rental_code", "drawer_landlord_dashboard",
            "drawer_landlord_requests", "drawer_agreements",
        )
        val guestTags = DrawerItemCatalog.guestItems().map { it.tag }.toSet()
        assertTrue(protected.intersect(guestTags).isEmpty())
    }

    @Test
    fun `auth sees seven account items`() {
        val account = DrawerItemCatalog.authItems(false)
            .filter { it.section == DrawerItemCatalog.Section.Account }
        assertEquals(7, account.size)
        assertEquals(
            listOf(
                "Mi perfil", "Preferencias", "Configuración inicial", "Notificaciones",
                "Mis solicitudes", "Conversaciones", "Mi renta",
            ),
            account.map { it.label },
        )
    }

    @Test
    fun `landlord items are in a temporary visual section`() {
        val landlord = DrawerItemCatalog.authItems(false)
            .filter { it.section == DrawerItemCatalog.Section.Landlord }
        assertEquals(4, landlord.size)
        assertEquals(
            listOf("Introducir código", "Mis alojamientos", "Solicitudes recibidas", "Mis acuerdos"),
            landlord.map { it.label },
        )
    }

    @Test
    fun `logout label reflects in-flight state`() {
        val idle = DrawerItemCatalog.authItems(false).last()
        val pending = DrawerItemCatalog.authItems(true).last()
        assertEquals("Cerrar sesión", idle.label)
        assertEquals("Cerrando sesión…", pending.label)
        assertEquals("drawer_logout", idle.tag)
    }

    @Test
    fun `every navigable item maps to a real route`() {
        val tags = (DrawerItemCatalog.guestItems() + DrawerItemCatalog.authItems(false))
            .map { it.tag }
            .filter { it != "drawer_logout" }
        tags.forEach { tag ->
            assertTrue("Falta ruta real para $tag", DrawerItemCatalog.routeByTag.containsKey(tag))
        }
        assertEquals(13, DrawerItemCatalog.routeByTag.size)
    }

    @Test
    fun `routed tags use existing app routes`() {
        assertEquals("perfil", DrawerItemCatalog.routeByTag["drawer_profile"])
        assertEquals("preferencias", DrawerItemCatalog.routeByTag["drawer_preferences"])
        assertEquals("configuracion_inicial", DrawerItemCatalog.routeByTag["drawer_quick_setup"])
        assertEquals("notificaciones", DrawerItemCatalog.routeByTag["drawer_notifications"])
        assertEquals("solicitudes", DrawerItemCatalog.routeByTag["drawer_requests"])
        assertEquals("conversaciones", DrawerItemCatalog.routeByTag["drawer_conversations"])
        assertEquals("mi_renta", DrawerItemCatalog.routeByTag["drawer_my_rental"])
        assertEquals("codigo_renta", DrawerItemCatalog.routeByTag["drawer_rental_code"])
        assertEquals("arrendador", DrawerItemCatalog.routeByTag["drawer_landlord_dashboard"])
        assertEquals("solicitudes_arrendador", DrawerItemCatalog.routeByTag["drawer_landlord_requests"])
        assertEquals("acuerdos_arrendador", DrawerItemCatalog.routeByTag["drawer_agreements"])
        assertEquals("privacidad", DrawerItemCatalog.routeByTag["drawer_privacy"])
    }
}
