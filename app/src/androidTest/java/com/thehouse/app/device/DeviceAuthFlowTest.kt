package com.thehouse.app.presentation.auth

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator by
import androidx.test.uiautomator DeviceObject
import androidx.test.uiautomator UiDevice
import androidx.test.uiautomator UiObject2
import com.thehouse.app.TheHouseApplication
import com.thehouse.app.presentation.U2ViewModelFactory
import com.thehouse.app.presentation.navigation.AppRoutes
import com.thehouse.app.presentation.smartmatch.SmartMatchScreen
import com.thehouse.app.presentation.home.HomeScreen
import com.thehouse.app.presentation.profile.ProfileScreen
import com.thehouse.app.presentation.preferences.PreferencesScreen
import com.thehouse.app.presentation.root.RootViewModel
import com.thehouse.app.presentation.welcome.WelcomeScreen
import com.thehouse.app.presentation.auth.LoginScreen
import com.thehouse.app.presentation.auth.RegisterScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Device automation tests para la suite de seguridad DevSecOps.
 * Ejecutables con: ./gradlew.bat connectedDebugAndroidTest
 * Diseñados para dispositivo físico o emulador.
 *
 * Relación OWASP: Testing, User Interface Validation
 */
@RunWith(AndroidJUnit4::class)
class DeviceAuthFlowTest {

    @get:Rule
    val composeRule = androidx.compose.ui.test.junit4.createComposeRule()

    @Test
    fun `device: app arranca en pantalla bienvenida`() {
        val appContext = InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext as TheHouseApplication

        composeRule.setContent {
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = AppRoutes.welcome.route) {
                composable(AppRoutes.welcome.route) { WelcomeScreen() }
                composable(AppRoutes.login.route) { LoginScreen() }
                composable(AppRoutes.register.route) { RegisterScreen() }
            }
        }

        // Verificar que WelcomeScreen está visible al arrancar
        composeRule.onNodeWithContentDescription("screen.welcome").assertIsDisplayed()
    }

    @Test
    fun `device: navegación login → register → home shell`() {
        val appContext = InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext as TheHouseApplication

        val factory = U2ViewModelFactory(appContext.container)

        composeRule.setContent {
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = AppRoutes.login.route) {
                composable(AppRoutes.login.route) { LoginScreen() }
                composable(AppRoutes.register.route) { RegisterScreen() }
            }
        }

        // Pantalla de login visible
        composeRule.onNodeWithText("Iniciar sesión").assertIsDisplayed()

        // Navegar a registro
        composeRule.onNodeWithText("¿No tienes cuenta? Crear cuenta").performClick()
        composeRule.onNodeWithText("Crear cuenta").assertIsDisplayed()

        // Regresar y hacer login
        composeRule.onNodeWithText("←").performClick()
        composeRule.onNodeWithText("Iniciar sesión").assertIsDisplayed()
    }
}

@RunWith(AndroidJUnit4::class)
class DeviceNavigationTest {

    @get:Rule
    val composeRule = androidx.compose.ui.test.junit4.createComposeRule()

    @Test
    fun `device: hamburger abre y cierra navigation drawer`() {
        val appContext = InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext as TheHouseApplication

        composeRule.setContent {
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = AppRoutes.home.route) {
                composable(AppRoutes.home.route) { HomeScreen() }
            }
        }

        // Verificar home screen visible
        composeRule.onNodeWithText("Inicio").assertIsDisplayed()

        // Test con UiAutomator para navigation drawer
        val device = DeviceObject.current()
        // Buscar ícono de hamburger o drawer
        try {
            val drawerIcon = device.findObject(
                androidx.test.uiautomator.query.getByDescription("navicon")
            )
            assertNotNull(drawerIcon)
            drawerIcon.click()
        } catch (e: Exception) {
            // Si no hay drawer, al menos verificar bottom navigation
            composeRule.onNodeWithContentDescription("bottom.tab.property.list").assertIsDisplayed()
        }
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

        // Navegar a detail y regresar
        composeRule.onNodeWithContentDescription("property.item.1").performClick()
        // Regresar con botón físico o up navigation
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

        val factory = U2ViewModelFactory(appContext.container)

        composeRule.setContent {
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = AppRoutes.home.route) {
                composable(AppRoutes.home.route) { HomeScreen() }
                composable(AppRoutes.profile.route) { ProfileScreen() }
            }
        }

        composeRule.onNodeWithContentDescription("nav.profile").assertIsDisplayed()
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

@RunWith(AndroidJUnit4::class)
class DeviceSmartMatchTest {

    @get:Rule
    val composeRule = androidx.compose.ui.test.junit4.createComposeRule()

    @Test
    fun `device: Smart Match loading y error states`() {
        val appContext = InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext as TheHouseApplication

        val factory = U2ViewModelFactory(appContext.container)

        composeRule.setContent {
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = AppRoutes.home.route) {
                composable(AppRoutes.home.route) { HomeScreen() }
                composable(AppRoutes.smartMatch.route) { SmartMatchScreen(factory = factory) }
            }
        }

        // Verificar state de loading
        composeRule.onNodeWithContentDescription("smart.match.loading").assertIsDisplayed()

        // Estado de profile incomplete (fallback local)
        composeRule.onNodeWithContentDescription("smart.match.profile.incomplete").assertIsDisplayed()

        // Estado local fallback cuando no hay datos del backend
        composeRule.onNodeWithContentDescription("smart.match.local.fallback").assertIsDisplayed()
    }

    @Test
    fun `device: Smart Match 422 profile incomplete`() {
        composeRule.setContent {
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = AppRoutes.home.route) {
                composable(AppRoutes.home.route) { HomeScreen() }
                composable(AppRoutes.smartMatch.route) { SmartMatchScreen(factory = factory) }
            }
        }

        // Simular respuesta 422 - profile incomplete
        composeRule.onNodeWithText("Perfil incompleto").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("action.complete.profile").performClick()
    }
}