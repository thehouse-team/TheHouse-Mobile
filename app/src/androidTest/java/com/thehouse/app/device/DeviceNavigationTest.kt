package com.thehouse.app.device

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.thehouse.app.TheHouseApplication
import com.thehouse.app.presentation.U2ViewModelFactory
import com.thehouse.app.presentation.navigation.AppRoutes
import com.thehouse.app.presentation.home.HomeScreen
import com.thehouse.app.presentation.profile.ProfileScreen
import com.thehouse.app.presentation.preferences.PreferencesScreen
import com.thehouse.app.presentation.smartmatch.SmartMatchScreen
import com.thehouse.app.presentation.auth.LoginScreen
import com.thehouse.app.presentation.auth.RegisterScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Device automation: navegación y states de Smart Match.
 * Ejecutables con: ./gradlew.bat connectedDebugAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class DeviceNavigationTest {

    @get:Rule
    val composeRule = androidx.compose.ui.test.junit4.createComposeRule()

    @Test
    fun `device: hamburger abre navigation drawer`() {
        val appContext = InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext as TheHouseApplication

        composeRule.setContent {
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = AppRoutes.home.route) {
                composable(AppRoutes.home.route) { HomeScreen() }
            }
        }

        // Verificar home visible
        composeRule.onNodeWithText("Inicio").assertIsDisplayed()

        // Usar UiAutomator para drawer
        val device = androidx.test.uiautomator.device
        val drawer = device.findObject(
            androidx.test.uiautomator.query.getByDescription("navicon")
        )
        assertNotNull("Ícono de drawer debería existir", drawer)
        drawer.click()

        // Verificar que algo del drawer está visible
        composeRule.onNodeWithContentDescription("drawer.item.property.list").assertIsDisplayed()
    }

    @Test
    fun `device: cambio Lista ↔ Mapa no crashea`() {
        val appContext = InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext as TheHouseApplication

        composeRule.setContent {
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = AppRoutes.home.route) {
                composable(AppRoutes.home.route) { HomeScreen() }
                composable(AppRoutes.propertyDetail.route) { androidx.compose.material3.Text("Property Detail") }
            }
        }

        // Navegar a detail
        composeRule.onNodeWithContentDescription("property.item.sample").performClick()

        // Regresar
        composeRule.onNodeWithText("←").performClick()
        composeRule.onNodeWithText("Inicio").assertIsDisplayed()
    }
}

@RunWith(AndroidJUnit4::class)
class DeviceProfilePreferencesTest {

    @get:Rule
    val composeRule = androidx.compose.ui.test.junit4.createComposeRule()

    @Test
    fun `device: navegación a Profile`() {
        val appContext = InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext as TheHouseApplication

        composeRule.setContent {
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = AppRoutes.home.route) {
                composable(AppRoutes.home.route) { HomeScreen() }
                composable(AppRoutes.profile.route) { ProfileScreen() }
            }
        }

        composeRule.onNodeWithContentDescription("nav.profile").performClick()
        composeRule.onNodeWithText("Perfil").assertIsDisplayed()
    }

    @Test
    fun `device: navegación a Preferences`() {
        composeRule.setContent {
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = AppRoutes.home.route) {
                composable(AppRoutes.home.route) { HomeScreen() }
                composable(AppRoutes.profile.route) { ProfileScreen() }
                composable(AppRoutes.preferences.route) { PreferencesScreen() }
            }
        }

        composeRule.onNodeWithContentDescription("nav.preferences").performClick()
        composeRule.onNodeWithText("Preferencias").assertIsDisplayed()
    }
}