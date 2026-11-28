package com.thehouse.app

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import com.thehouse.app.ui.theme.Spacing
import kotlinx.coroutines.launch
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
import com.thehouse.app.domain.push.NotificationDestination
import com.thehouse.app.presentation.U2ViewModelFactory
import com.thehouse.app.presentation.auth.LoginScreen
import com.thehouse.app.presentation.auth.RegisterScreen
import com.thehouse.app.presentation.components.BottomNavigation
import com.thehouse.app.presentation.explore.ExploreScreen
import com.thehouse.app.presentation.favorites.FavoritesEvent
import com.thehouse.app.presentation.favorites.FavoritesScreen
import com.thehouse.app.presentation.favorites.FavoritesViewModel
import com.thehouse.app.presentation.home.HomeScreen
import com.thehouse.app.presentation.navigation.AppDrawerContent
import com.thehouse.app.presentation.navigation.AppRoutes
import com.thehouse.app.presentation.navigation.ConversationRoute
import com.thehouse.app.presentation.navigation.CreateRequestRoute
import com.thehouse.app.presentation.navigation.LandlordRequestDetailRoute
import com.thehouse.app.presentation.navigation.PropertyDetailRoute
import com.thehouse.app.presentation.navigation.RoomDetailRoute
import com.thehouse.app.presentation.roomdetail.RoomDetailScreen
import com.thehouse.app.presentation.navigation.ReportRoute
import com.thehouse.app.presentation.navigation.AgreementPreviewRoute
import com.thehouse.app.presentation.navigation.CreateAgreementRoute as CreateAgreementNavRoute
import com.thehouse.app.presentation.navigation.LandlordAgreementDetailRoute
import com.thehouse.app.presentation.navigation.RequestDetailRoute
import com.thehouse.app.presentation.navigation.BuildingDetailRoute
import com.thehouse.app.presentation.navigation.BuildingFormRoute
import com.thehouse.app.presentation.navigation.RoomFormRoute
import com.thehouse.app.presentation.navigation.ScheduleVisitRoute
import com.thehouse.app.presentation.navigation.ScheduleRoomVisitRoute
import com.thehouse.app.presentation.navigation.VisitDetailRoute
import com.thehouse.app.presentation.navigation.BottomTab
import com.thehouse.app.presentation.navigation.TabNavigationState
import com.thehouse.app.presentation.notifications.NotificationsDestination
import com.thehouse.app.presentation.onboarding.OnboardingScreen
import com.thehouse.app.presentation.preferences.PreferencesDestination
import com.thehouse.app.presentation.privacy.PrivacyDestination
import com.thehouse.app.presentation.profile.ProfileDestination
import com.thehouse.app.presentation.quicksetup.QuickSetupDestination
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
        val container = (application as TheHouseApplication).container
        if (savedInstanceState == null) {
            // Cold start desde una notificación local (Fase B).
            container.pushNavigationIntentHandler.handle(intent)
        }
        val factory = U2ViewModelFactory(container)

        setContent {
            TheHouseTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TheHouseRoot(factory = factory)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // App ya abierta: FLAG_ACTIVITY_SINGLE_TOP + launchMode singleTop.
        (application as TheHouseApplication).container.pushNavigationIntentHandler.handle(intent)
    }
}

@Composable
fun TheHouseRoot(factory: U2ViewModelFactory, modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val rootViewModel: RootViewModel = viewModel(factory = factory)
    val rootState by rootViewModel.state.collectAsStateWithLifecycle()

    val container = (LocalContext.current.applicationContext as TheHouseApplication).container
    val sessionState by container.sessionStore.state.collectAsStateWithLifecycle()
    val pendingNotificationDestination by container.pendingNotificationNavigation.destination
        .collectAsStateWithLifecycle()
    val isAuthenticated = sessionState is SessionState.Active
    val isAccountSuspended = sessionState is SessionState.Suspended
    val displayName = (sessionState as? SessionState.Active)?.session?.displayName.orEmpty()
    LaunchedEffect(Unit) {
        rootViewModel.collect(container.sessionStore.state)
    }

    // D20: sincronizar favoritos reales (roomIds) al iniciar sesión/cada cambio de sesión.
    LaunchedEffect(isAuthenticated) { container.syncFavorites() }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val selectedTab = TabNavigationState.fromRoute(currentRoute)
    val showBottomNav = AppRoutes.isBottomTab(currentRoute) && !isAccountSuspended

    // D15: drawer global único en el shell. El hamburger solo aparece en las
    // pestañas bottom-tab, para no duplicar el back arrow de las pantallas de detalle.
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val drawerScope = rememberCoroutineScope()
    val showHamburger = showBottomNav
    val closeDrawer: () -> Unit = { drawerScope.launch { drawerState.close() } }
    val navigate: (String) -> Unit = { route ->
        navController.navigate(route) { launchSingleTop = true }
    }
    val logoutFromDrawer: () -> Unit = {
        drawerScope.launch {
            container.logout()
            // RootViewModel observa sessionStore y resuelve Home guest / Onboarding;
            // el cambio de targetRoute limpia el back stack con popUpTo(0).
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = showHamburger,
        drawerContent = {
            ModalDrawerSheet {
                AppDrawerContent(
                    isGuest = !isAuthenticated,
                    isLoggingOut = rootState.isLoggingOut,
                    onCloseDrawer = closeDrawer,
                    onRequireLogin = { navigate(AppRoutes.login.route) },
                    onOpenProfile = { navigate(AppRoutes.profile.route) },
                    onOpenPreferences = { navigate(AppRoutes.preferences.route) },
                    onOpenQuickSetup = { navigate(AppRoutes.quickSetup.route) },
                    onOpenNotifications = { navigate(AppRoutes.notifications.route) },
                    onOpenRequests = { navigate(AppRoutes.requests.route) },
                    onOpenConversations = { navigate(AppRoutes.conversations.route) },
                    onOpenMyRental = { navigate(AppRoutes.myRental.route) },
                    onOpenRentalCode = { navigate(AppRoutes.rentalCode.route) },
                    onOpenLandlordDashboard = { navigate(AppRoutes.landlordDashboard.route) },
                    onOpenLandlordRequests = { navigate(AppRoutes.landlordRequests.route) },
                    onOpenLandlordAgreements = { navigate(AppRoutes.landlordAgreements.route) },
                    onOpenPrivacy = { navigate(AppRoutes.privacy.route) },
                    onLogout = logoutFromDrawer,
                    selectedRoute = currentRoute,
                )
            }
        },
    ) {
    Scaffold(
        modifier = modifier,
        topBar = {
            if (showHamburger) {
                // D19: header global — [logo] TheHouse ............ [hamburger].
                // Un solo hamburger, alineado a la derecha, con insets de
                // status bar para no superponerse al reloj.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // BrandMark dibuja el techo + el texto "TheHouse".
                    com.thehouse.app.presentation.components.BrandMark()
                    IconButton(
                        onClick = { drawerScope.launch { drawerState.open() } },
                        modifier = Modifier.testTag("home_menu_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Menu,
                            contentDescription = "Abrir menú",
                        )
                    }
                }
            }
        },
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
                        onOpenPrivacy = {
                            navController.navigate(AppRoutes.privacy.route) { launchSingleTop = true }
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
                        onNavigateToSmartMatch = {
                            navController.navigateToTab(BottomTab.SmartMatch)
                        },
                        onPropertyClick = { id ->
                            navController.navigate(PropertyDetailRoute(id).route) {
                                launchSingleTop = true
                            }
                        },
                        factory = factory,
                        roomsEnabled = true,
                        onRoomClick = { roomId ->
                            if (currentRoute != RoomDetailRoute.PATTERN && RoomDetailRoute.isValid(roomId)) {
                                navController.navigate(RoomDetailRoute(roomId).route) { launchSingleTop = true }
                            }
                        },
                        onFavoriteRoomClick = { _ -> Unit },
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
                        onOpenMyRental = {
                            navController.navigate(AppRoutes.myRental.route) { launchSingleTop = true }
                        },
                        onOpenRentalCode = {
                            navController.navigate(AppRoutes.rentalCode.route) { launchSingleTop = true }
                        },
                        onOpenLandlordAgreements = {
                            navController.navigate(AppRoutes.landlordAgreements.route) { launchSingleTop = true }
                        },
                        onOpenPrivacy = {
                            navController.navigate(AppRoutes.privacy.route) { launchSingleTop = true }
                        },
                        onOpenProfile = {
                            navController.navigate(AppRoutes.profile.route) { launchSingleTop = true }
                        },
                        onOpenPreferences = {
                            navController.navigate(AppRoutes.preferences.route) { launchSingleTop = true }
                        },
                        onOpenQuickSetup = {
                            navController.navigate(AppRoutes.quickSetup.route) { launchSingleTop = true }
                        },
                        onOpenNotifications = {
                            navController.navigate(AppRoutes.notifications.route) { launchSingleTop = true }
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
                        roomsEnabled = true,
                        onRoomClick = { roomId ->
                            if (currentRoute != RoomDetailRoute.PATTERN && RoomDetailRoute.isValid(roomId)) {
                                navController.navigate(RoomDetailRoute(roomId).route) { launchSingleTop = true }
                            }
                        },
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
                        factory = factory,
                        onRequireLogin = {
                            navController.navigate(AppRoutes.login.route) { launchSingleTop = true }
                        },
                        onConfigurePreferences = { missing ->
                            if (!isAuthenticated) {
                                navController.navigate(AppRoutes.login.route) { launchSingleTop = true }
                            } else {
                                navController.navigate(AppRoutes.preferencesWithMissing(missing)) {
                                    launchSingleTop = true
                                }
                            }
                        },
                        onOpenProfile = {
                            if (!isAuthenticated) {
                                navController.navigate(AppRoutes.login.route) { launchSingleTop = true }
                            } else {
                                navController.navigate(AppRoutes.profile.route) { launchSingleTop = true }
                            }
                        },
                        // D18: el id de Smart Match es un roomId real.
                        // NUNCA se reutiliza como propertyId (GET /properties/{id}).
                        onRoomClick = { roomId ->
                            // Evita navegaciones duplicadas por pulsaciones rápidas.
                            if (currentRoute != RoomDetailRoute.PATTERN && RoomDetailRoute.isValid(roomId)) {
                                navController.navigate(RoomDetailRoute(roomId).route) { launchSingleTop = true }
                            }
                        },
                        onFavoriteClick = {
                            // D20: el toggle real lo ejecuta SmartMatchViewModel con roomId.
                            if (!isAuthenticated) {
                                navController.navigate(AppRoutes.login.route) { launchSingleTop = true }
                            }
                        },
                        onContactClick = { roomId ->
                            if (!isAuthenticated) {
                                navController.navigate(AppRoutes.login.route) { launchSingleTop = true }
                            } else if (RoomDetailRoute.isValid(roomId)) {
                                // D20: formulario real de solicitudes con roomId.
                                navController.navigate(CreateRequestRoute(roomId).route) { launchSingleTop = true }
                            }
                        },
                    )
                }
                composable(
                    route = RoomDetailRoute.PATTERN,
                    arguments = listOf(navArgument(RoomDetailRoute.ID_ARGUMENT) {
                        type = NavType.StringType
                    }),
                ) { entry ->
                    val roomId = entry.arguments?.getString(RoomDetailRoute.ID_ARGUMENT).orEmpty()
                    if (!RoomDetailRoute.isValid(roomId)) {
                        LaunchedEffect(Unit) { navController.popBackStack() }
                    } else {
                        val roomFactory = remember(factory, roomId) { factory.roomDetail(roomId) }
                        RoomDetailScreen(
                            roomId = roomId,
                            factory = roomFactory,
                            onBack = { navController.popBackStack() },
                            onRequireLogin = {
                                navController.navigate(AppRoutes.login.route) { launchSingleTop = true }
                            },
                            onContact = { id ->
                                if (!isAuthenticated) {
                                    navController.navigate(AppRoutes.login.route) { launchSingleTop = true }
                                } else {
                                    navController.navigate(CreateRequestRoute(id).route) { launchSingleTop = true }
                                }
                            },
                            onScheduleVisit = { id ->
                                if (!isAuthenticated) {
                                    navController.navigate(AppRoutes.login.route) { launchSingleTop = true }
                                } else {
                                    navController.navigate(ScheduleRoomVisitRoute(id).route) { launchSingleTop = true }
                                }
                            },
                        )
                    }
                }
                composable(AppRoutes.favorites.route) {
                    val favVm: FavoritesViewModel = viewModel(factory = factory)
                    val favState by favVm.uiState.collectAsStateWithLifecycle()
                    val favOnline by favVm.isOnline.collectAsStateWithLifecycle()
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
                        isOnline = favOnline,
                        onRoomClick = { roomId ->
                            if (currentRoute != RoomDetailRoute.PATTERN && RoomDetailRoute.isValid(roomId)) {
                                navController.navigate(RoomDetailRoute(roomId).route) { launchSingleTop = true }
                            }
                        },
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
                // D20: agendar visita con roomId real (XOR roomId/propertyId).
                composable(
                    route = ScheduleRoomVisitRoute.PATTERN,
                    arguments = listOf(navArgument(ScheduleRoomVisitRoute.ID_ARGUMENT) {
                        type = NavType.StringType
                    }),
                ) { entry ->
                    val roomId = entry.arguments?.getString(ScheduleRoomVisitRoute.ID_ARGUMENT).orEmpty()
                    if (!isAuthenticated || !RoomDetailRoute.isValid(roomId)) {
                        LaunchedEffect(Unit) {
                            navController.navigate(if (!isAuthenticated) AppRoutes.login.route else AppRoutes.visits.route) {
                                launchSingleTop = true
                            }
                        }
                    } else {
                        val roomScheduleFactory = remember(factory, roomId) { factory.scheduleRoomVisit(roomId) }
                        ScheduleVisitDestination(
                            propertyId = roomId,
                            factory = roomScheduleFactory,
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
                            onCreateAgreement = { reqId ->
                                navController.navigate(CreateAgreementNavRoute(reqId).route) {
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
                composable(AppRoutes.notifications.route) {
                    if (!isAuthenticated) {
                        LaunchedEffect(Unit) {
                            navController.navigate(AppRoutes.login.route) { launchSingleTop = true }
                        }
                    } else {
                        NotificationsDestination(
                            factory = factory,
                            onBack = { navController.popBackStack() },
                            onNavigateToRequest = { requestId ->
                                navController.navigate(RequestDetailRoute(requestId).route) {
                                    launchSingleTop = true
                                }
                            },
                            onNavigateToConversation = { conversationId ->
                                navController.navigate(ConversationRoute(conversationId).route) {
                                    launchSingleTop = true
                                }
                            },
                            onNavigateToVisit = { visitId ->
                                navController.navigate(VisitDetailRoute(visitId).route) {
                                    launchSingleTop = true
                                }
                            },
                            onNavigateToAgreement = {
                                // "Agreement Detail real": no existe ruta de detalle de acuerdo por id
                                // para estudiante; se navega a Mi renta (flujo real disponible).
                                navController.navigate(AppRoutes.myRental.route) { launchSingleTop = true }
                            },
                            onNavigateToRental = {
                                navController.navigate(AppRoutes.myRental.route) { launchSingleTop = true }
                            },
                            onNavigateToBuilding = { buildingId ->
                                navController.navigate(BuildingDetailRoute(buildingId).route) {
                                    launchSingleTop = true
                                }
                            },
                            onNavigateToRoom = { roomId ->
                                // NV-20 v1.3.0: id validado por el resolver central.
                                if (RoomDetailRoute.isValid(roomId)) {
                                    navController.navigate(RoomDetailRoute(roomId).route) {
                                        launchSingleTop = true
                                    }
                                }
                            },
                        )
                    }
                }
                composable(AppRoutes.privacy.route) {
                    // NV-15: ruta pública. La eliminación de cuenta solo se
                    // ofrece con sesión activa y contrato Backend confirmado.
                    PrivacyDestination(
                        factory = factory,
                        onBack = { navController.popBackStack() },
                        onRequireLogin = {
                            navController.navigate(AppRoutes.login.route) {
                                launchSingleTop = true
                            }
                        },
                        onAccountDeleted = {
                            rootViewModel.onAccountDeleted()
                            // Fuera del área privada y sin back hacia pantallas
                            // autenticadas de la cuenta eliminada.
                            navController.navigate(AppRoutes.login.route) {
                                popUpTo(0) { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                    )
                }
                composable(AppRoutes.profile.route) {
                    if (!isAuthenticated) {
                        LaunchedEffect(Unit) {
                            navController.navigate(AppRoutes.login.route) { launchSingleTop = true }
                        }
                    } else {
                        ProfileDestination(
                            factory = factory,
                            onBack = { navController.popBackStack() },
                            onRequireLogin = {
                                navController.navigate(AppRoutes.login.route) { launchSingleTop = true }
                            },
                            onOpenPreferences = {
                                navController.navigate(AppRoutes.preferences.route) { launchSingleTop = true }
                            },
                            onOpenQuickSetup = {
                                navController.navigate(AppRoutes.quickSetup.route) { launchSingleTop = true }
                            },
                        )
                    }
                }
                composable(
                    route = AppRoutes.PREFERENCES_PATTERN,
                    arguments = listOf(navArgument(AppRoutes.MISSING_ARGUMENT) {
                        type = NavType.StringType
                        defaultValue = ""
                    }),
                ) { entry ->
                    if (!isAuthenticated) {
                        LaunchedEffect(Unit) {
                            navController.navigate(AppRoutes.login.route) { launchSingleTop = true }
                        }
                    } else {
                        val missing = entry.arguments?.getString(AppRoutes.MISSING_ARGUMENT)
                            ?.split(',')
                            ?.map { it.trim() }
                            ?.filter { it.isNotBlank() }
                            ?: emptyList()
                        PreferencesDestination(
                            factory = factory,
                            onBack = { navController.popBackStack() },
                            onOpenProfile = {
                                navController.navigate(AppRoutes.profile.route) { launchSingleTop = true }
                            },
                            onOpenQuickSetup = {
                                navController.navigate(AppRoutes.quickSetup.route) { launchSingleTop = true }
                            },
                            missingFields = missing,
                        )
                    }
                }
                composable(AppRoutes.quickSetup.route) {
                    if (!isAuthenticated) {
                        LaunchedEffect(Unit) {
                            navController.navigate(AppRoutes.login.route) { launchSingleTop = true }
                        }
                    } else {
                        QuickSetupDestination(
                            factory = factory,
                            onBack = { navController.popBackStack() },
                            onContinue = {
                                navController.navigate(AppRoutes.home.route) {
                                    popUpTo(AppRoutes.home.route) { inclusive = false }
                                    launchSingleTop = true
                                }
                            },
                            onOpenPreferences = {
                                navController.navigate(AppRoutes.preferences.route) { launchSingleTop = true }
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
                            onOpenConversation = { conversationId ->
                                navController.navigate(ConversationRoute(conversationId).route) {
                                    launchSingleTop = true
                                }
                            },
                        )
                    }
                }
                composable(AppRoutes.myRental.route) {
                    if (!isAuthenticated) {
                        LaunchedEffect(Unit) {
                            navController.navigate(AppRoutes.login.route) { launchSingleTop = true }
                        }
                    } else {
                        com.thehouse.app.presentation.rental.MyRentalDestination(
                            factory = factory,
                            onBack = { navController.popBackStack() },
                            onRequireLogin = {
                                navController.navigate(AppRoutes.login.route) { launchSingleTop = true }
                            },
                            onOpenConversation = { conversationId ->
                                navController.navigate(ConversationRoute(conversationId).route) {
                                    launchSingleTop = true
                                }
                            },
                        )
                    }
                }
                composable(AppRoutes.rentalCode.route) {
                    if (!isAuthenticated) {
                        LaunchedEffect(Unit) {
                            navController.navigate(AppRoutes.login.route) { launchSingleTop = true }
                        }
                    } else {
                        val rf = remember(factory) { factory.rentalCode() }
                        com.thehouse.app.presentation.rental.RentalCodeDestination(
                            factory = rf,
                            onBack = { navController.popBackStack() },
                            onNavigateToPreview = { code ->
                                navController.navigate(AgreementPreviewRoute(code).route) {
                                    launchSingleTop = true
                                }
                            },
                        )
                    }
                }
                composable(
                    route = AgreementPreviewRoute.PATTERN,
                    arguments = listOf(navArgument(AgreementPreviewRoute.ID_ARGUMENT) { type = NavType.StringType }),
                ) { entry ->
                    val raw = entry.arguments?.getString(AgreementPreviewRoute.ID_ARGUMENT).orEmpty()
                    val code = AgreementPreviewRoute.decode(raw)
                    if (!isAuthenticated) {
                        LaunchedEffect(Unit) {
                            navController.navigate(AppRoutes.login.route) { launchSingleTop = true }
                        }
                    } else {
                        val pf = remember(factory, code) { factory.agreementPreview(code) }
                        com.thehouse.app.presentation.rental.AgreementPreviewDestination(
                            factory = pf,
                            onBack = { navController.popBackStack() },
                            onNavigateToMyRental = {
                                navController.navigate(AppRoutes.myRental.route) {
                                    popUpTo(AppRoutes.rentalCode.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                        )
                    }
                }
                composable(AppRoutes.landlordAgreements.route) {
                    if (!isAuthenticated) {
                        LaunchedEffect(Unit) {
                            navController.navigate(AppRoutes.login.route) { launchSingleTop = true }
                        }
                    } else {
                        com.thehouse.app.presentation.landlord.LandlordAgreementsDestination(
                            factory = factory,
                            onBack = { navController.popBackStack() },
                            onOpenDetail = { id ->
                                navController.navigate(LandlordAgreementDetailRoute(id).route) {
                                    launchSingleTop = true
                                }
                            },
                        )
                    }
                }
                composable(
                    route = LandlordAgreementDetailRoute.PATTERN,
                    arguments = listOf(navArgument(LandlordAgreementDetailRoute.ID_ARGUMENT) { type = NavType.StringType }),
                ) { entry ->
                    val id = entry.arguments?.getString(LandlordAgreementDetailRoute.ID_ARGUMENT).orEmpty()
                    if (!isAuthenticated) {
                        LaunchedEffect(Unit) {
                            navController.navigate(AppRoutes.login.route) { launchSingleTop = true }
                        }
                    } else {
                        val df = remember(factory, id) { factory.landlordAgreementDetail(id) }
                        com.thehouse.app.presentation.landlord.LandlordAgreementDetailDestination(
                            factory = df,
                            onBack = { navController.popBackStack() },
                        )
                    }
                }
                composable(
                    route = CreateAgreementNavRoute.PATTERN,
                    arguments = listOf(navArgument(CreateAgreementNavRoute.ID_ARGUMENT) { type = NavType.StringType }),
                ) { entry ->
                    val requestId = entry.arguments?.getString(CreateAgreementNavRoute.ID_ARGUMENT).orEmpty()
                    if (!isAuthenticated) {
                        LaunchedEffect(Unit) {
                            navController.navigate(AppRoutes.login.route) { launchSingleTop = true }
                        }
                    } else {
                        val cf = remember(factory, requestId) { factory.createAgreement(requestId) }
                        com.thehouse.app.presentation.landlord.CreateAgreementDestination(
                            factory = cf,
                            onBack = { navController.popBackStack() },
                            onDone = { navController.popBackStack() },
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

            // NV-14 Fase B: target de push tras resolverse root/sesión.
            // Se declara después del efecto de root para que su navegación
            // no sobrescriba el destino de la notificación.
            LaunchedEffect(
                pendingNotificationDestination,
                isAuthenticated,
                isAccountSuspended,
                rootState.isResolving,
            ) {
                val destination = pendingNotificationDestination ?: return@LaunchedEffect
                if (rootState.isResolving || isAccountSuspended) return@LaunchedEffect
                if (!isAuthenticated) {
                    // No se consume: el target se reanuda tras Login.
                    navController.navigate(AppRoutes.login.route) { launchSingleTop = true }
                    return@LaunchedEffect
                }
                navController.navigate(destination.toRoute()) { launchSingleTop = true }
                container.pendingNotificationNavigation.consume()
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

/**
 * Único mapeo destino → ruta real existente. Sin rutas inventadas:
 * agreement/rental → Mi renta (decisión Fase A).
 */
private fun NotificationDestination.toRoute(): String = when (this) {
    is NotificationDestination.Request -> RequestDetailRoute(id).route
    is NotificationDestination.Conversation -> ConversationRoute(id).route
    is NotificationDestination.Visit -> VisitDetailRoute(id).route
    NotificationDestination.Agreement,
    NotificationDestination.Rental -> AppRoutes.myRental.route
    is NotificationDestination.Building -> BuildingDetailRoute(id).route
    // NV-20 v1.3.0: room abre el detalle REAL del cuarto (GET /rooms/{id}).
    // Nunca PropertyDetailRoute ni propertyId; el id ya fue validado por el
    // resolver centralizado (SAFE_ID) antes de llegar aquí.
    is NotificationDestination.Room -> if (RoomDetailRoute.isValid(id)) {
        RoomDetailRoute(id).route
    } else {
        AppRoutes.notifications.route
    }
    NotificationDestination.NotificationCenter -> AppRoutes.notifications.route
}
