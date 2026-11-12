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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
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
import com.thehouse.app.presentation.favorites.FavoritesEvent
import com.thehouse.app.presentation.favorites.FavoritesScreen
import com.thehouse.app.presentation.favorites.FavoritesViewModel
import com.thehouse.app.presentation.home.HomeScreen
import com.thehouse.app.presentation.navigation.AppRoutes
import com.thehouse.app.presentation.navigation.PropertyDetailRoute
import com.thehouse.app.presentation.navigation.ScheduleVisitRoute
import com.thehouse.app.presentation.navigation.VisitDetailRoute
import com.thehouse.app.presentation.navigation.BottomTab
import com.thehouse.app.presentation.navigation.TabNavigationState
import com.thehouse.app.presentation.onboarding.OnboardingScreen
import com.thehouse.app.presentation.root.RootViewModel
import com.thehouse.app.presentation.propertydetail.PropertyDetailDestination
import com.thehouse.app.presentation.smartmatch.SmartMatchScreen
import com.thehouse.app.presentation.visits.ScheduleVisitDestination
import com.thehouse.app.presentation.visits.VisitDetailEvent
import com.thehouse.app.presentation.visits.VisitDetailScreen
import com.thehouse.app.presentation.visits.VisitDetailViewModel
import com.thehouse.app.presentation.visits.VisitsEvent
import com.thehouse.app.presentation.visits.VisitsScreen
import com.thehouse.app.presentation.visits.VisitsViewModel
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
                        onPropertyClick = { id ->
                            navController.navigate(PropertyDetailRoute(id).route) {
                                launchSingleTop = true
                            }
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
                        onPropertyClick = { id ->
                            navController.navigate(PropertyDetailRoute(id).route) {
                                launchSingleTop = true
                            }
                        },
                        factory = factory,
                    )
                }
                composable(
                    route = PropertyDetailRoute.PATTERN,
                    arguments = listOf(navArgument(PropertyDetailRoute.ID_ARGUMENT) {
                        type = NavType.StringType
                    }),
                ) { entry ->
                    val id = entry.arguments?.getString(PropertyDetailRoute.ID_ARGUMENT).orEmpty()
                    val detailFactory = remember(factory, id) { factory.propertyDetail(id) }
                    PropertyDetailDestination(
                        propertyId = id,
                        factory = detailFactory,
                        onBack = { navController.popBackStack() },
                        onRequireLogin = {
                            navController.navigate(AppRoutes.login.route) { launchSingleTop = true }
                        },
                        onScheduleVisit = { propertyId ->
                            navController.navigate(ScheduleVisitRoute(propertyId).route) {
                                launchSingleTop = true
                            }
                        },
                        onOpenVisits = {
                            navController.navigate(AppRoutes.visits.route) {
                                launchSingleTop = true
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
                    val favVm: FavoritesViewModel = viewModel(factory = factory)
                    val favState by favVm.uiState.collectAsStateWithLifecycle()
                    LaunchedEffect(isAuthenticated) {
                        if (isAuthenticated) favVm.onEvent(FavoritesEvent.Load)
                    }
                    FavoritesScreen(
                        isAuthenticated = isAuthenticated,
                        isLoading = favState.isLoading,
                        items = favState.items,
                        error = favState.error,
                        removingId = favState.removingId,
                        onLoginClick = {
                            navController.navigate(AppRoutes.login.route) {
                                launchSingleTop = true
                            }
                        },
                        onExploreClick = {
                            navController.navigateToTab(BottomTab.Explorar)
                        },
                        onRemove = { id -> favVm.onEvent(FavoritesEvent.Remove(id)) },
                        onRetry = { favVm.onEvent(FavoritesEvent.Load) },
                    )
                }
                composable(AppRoutes.visits.route) {
                    if (!isAuthenticated) {
                        LaunchedEffect(Unit) {
                            navController.navigate(AppRoutes.login.route) { launchSingleTop = true }
                        }
                    } else {
                        val visitsVm: VisitsViewModel = viewModel(factory = factory)
                        val visitsState by visitsVm.uiState.collectAsStateWithLifecycle()
                        VisitsScreen(
                            state = visitsState,
                            onFilterChanged = { visitsVm.onEvent(VisitsEvent.FilterChanged(it)) },
                            onCancel = { visitsVm.onEvent(VisitsEvent.Cancel(it)) },
                            onVisitClick = { visitId ->
                                navController.navigate(VisitDetailRoute(visitId).route) {
                                    launchSingleTop = true
                                }
                            },
                            onRetry = { visitsVm.onEvent(VisitsEvent.Load) },
                        )
                    }
                }
                composable(
                    route = ScheduleVisitRoute.PATTERN,
                    arguments = listOf(navArgument(ScheduleVisitRoute.ID_ARGUMENT) {
                        type = NavType.StringType
                    }),
                ) { entry ->
                    val propertyId = entry.arguments?.getString(ScheduleVisitRoute.ID_ARGUMENT).orEmpty()
                    if (!isAuthenticated) {
                        LaunchedEffect(Unit) {
                            navController.navigate(AppRoutes.login.route) { launchSingleTop = true }
                        }
                    } else {
                        val scheduleFactory = remember(factory, propertyId) { factory.scheduleVisit(propertyId) }
                        ScheduleVisitDestination(
                            propertyId = propertyId,
                            factory = scheduleFactory,
                            onBack = { navController.popBackStack() },
                            onCreated = { visit ->
                                navController.navigate(VisitDetailRoute(visit.id).route) {
                                    popUpTo(AppRoutes.visits.route) { inclusive = false }
                                    launchSingleTop = true
                                }
                            },
                        )
                    }
                }
                composable(
                    route = VisitDetailRoute.PATTERN,
                    arguments = listOf(navArgument(VisitDetailRoute.ID_ARGUMENT) {
                        type = NavType.StringType
                    }),
                ) { entry ->
                    val visitId = entry.arguments?.getString(VisitDetailRoute.ID_ARGUMENT).orEmpty()
                    if (!isAuthenticated) {
                        LaunchedEffect(Unit) {
                            navController.navigate(AppRoutes.login.route) { launchSingleTop = true }
                        }
                    } else {
                        val detailFactory = remember(factory, visitId) { factory.visitDetail(visitId) }
                        val visitVm: VisitDetailViewModel = viewModel(
                            key = "visit-detail:$visitId",
                            factory = detailFactory,
                        )
                        val visitState by visitVm.uiState.collectAsStateWithLifecycle()
                        VisitDetailScreen(
                            state = visitState,
                            onBack = { navController.popBackStack() },
                            onRetry = { visitVm.onEvent(VisitDetailEvent.Load) },
                            onCancel = { visitVm.onEvent(VisitDetailEvent.Cancel) },
                        )
                    }
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
