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
import com.thehouse.app.domain.model.ReportTargetType
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
import com.thehouse.app.presentation.navigation.ConversationRoute
import com.thehouse.app.presentation.navigation.CreateRequestRoute
import com.thehouse.app.presentation.navigation.LandlordRequestDetailRoute
import com.thehouse.app.presentation.navigation.PropertyDetailRoute
import com.thehouse.app.presentation.navigation.ReportRoute
import com.thehouse.app.presentation.navigation.RequestDetailRoute
import com.thehouse.app.presentation.navigation.BuildingDetailRoute
import com.thehouse.app.presentation.navigation.BuildingFormRoute
import com.thehouse.app.presentation.navigation.RoomFormRoute
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
    val isAccountSuspended = sessionState is SessionState.Suspended
    val displayName = (sessionState as? SessionState.Active)?.session?.displayName.orEmpty()

    LaunchedEffect(Unit) {
        rootViewModel.collect(container.sessionStore.state)
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val selectedTab = TabNavigationState.fromRoute(currentRoute)
    val showBottomNav = AppRoutes.isBottomTab(currentRoute) && !isAccountSuspended

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
        if (isAccountSuspended) {
            com.thehouse.app.presentation.auth.AccountSuspendedScreen(
                isLoggingOut = rootState.isLoggingOut,
                onLogout = rootViewModel::logoutSuspendedAccount,
                modifier = Modifier.padding(padding),
            )
        } else if (rootState.isResolving) {
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
                        onOpenRequests = {
                            navController.navigate(AppRoutes.requests.route) { launchSingleTop = true }
                        },
                        onOpenConversations = {
                            navController.navigate(AppRoutes.conversations.route) { launchSingleTop = true }
                        },
                        onOpenLandlordRequests = {
                            navController.navigate(AppRoutes.landlordRequests.route) { launchSingleTop = true }
                        },
                        onOpenLandlord = {
                            navController.navigate(AppRoutes.landlordDashboard.route) { launchSingleTop = true }
                        },
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
                        onOpenRequests = {
                            navController.navigate(AppRoutes.requests.route) {
                                launchSingleTop = true
                            }
                        },
                        onMakeRequest = { roomId ->
                            navController.navigate(CreateRequestRoute(roomId).route) {
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
                composable(AppRoutes.requests.route) {
                    if (!isAuthenticated) {
                        LaunchedEffect(Unit) {
                            navController.navigate(AppRoutes.login.route) { launchSingleTop = true }
                        }
                    } else {
                        com.thehouse.app.presentation.requests.RequestsDestination(
                            factory = factory,
                            onBack = { navController.popBackStack() },
                            onRequestClick = { requestId ->
                                navController.navigate(RequestDetailRoute(requestId).route) {
                                    launchSingleTop = true
                                }
                            },
                        )
                    }
                }
                composable(
                    route = RequestDetailRoute.PATTERN,
                    arguments = listOf(navArgument(RequestDetailRoute.ID_ARGUMENT) {
                        type = NavType.StringType
                    }),
                ) { entry ->
                    val requestId = entry.arguments?.getString(RequestDetailRoute.ID_ARGUMENT).orEmpty()
                    if (!isAuthenticated) {
                        LaunchedEffect(Unit) {
                            navController.navigate(AppRoutes.login.route) { launchSingleTop = true }
                        }
                    } else {
                        val requestFactory = remember(factory, requestId) { factory.requestDetail(requestId) }
                        com.thehouse.app.presentation.requests.RequestDetailDestination(
                            requestId = requestId,
                            factory = requestFactory,
                            onBack = { navController.popBackStack() },
                            onOpenConversation = { conversationId ->
                                navController.navigate(ConversationRoute(conversationId).route) {
                                    launchSingleTop = true
                                }
                            },
                            onReportRoom = { roomId ->
                                navController.navigate(
                                    ReportRoute(ReportTargetType.ROOM, roomId).route,
                                ) {
                                    launchSingleTop = true
                                }
                            },
                            onReportBuilding = { buildingId ->
                                navController.navigate(
                                    ReportRoute(ReportTargetType.BUILDING, buildingId).route,
                                ) {
                                    launchSingleTop = true
                                }
                            },
                        )
                    }
                }
                composable(
                    route = ReportRoute.PATTERN,
                    arguments = listOf(
                        navArgument(ReportRoute.TYPE_ARGUMENT) { type = NavType.StringType },
                        navArgument(ReportRoute.ID_ARGUMENT) { type = NavType.StringType },
                    ),
                ) { entry ->
                    val targetTypeApi = entry.arguments?.getString(ReportRoute.TYPE_ARGUMENT).orEmpty()
                    val targetId = entry.arguments?.getString(ReportRoute.ID_ARGUMENT).orEmpty()
                    val targetType = ReportTargetType.entries.firstOrNull { it.api == targetTypeApi }
                    if (!isAuthenticated) {
                        LaunchedEffect(Unit) {
                            navController.navigate(AppRoutes.login.route) { launchSingleTop = true }
                        }
                    } else if (targetType == null || targetId.isEmpty()) {
                        // Ruta D13 validada: sin target válido no hay reporte (sin fallback inventado).
                        LaunchedEffect(Unit) { navController.popBackStack() }
                    } else {
                        com.thehouse.app.presentation.reports.ReportDestination(
                            targetType = targetType,
                            targetId = targetId,
                            factory = factory,
                            onBack = { navController.popBackStack() },
                        )
                    }
                }
                composable(AppRoutes.landlordRequests.route) {
                    if (!isAuthenticated) {
                        LaunchedEffect(Unit) {
                            navController.navigate(AppRoutes.login.route) { launchSingleTop = true }
                        }
                    } else {
                        com.thehouse.app.presentation.landlord.LandlordRequestsDestination(
                            factory = factory,
                            onBack = { navController.popBackStack() },
                            onRequestClick = { requestId ->
                                navController.navigate(LandlordRequestDetailRoute(requestId).route) {
                                    launchSingleTop = true
                                }
                            },
                        )
                    }
                }
                composable(
                    route = LandlordRequestDetailRoute.PATTERN,
                    arguments = listOf(navArgument(LandlordRequestDetailRoute.ID_ARGUMENT) {
                        type = NavType.StringType
                    }),
                ) { entry ->
                    val requestId = entry.arguments?.getString(LandlordRequestDetailRoute.ID_ARGUMENT).orEmpty()
                    if (!isAuthenticated) {
                        LaunchedEffect(Unit) {
                            navController.navigate(AppRoutes.login.route) { launchSingleTop = true }
                        }
                    } else {
                        val requestFactory = remember(factory, requestId) { factory.landlordRequestDetail(requestId) }
                        com.thehouse.app.presentation.landlord.LandlordRequestDetailDestination(
                            requestId = requestId,
                            factory = requestFactory,
                            onBack = { navController.popBackStack() },
                            onOpenConversation = { conversationId ->
                                navController.navigate(ConversationRoute(conversationId).route) {
                                    launchSingleTop = true
                                }
                            },
                        )
                    }
                }
                composable(AppRoutes.landlordDashboard.route) {
                    if (!isAuthenticated) {
                        LaunchedEffect(Unit) {
                            navController.navigate(AppRoutes.login.route) { launchSingleTop = true }
                        }
                    } else {
                        com.thehouse.app.presentation.landlord.LandlordDashboardDestination(
                            factory = factory,
                            onBack = { navController.popBackStack() },
                            onCreateBuilding = {
                                navController.navigate(
                                    com.thehouse.app.presentation.navigation.BuildingFormRoute(null).route,
                                ) { launchSingleTop = true }
                            },
                            onBuildingClick = { buildingId ->
                                navController.navigate(
                                    com.thehouse.app.presentation.navigation.BuildingDetailRoute(buildingId).route,
                                ) { launchSingleTop = true }
                            },
                            onOpenLandlordRequests = {
                                navController.navigate(AppRoutes.landlordRequests.route) {
                                    launchSingleTop = true
                                }
                            },
                            onActivated = {
                                navController.navigate(AppRoutes.landlordDashboard.route) {
                                    popUpTo(AppRoutes.home.route) { inclusive = false }
                                    launchSingleTop = true
                                }
                            },
                        )
                    }
                }
                composable("arrendador/edificio/nuevo") {
                    if (!isAuthenticated) {
                        LaunchedEffect(Unit) {
                            navController.navigate(AppRoutes.login.route) { launchSingleTop = true }
                        }
                    } else {
                        com.thehouse.app.presentation.landlord.BuildingFormDestination(
                            buildingId = null,
                            factory = factory,
                            onBack = { navController.popBackStack() },
                            onSaved = { buildingId ->
                                navController.navigate(
                                    com.thehouse.app.presentation.navigation.BuildingDetailRoute(buildingId).route,
                                ) {
                                    popUpTo(AppRoutes.landlordDashboard.route) { inclusive = false }
                                    launchSingleTop = true
                                }
                            },
                        )
                    }
                }
                composable(
                    route = com.thehouse.app.presentation.navigation.BuildingFormRoute.PATTERN,
                    arguments = listOf(navArgument(BuildingFormRoute.ID_ARGUMENT) {
                        type = NavType.StringType
                    }),
                ) { entry ->
                    val buildingId = entry.arguments?.getString(BuildingFormRoute.ID_ARGUMENT).orEmpty()
                    if (!isAuthenticated) {
                        LaunchedEffect(Unit) {
                            navController.navigate(AppRoutes.login.route) { launchSingleTop = true }
                        }
                    } else {
                        com.thehouse.app.presentation.landlord.BuildingFormDestination(
                            buildingId = buildingId,
                            factory = factory,
                            onBack = { navController.popBackStack() },
                            onSaved = {
                                navController.popBackStack()
                            },
                        )
                    }
                }
                composable(
                    route = com.thehouse.app.presentation.navigation.BuildingDetailRoute.PATTERN,
                    arguments = listOf(navArgument(BuildingDetailRoute.ID_ARGUMENT) {
                        type = NavType.StringType
                    }),
                ) { entry ->
                    val buildingId = entry.arguments?.getString(BuildingDetailRoute.ID_ARGUMENT).orEmpty()
                    if (!isAuthenticated) {
                        LaunchedEffect(Unit) {
                            navController.navigate(AppRoutes.login.route) { launchSingleTop = true }
                        }
                    } else {
                        com.thehouse.app.presentation.landlord.BuildingDetailDestination(
                            buildingId = buildingId,
                            factory = factory,
                            onBack = { navController.popBackStack() },
                            onEditBuilding = { id ->
                                navController.navigate(BuildingFormRoute(id).route) {
                                    launchSingleTop = true
                                }
                            },
                            onAddRooms = { id ->
                                navController.navigate(
                                    com.thehouse.app.presentation.navigation.RoomFormRoute(id, null).route,
                                ) { launchSingleTop = true }
                            },
                            onEditRoom = { id, roomId ->
                                navController.navigate(
                                    com.thehouse.app.presentation.navigation.RoomFormRoute(id, roomId).route,
                                ) { launchSingleTop = true }
                            },
                            onDeleted = { navController.popBackStack() },
                            onOpenLandlordRequests = {
                                navController.navigate(AppRoutes.landlordRequests.route) {
                                    launchSingleTop = true
                                }
                            },
                        )
                    }
                }
                composable(
                    route = com.thehouse.app.presentation.navigation.RoomFormRoute.PATTERN,
                    arguments = listOf(
                        navArgument(RoomFormRoute.BUILDING_ARGUMENT) { type = NavType.StringType },
                        navArgument(RoomFormRoute.ROOM_ARGUMENT) { type = NavType.StringType },
                    ),
                ) { entry ->
                    val buildingId = entry.arguments?.getString(RoomFormRoute.BUILDING_ARGUMENT).orEmpty()
                    val roomId = entry.arguments?.getString(RoomFormRoute.ROOM_ARGUMENT)
                        ?.takeIf { it != "nuevo" }
                    if (!isAuthenticated) {
                        LaunchedEffect(Unit) {
                            navController.navigate(AppRoutes.login.route) { launchSingleTop = true }
                        }
                    } else {
                        com.thehouse.app.presentation.landlord.RoomFormDestination(
                            buildingId = buildingId,
                            roomId = roomId,
                            factory = factory,
                            onBack = { navController.popBackStack() },
                            onSaved = { navController.popBackStack() },
                        )
                    }
                }
                composable(AppRoutes.conversations.route) {
                    if (!isAuthenticated) {
                        LaunchedEffect(Unit) {
                            navController.navigate(AppRoutes.login.route) { launchSingleTop = true }
                        }
                    } else {
                        com.thehouse.app.presentation.chat.ConversationsDestination(
                            factory = factory,
                            onBack = { navController.popBackStack() },
                            onConversationClick = { conversationId ->
                                navController.navigate(ConversationRoute(conversationId).route) {
                                    launchSingleTop = true
                                }
                            },
                        )
                    }
                }
                composable(
                    route = CreateRequestRoute.PATTERN,
                    arguments = listOf(navArgument(CreateRequestRoute.ID_ARGUMENT) {
                        type = NavType.StringType
                    }),
                ) { entry ->
                    val roomId = entry.arguments?.getString(CreateRequestRoute.ID_ARGUMENT).orEmpty()
                    if (!isAuthenticated) {
                        LaunchedEffect(Unit) {
                            navController.navigate(AppRoutes.login.route) { launchSingleTop = true }
                        }
                    } else {
                        val createFactory = remember(factory, roomId) { factory.createRequest(roomId) }
                        com.thehouse.app.presentation.requests.CreateRequestDestination(
                            roomId = roomId,
                            factory = createFactory,
                            onBack = { navController.popBackStack() },
                            onCreated = { request ->
                                navController.navigate(RequestDetailRoute(request.id).route) {
                                    launchSingleTop = true
                                }
                            },
                        )
                    }
                }
                composable(
                    route = ConversationRoute.PATTERN,
                    arguments = listOf(
                        navArgument(ConversationRoute.ID_ARGUMENT) { type = NavType.StringType },
                        navArgument("closed") {
                            type = NavType.BoolType
                            defaultValue = false
                        },
                    ),
                ) { entry ->
                    val conversationId =
                        entry.arguments?.getString(ConversationRoute.ID_ARGUMENT).orEmpty()
                    val initialClosed = entry.arguments?.getBoolean("closed") ?: false
                    if (!isAuthenticated) {
                        LaunchedEffect(Unit) {
                            navController.navigate(AppRoutes.login.route) { launchSingleTop = true }
                        }
                    } else {
                        val conversationFactory =
                            remember(factory, conversationId) { factory.conversation(conversationId, initialClosed) }
                        com.thehouse.app.presentation.chat.ConversationDestination(
                            conversationId = conversationId,
                            initialClosed = initialClosed,
                            factory = conversationFactory,
                            onBack = { navController.popBackStack() },
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
