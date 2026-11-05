package com.thehouse.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.thehouse.app.presentation.U2ViewModelFactory
import com.thehouse.app.domain.model.SessionState
import com.thehouse.app.presentation.auth.LoginScreen
import com.thehouse.app.presentation.auth.RegisterScreen
import com.thehouse.app.presentation.home.HomeScreen
import com.thehouse.app.presentation.onboarding.OnboardingScreen
import com.thehouse.app.presentation.navigation.AppRoutes
import com.thehouse.app.presentation.root.RootViewModel
import com.thehouse.app.presentation.welcome.WelcomeScreen
import com.thehouse.app.ui.theme.TheHouseTheme

/**
 * Host de la fase U2 (spec NV-04, MVVM + UDF).
 *
 * - `RootViewModel` resuelve la pantalla inicial según sesión/config.
 * - Navigation Compose declara el grafo U2; la navegación no contiene lógica
 *   de negocio.
 * - La UI no consume repositorios ni infraestructura directamente.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val factory = U2ViewModelFactory((application as TheHouseApplication).container)

        setContent {
            TheHouseTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TheHouseRoot(factory = factory)
                }
            }
        }
    }
}

@Composable
fun TheHouseRoot(factory: U2ViewModelFactory, modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val rootViewModel: RootViewModel = viewModel(factory = factory)
    val rootState by rootViewModel.state.collectAsStateWithLifecycle()

    val container = (LocalContext.current.applicationContext as TheHouseApplication).container
    val sessionState by container.sessionStore.state.collectAsStateWithLifecycle()
    val displayName = (sessionState as? SessionState.Active)?.session?.displayName.orEmpty()

    LaunchedEffect(Unit) {
        rootViewModel.collect(container.sessionStore.state)
    }

    Scaffold(modifier = modifier) { padding ->
        if (rootState.isResolving) {
            Surface(modifier = Modifier.fillMaxSize()) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        } else {
            NavHost(
                navController = navController,
                startDestination = AppRoutes.root.route,
                modifier = Modifier.padding(padding),
            ) {
                composable(AppRoutes.root.route) {
                    // Placeholder mínimo: la decisión de root navega apenas resuelve.
                }
                composable(AppRoutes.login.route) {
                    LoginScreen(
                        onNavigateToRegister = {
                            navController.navigate(AppRoutes.register.route)
                        },
                        onLoggedIn = {
                            // La sesión activa dispara la decisión de root
                            // (onboarding pendiente → onboarding; completado → home).
                        },
                        factory = factory,
                    )
                }
                composable(AppRoutes.register.route) {
                    RegisterScreen(
                        onNavigateToLogin = {
                            navController.navigate(AppRoutes.login.route) {
                                popUpTo(0) { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                        onNavigateToOnboarding = {
                            navController.navigate(AppRoutes.onboarding.route) {
                                popUpTo(0) { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                        factory = factory,
                    )
                }
                composable(AppRoutes.onboarding.route) {
                    OnboardingScreen(
                        onFinished = {
                            navController.navigate(AppRoutes.welcome.route) {
                                popUpTo(0) { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                        factory = factory,
                    )
                }
                composable(AppRoutes.welcome.route) {
                    WelcomeScreen(
                        displayName = displayName,
                        onContinue = {
                            navController.navigate(AppRoutes.home.route) {
                                popUpTo(0) { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                    )
                }
                composable(AppRoutes.home.route) {
                    HomeScreen(
                        displayName = displayName,
                        onLoggedOut = {
                            navController.navigate(AppRoutes.login.route) {
                                popUpTo(0) { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                        factory = factory,
                    )
                }
            }

            // Navegación dirigida por la decisión de root (sesión + onboarding).
            LaunchedEffect(rootState.targetRoute) {
                rootState.targetRoute?.let { target ->
                    navController.navigate(target) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
        }
    }
}
