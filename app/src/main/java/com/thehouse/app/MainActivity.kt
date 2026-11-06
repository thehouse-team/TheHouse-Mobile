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
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.thehouse.app.domain.model.SessionState
import com.thehouse.app.presentation.U2ViewModelFactory
import com.thehouse.app.presentation.auth.LoginScreen
import com.thehouse.app.presentation.auth.RegisterScreen
import com.thehouse.app.presentation.components.BottomNavigation
import com.thehouse.app.presentation.explore.ExploreScreen
import com.thehouse.app.presentation.favorites.FavoritesScreen
import com.thehouse.app.presentation.home.HomeScreen
import com.thehouse.app.presentation.navigation.AppRoutes
import com.thehouse.app.presentation.navigation.BottomTab
import com.thehouse.app.presentation.navigation.TabNavigationState
import com.thehouse.app.presentation.onboarding.OnboardingScreen
import com.thehouse.app.presentation.root.RootViewModel
import com.thehouse.app.presentation.smartmatch.SmartMatchScreen
import com.thehouse.app.presentation.welcome.WelcomeScreen
import com.thehouse.app.ui.theme.TheHouseTheme

/**
 * Host U2 + D4 (NV-04/NV-05, MVVM + UDF).
 *
 * - `RootViewModel` resuelve la pantalla inicial (Onboarding / Home shell).
 * - D4: shell con bottom nav (Inicio, Explorar, Smart Match, Favoritos).
 *   Guest y Auth comparten destinos; Favoritos y acciones protegidas
 *   aplican el muro de acceso (→ Login). La navegación no contiene
 *   lógica de negocio. La UI no consume repositorios directamente.
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
    val isAuthenticated = sessionState is SessionState.Active
    val displayName = (sessionState as? SessionState.Active)?.session?.displayName.orEmpty()

    LaunchedEffect(Unit) {
        rootViewModel.collect(container.sessionStore.state)
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val selectedTab = TabNavigationState.fromRoute(currentRoute)
    val showBottomNav = AppRoutes.isBottomTab(currentRoute)

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (showBottomNav) {
                BottomNavigation(
                    selectedTab = selectedTab,
                    onTabSelected = { tab -> navController.navigateToTab(tab) },
                )
            }
        },
    ) { padding ->
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
                            // D4: Omitir/Terminar → Guest/Home (shell).
                            navController.navigate(AppRoutes.home.route) {
                                popUpTo(0) { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                        onLoginClick = {
                            navController.navigate(AppRoutes.login.route) {
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
                            // Tras el logout, RootViewModel reevalúa
                            // (Guest/Home o Onboarding según marca local).
                            // No se fuerza Login: las acciones protegidas
                            // lo piden explícitamente.
                        },
                        onRequireLogin = {
                            navController.navigate(AppRoutes.login.route) {
                                launchSingleTop = true
                            }
                        },
                        onNavigateToExplore = {
                            navController.navigateToTab(BottomTab.Explorar)
                        },
                        factory = factory,
                    )
                }
                composable(AppRoutes.explore.route) {
                    ExploreScreen(
                        onFavoriteClick = {
                            if (!isAuthenticated) {
                                navController.navigate(AppRoutes.login.route) {
                                    launchSingleTop = true
                                }
                            }
                        },
                        onContactClick = {
                            if (!isAuthenticated) {
                                navController.navigate(AppRoutes.login.route) {
                                    launchSingleTop = true
                                }
                            }
                        },
                    )
                }
                composable(AppRoutes.smartMatch.route) {
                    SmartMatchScreen(
                        onFavoriteClick = {
                            if (!isAuthenticated) {
                                navController.navigate(AppRoutes.login.route) {
                                    launchSingleTop = true
                                }
                            }
                        },
                        onContactClick = {
                            if (!isAuthenticated) {
                                navController.navigate(AppRoutes.login.route) {
                                    launchSingleTop = true
                                }
                            }
                        },
                    )
                }
                composable(AppRoutes.favorites.route) {
                    FavoritesScreen(
                        isAuthenticated = isAuthenticated,
                        onLoginClick = {
                            navController.navigate(AppRoutes.login.route) {
                                launchSingleTop = true
                            }
                        },
                    )
                }
            }

            // Navegación dirigida por la decisión de root (sesión + onboarding/guest).
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

private fun NavController.navigateToTab(tab: BottomTab) {
    navigate(tab.route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
