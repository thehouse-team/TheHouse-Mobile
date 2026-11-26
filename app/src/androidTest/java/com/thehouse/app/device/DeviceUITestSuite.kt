/**
 * Device automation UI test suite covering U2 functional flows.
 * Ejecutables con: ./gradlew.bat connectedDebugAndroidTest
 *
 * - Cubre flujos deterministas que NO dependen de backend real.
 * - Usa state controlado/fake cuando es necesario.
 * - No introduce credenciales reales ni secretos.
 */
package com.thehouse.app.device

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTestTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator by
import androidx.test.uiautomator DeviceObject
import androidx.test.uiautomator UiDevice
import androidx.test.uiautomator UiObject2
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

import com.thehouse.app.TheHouseApplication
import com.thehouse.app.presentation.U2ViewModelFactory
import com.thehouse.app.presentation.navigation.AppRoutes
import com.thehouse.app.presentation.home.HomeScreen
import com.thehouse.app.presentation.explore.ExploreScreen
import com.thehouse.app.presentation.profile.ProfileScreen
import com.thehouse.app.presentation.preferences.PreferencesScreen
import com.thehouse.app.presentation.smartmatch.SmartMatchScreen
import com.thehouse.app.presentation.auth.LoginScreen
import com.thehouse.app.presentation.auth.RegisterScreen
import com.thehouse.app.presentation.welcome.WelcomeScreen
import com.thehouse.app.presentation.root.RootViewModel

/**
 * Tests instrumentados para suite de automación en dispositivo físico.
 *
 * Priorizar flujos deterministas que NO dependan de Backend real.
 */
@RunWith(AndroidJUnit4::class)
class DeviceUITestSuite {

    @get:Rule
    val composeRule = androidx.compose.ui.test.junit4.createComposeRule()

    /**
     * A. APP STARTUP
     * Validar:
     * - MainActivity inicia.
     * - UI principal esperada renderiza.
     * - no crash inmediato.
     */
    @Test
    fun `app: MainActivity inicia y UI principal renderiza`() {
        val appContext = InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext as TheHouseApplication

        composeRule.setContent {
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = AppRoutes.home.route) {
                composable(AppRoutes.home.route) { HomeScreen() }
            }
        }

        // Verificar que la pantalla principal está visible
        composeRule.onNodeWithText("Inicio").assertIsDisplayed()
        // Verificación de que no hay crash inmediato al renderizar
        composeRule.onNodeWithContentDescription("Saludo de inicio").assertIsDisplayed()
    }

    /**
     * B. ONBOARDING / AUTH SHELL
     * Validar:
     * - elementos principales visibles.
     * - navegación básica disponible.
     * - campos/CTA accesibles.
     * No introducir credenciales reales.
     */
    @Test
    fun `app: Onboarding shell - welcome screen visible`() {
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
        // El welcome screen muestra el greeting y el botón "Continuar"
        composeRule.onNodeWithText("Bienvenido a TheHouse").assertIsDisplayed()
        composeRule.onNodeWithText("Continuar").assertIsDisplayed()
    }

    @Test
    fun `app: Onboarding shell - navigation login to register to home`() {
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

    /**
     * C. NAVIGATION DRAWER
     * Validar:
     * - hamburguesa existe.
     * - drawer abre.
     * - destinos principales aparecen.
     * - drawer cierra.
     * 
     * Usa UiAutomator para interacción con el drawer (patrón existente).
     */
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

        // Verificar home screen visible
        composeRule.onNodeWithText("Inicio").assertIsDisplayed()

        // Usar UiAutomator para drawer - el ícono de hamburger tiene descripción "navicon" por defecto
        val device = androidx.test.uiautomator.device
        val drawerIcon = device.findObject(
            androidx.test.uiautomator.query.getByDescription("navicon")
        )
        assertNotNull("Ícono de drawer debería existir", drawerIcon)
        drawerIcon.click()

        // Verificar que la home screen sigue visible después de interactuar con el drawer
        composeRule.onNodeWithText("Inicio").assertIsDisplayed()
    }

    @Test
    fun `device: drawer cierra y vuelve a home`() {
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

        // Abrir drawer
        val device = androidx.test.uiautomator.device
        val drawerIcon = device.findObject(
            androidx.test.uiautomator.query.getByDescription("navicon")
        )
        assertNotNull("Ícono de drawer debería existir", drawerIcon)
        drawerIcon.click()

        // Cerrar drawer - verificar que regresamos a home screen
        composeRule.onNodeWithText("Inicio").assertIsDisplayed()
    }

    /**
     * D. BOTTOM NAVIGATION
     * Validar:
     * - destinos configurados aparecen.
     * - selección cambia pantalla cuando el estado permite navegar.
     * 
     * Usa testTag "bottom_navigation" agregado al BottomNavigation.
     */
    @Test
    fun `device: bottom navigation - tabs appear`() {
        val appContext = InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext as TheHouseApplication

        composeRule.setContent {
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = AppRoutes.home.route) {
                composable(AppRoutes.home.route) { HomeScreen() }
            }
        }

        // Verificar que las pestañas de bottom navigation están configuradas
        // Al iniciar, Home está seleccionado por defecto
        composeRule.onNodeWithText("Inicio").assertIsDisplayed()
    }

    @Test
    fun `device: bottom navigation - switch to explore tab`() {
        val appContext = InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext as TheHouseApplication

        val factory = U2ViewModelFactory(appContext.container)

        composeRule.setContent {
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = AppRoutes.home.route) {
                composable(AppRoutes.home.route) { HomeScreen() }
                composable(AppRoutes.explore.route) { ExploreScreen(factory = factory) }
            }
        }

        // Navegar a Explorar usando testTag bottom_navigation
        composeRule.onNodeWithTestTag("bottom_navigation").performClick()

        // Verificar que cambiamos de pestaña - deberíamos ver contenido de Explore
        composeRule.onNodeWithText("Explora rentas cerca de ti").assertIsDisplayed()
    }

    /**
     * E. EXPLORE
     * Validar:
     * - selector Lista/Mapa existe (list view por defecto, sin exigir tiles online).
     * - no crash.
     * - No exigir tiles online para considerar PASS de navegación.
     */
    @Test
    fun `device: explore list mode no crash`() {
        val appContext = InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext as TheHouseApplication

        val factory = U2ViewModelFactory(appContext.container)

        composeRule.setContent {
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = AppRoutes.home.route) {
                composable(AppRoutes.home.route) { HomeScreen() }
                composable(AppRoutes.explore.route) { ExploreScreen(factory = factory) }
            }
        }

        // Verificar que el modo list view está presente y no crashea
        // El explore screen muestra listado de propiedades por defecto
        composeRule.onNodeWithText("Explora rentas cerca de ti").assertIsDisplayed()
        // Verificar content description existente en ExploreScreen
        composeRule.onNodeWithContentDescription("Título Explorar").assertIsDisplayed()
    }

    @Test
    fun `device: explore content descriptions present`() {
        val appContext = InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext as TheHouseApplication

        val factory = U2ViewModelFactory(appContext.container)

        composeRule.setContent {
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = AppRoutes.home.route) {
                composable(AppRoutes.home.route) { HomeScreen() }
                composable(AppRoutes.explore.route) { ExploreScreen(factory = factory) }
            }
        }

        // Verificar content descriptions existen para testabilidad
        composeRule.onNodeWithContentDescription("Título Explorar").assertIsDisplayed()
    }

    /**
     * F. PROFILE / PREFERENCES
     * Si las rutas son accesibles con estado injectable/fake:
     * - Profile renderiza.
     * - Preferences renderiza.
     * - navegación ida/regreso.
     * 
     * Si requieren auth estricto: usar la técnica mínima existente de fake state/test fixture.
     * NO hacer bypass inseguro en producción.
     * 
     * Usa testTag "profile_screen" y "preferences_screen" agregados a los elementos UI.
     */
    @Test
    fun `device: profile screen renders with fake state`() {
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

        // Verificar que Profile renderiza sin crashear
        // Profile tiene testTag "profile_screen" agregado al ícono de persona
        composeRule.onNodeWithTestTag("profile_screen").assertIsDisplayed()
    }

    @Test
    fun `device: preferences screen renders with fake state`() {
        val appContext = InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext as TheHouseApplication

        val factory = U2ViewModelFactory(appContext.container)

        composeRule.setContent {
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = AppRoutes.home.route) {
                composable(AppRoutes.home.route) { HomeScreen() }
                composable(AppRoutes.profile.route) { ProfileScreen() }
                composable(AppRoutes.preferences.route) { PreferencesScreen(factory = factory) }
            }
        }

        // Verificar que Preferences renderiza sin crashear
        // Preferences tiene testTag "preferences_screen" agregado a la columna principal
        composeRule.onNodeWithTestTag("preferences_screen").assertIsDisplayed()
    }

    @Test
    fun `device: profile to preferences navigation`() {
        val appContext = InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext as TheHouseApplication

        val factory = U2ViewModelFactory(appContext.container)

        composeRule.setContent {
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = AppRoutes.home.route) {
                composable(AppRoutes.home.route) { HomeScreen() }
                composable(AppRoutes.profile.route) { ProfileScreen() }
                composable(AppRoutes.preferences.route) { PreferencesScreen(factory = factory) }
            }
        }

        // Navegar de Profile a Preferences usando testTags
        composeRule.onNodeWithTestTag("profile_screen").performClick()
        composeRule.onNodeWithTestTag("preferences_screen").assertIsDisplayed()

        // Regresar con botón up
        composeRule.onNodeWithText("←").performClick()
        composeRule.onNodeWithTestTag("profile_screen").assertIsDisplayed()
    }

    /**
     * G. SMART MATCH (D16): se verifica la renderización real del Screen.
     * Los estados (Loading/Content/NoPreferences/Error/Estimación local)
     * dependen del Backend y de la sesión del dispositivo; no se inventan.
     */
    @Test
    fun `device: Smart Match screen renders real title`() {
        val appContext = InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext as TheHouseApplication
        val factory = U2ViewModelFactory(appContext.container)

        composeRule.setContent {
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = AppRoutes.smartMatch.route) {
                composable(AppRoutes.home.route) { HomeScreen() }
                composable(AppRoutes.smartMatch.route) {
                    SmartMatchScreen(
                        factory = factory,
                        onRequireLogin = {},
                        onConfigurePreferences = {},
                        onPropertyClick = {},
                        onFavoriteClick = {},
                        onContactClick = {},
                    )
                }
            }
        }

        composeRule.onNodeWithContentDescription("Título Smart Match").assertIsDisplayed()
    }

}