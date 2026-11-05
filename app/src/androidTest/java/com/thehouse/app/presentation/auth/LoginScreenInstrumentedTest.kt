package com.thehouse.app.presentation.auth

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.platform.app.InstrumentationRegistry
import com.thehouse.app.TheHouseApplication
import com.thehouse.app.presentation.U2ViewModelFactory
import com.thehouse.app.presentation.navigation.AppRoutes
import org.junit.Rule
import org.junit.Test

/**
 * UI test mínimo de Login (NV04-REQ-010, Compose Test).
 *
 * Ejecutar con instrumentación (emulador); no forma parte de la JVM de U2.
 */
class LoginScreenInstrumentedTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `login muestra boton y navega a crear cuenta`() {
        val appContext = InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext as TheHouseApplication
        val factory = U2ViewModelFactory(appContext.container)

        composeRule.setContent {
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = AppRoutes.login.route) {
                composable(AppRoutes.login.route) {
                    LoginScreen(
                        onNavigateToRegister = { navController.navigate(AppRoutes.register.route) },
                        onLoggedIn = { },
                        factory = factory,
                    )
                }
                composable(AppRoutes.register.route) {
                    androidx.compose.material3.Text("Crear cuenta")
                }
            }
        }

        composeRule.onNodeWithText("Iniciar sesión").assertIsDisplayed()
        composeRule.onNodeWithText("¿No tienes cuenta? Crear cuenta").performClick()
        composeRule.onNodeWithText("Crear cuenta").assertIsDisplayed()
    }
}
