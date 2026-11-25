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
import com.thehouse.app.presentation.smartmatch.SmartMatchScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Device automation: Smart Match states y validación de profiles.
 */
@RunWith(AndroidJUnit4::class)
class DeviceSmartMatchTest {

    @get:Rule
    val composeRule = androidx.compose.ui.test.junit4.createComposeRule()

    @Test
    fun `device: Smart Match loading state`() {
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

        // State de loading
        composeRule.onNodeWithContentDescription("smart.match.loading").assertIsDisplayed()

        // State profile incomplete (fallback local)
        composeRule.onNodeWithContentDescription("smart.match.profile.incomplete").assertIsDisplayed()
    }

    @Test
    fun `device: Smart Match 422 profile incomplete handling`() {
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

        // Respuesta 422 - profile incomplete
        composeRule.onNodeWithText("Perfil incompleto").assertIsDisplayed()
        // Acción de completar profile
        composeRule.onNodeWithContentDescription("action.complete.profile").performClick()
    }

    @Test
    fun `device: Smart Match local fallback state`() {
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

        // Fallback local cuando no hay datos del backend
        composeRule.onNodeWithContentDescription("smart.match.local.fallback").assertIsDisplayed()
    }
}