package com.thehouse.app.presentation.home

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * D15 — regresión estructural: el panel "Tu cuenta" y el drawer local ya no
 * deben existir en Home; el hamburger vive únicamente en el shell.
 */
class HomeAccountBlockRemovalTest {

    private val homeSource: String by lazy {
        java.io.File("src/main/java/com/thehouse/app/presentation/home/HomeScreen.kt").readText()
    }

    private val mainSource: String by lazy {
        java.io.File("src/main/java/com/thehouse/app/MainActivity.kt").readText()
    }

    @Test
    fun `home has no AccountActionsCard or local drawer`() {
        assertFalse(homeSource.contains("AccountActionsCard"))
        assertFalse(homeSource.contains("AccountActionRow"))
        assertFalse(homeSource.contains("HomeMenuDrawer"))
        assertFalse(homeSource.contains("ModalNavigationDrawer"))
        assertFalse(homeSource.contains("rememberDrawerState"))
    }

    @Test
    fun `home keeps its main content CTA and privacy card`() {
        assertTrue(homeSource.contains("onOpenPrivacy"))
        assertTrue(homeSource.contains("PublishClicked"))
        assertTrue(homeSource.contains("TheHouseSearchBar"))
    }

    @Test
    fun `shell owns the only hamburger and the drawer`() {
        assertTrue(mainSource.contains("ModalNavigationDrawer"))
        assertTrue(mainSource.contains("AppDrawerContent"))
        assertTrue(mainSource.contains("home_menu_button"))
    }

    @Test
    fun `notifications callback is wired in main`() {
        val normalized = mainSource.replace("\r\n", "\n")
        val wired = Regex(
            "onOpenNotifications = \\{\\s*navController\\.navigate\\(AppRoutes\\.notifications\\.route\\)",
        )
        assertTrue(wired.containsMatchIn(normalized))
    }
}
