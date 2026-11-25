package com.thehouse.app.presentation.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thehouse.app.presentation.U2ViewModelFactory
import com.thehouse.app.presentation.catalog.CatalogLoadState
import com.thehouse.app.presentation.catalog.errorMessage
import com.thehouse.app.presentation.components.Avatar
import com.thehouse.app.presentation.components.BrandMark
import com.thehouse.app.presentation.components.PropertyCard
import com.thehouse.app.presentation.components.SectionHeader
import com.thehouse.app.presentation.components.StatusChip
import com.thehouse.app.presentation.components.StatusTone
import com.thehouse.app.presentation.components.TheHouseCard
import com.thehouse.app.presentation.components.TheHousePrimaryButton
import com.thehouse.app.presentation.components.TheHouseSearchBar
import com.thehouse.app.presentation.components.TheHouseSecondaryButton
import com.thehouse.app.ui.theme.IconSizes
import com.thehouse.app.ui.theme.Spacing

/**
 * Home visual (D4 diseño + D5 datos reales, NV-05/NV-06/NV-07).
 *
 * - Universidad seleccionada REAL (lista backend + persistencia local;
 *   sin UTTT hardcodeada productiva).
 * - Propiedad destacada REAL = primera disponible (regla UI local,
 *   no marcada por el backend).
 * - Solo campos del contrato (sin Match/verificación/distancia inventados).
 * - Estados loading/empty/error + retry para universidades y propiedades.
 * - Guest y Auth comparten shell; acciones protegidas → Login.
 *
 * La barra inferior vive en el shell (MainActivity D4).
 */
@Composable
fun HomeScreen(
    displayName: String,
    onLoggedOut: () -> Unit,
    onRequireLogin: () -> Unit,
    onNavigateToExplore: () -> Unit,
    onPropertyClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    factory: U2ViewModelFactory,
    onOpenRequests: () -> Unit = {},
    onOpenConversations: () -> Unit = {},
    onOpenLandlordRequests: () -> Unit = {},
    onOpenLandlord: () -> Unit = {},
    onOpenMyRental: () -> Unit = {},
    onOpenRentalCode: () -> Unit = {},
    onOpenLandlordAgreements: () -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    onOpenPrivacy: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    onOpenPreferences: () -> Unit = {},
    onOpenQuickSetup: () -> Unit = {},
) {
    val viewModel: HomeViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsState()
    var showUniversityPicker by remember { mutableStateOf(false) }
    val drawerState = remember {DrawerState()}

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                HomeEffect.LoggedOut -> onLoggedOut()
                HomeEffect.RequireLogin -> onRequireLogin()
            }
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        viewModel.onEvent(HomeEvent.StartUnreadPolling)
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        viewModel.onEvent(HomeEvent.StopUnreadPolling)
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onEvent(HomeEvent.RefreshUnread)
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        drawerState = drawerState,
        drawerContent = {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(Spacing.xl),
                horizontalAlignment = Alignment.Start,
                padding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.lg),
            ) {
                Text(
                    text = "Menú",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = Spacing.lg),
                )
                // Destinos del drawer
                OutlinedTextButton(
                    onClick = { onOpenProfile() },
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.button,
                ) {
                    Text(text = "Perfil", modifier = Modifier.padding(end = Spacing.sm))
                }
                OutlinedTextButton(
                    onClick = { onOpenPreferences() },
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.button,
                ) {
                    Text(text = "Preferencias", modifier = Modifier.padding(end = Spacing.sm))
                }
                OutlinedTextButton(
                    onClick = { onLogout() },
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.button,
                    enabled = !state.isLoggingOut,
                    colors = ButtonColors(
                        defaultColor = MaterialTheme.colorScheme.error,
                        highlitColor = MaterialTheme.colorScheme.errorContainer,
                    ),
                ) {
                    Text(
                        text = if (state.isLoggingOut) "Cerrando sesión…" else "Cerrar sesión",
                        modifier = Modifier.padding(end = Spacing.sm),
                    )
                }
                Spacer(modifier = Modifier.height(Spacing.xl))
                OutlinedTextButton(
                    onClick = { onRequireLogin() },
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.button,
                ) {
                    Text(text = "Iniciar sesión o registrarse", modifier = Modifier.padding(end = Spacing.sm))
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Spacing.xl, vertical = Spacing.lg)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.xl),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BrandMark()
                IconButton(
                    onClick = { drawerState.open() },
                    modifier = Modifier.semantics { contentDescription = "Abrir menú" }
                        .contentSemantics { role = Role.Button }
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Abrir menú",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text(
                    text = if (state.isGuest) {
                        "Hola, invitado 👋"
                    } else if (displayName.isNotBlank()) {
                        "Hola, $displayName 👋"
                    } else {
                        "Hola 👋"
                    },
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.semantics {
                        contentDescription = "Saludo de inicio"
                    },
                )
                Text(
                    text = if (state.isGuest) {
                        "Explorando como invitado. Inicia sesión para guardar favoritos y contactar."
                    } else {
                        "Encuentra tu próximo espacio cerca de tu universidad."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            TheHouseSearchBar(
                hint = "Busca rentas por título o ciudad",
                onClick = onNavigateToExplore,
            )

            UniversitySection(
                state = state,
                showPicker = showUniversityPicker,
                onTogglePicker = { showUniversityPicker = !showUniversityPicker },
                onSelect = { id ->
                    viewModel.onEvent(HomeEvent.UniversitySelected(id))
                    showUniversityPicker = false
                },
                onRetry = { viewModel.onEvent(HomeEvent.RetryUniversitiesClicked) },
            )

            SmartMatchPromo()

            SectionHeader(
                title = "Recomendados para ti",
                subtitle = "Opciones reales cerca de tu universidad",
                actionLabel = "Ver todas",
                onAction = onNavigateToExplore,
            )

            when (val properties = state.properties) {
                CatalogLoadState.Loading -> LoadingRow(text = "Buscando rentas…")
                CatalogLoadState.Empty -> EmptyMessage("Sin propiedades disponibles.")
                is CatalogLoadState.Error -> ErrorMessage(
                    message = properties.errorMessage() ?: "Algo salió mal.",
                    onRetry = { viewModel.onEvent(HomeEvent.RetryPropertiesClicked) },
                )
                is CatalogLoadState.Success -> {
                    val featured = state.featuredProperty
                    if (featured != null) {
                        PropertyCard(
                            property = featured,
                            onFavoriteClick = { viewModel.onEvent(HomeEvent.FavoriteClicked) },
                            onContactClick = { viewModel.onEvent(HomeEvent.ContactClicked) },
                            onPropertyClick = { onPropertyClick(featured.id) },
                        )
                    }
                }
            }

            TheHouseSecondaryButton(
                text = "Publicar mi renta",
                onClick = { viewModel.onEvent(HomeEvent.PublishClicked) },
            )

            if (state.isGuest) {
                TheHouseCard {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                        Text(
                            text = "Tu cuenta",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "Inicia sesión para guardar favoritos, contactar arrendadores y dar seguimiento a tus solicitudes.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        TheHousePrimaryButton(
                            text = "Iniciar sesión",
                            onClick = onRequireLogin,
                        )
                    }
                }
            } else {
                AccountActionsCard(
                    unreadNotifications = state.unreadNotifications,
                    isLoggingOut = state.isLoggingOut,
                    onOpenProfile = onOpenProfile,
                    onOpenPreferences = onOpenPreferences,
                    onOpenQuickSetup = onOpenQuickSetup,
                    onOpenNotifications = onOpenNotifications,
                    onOpenRequests = onOpenRequests,
                    onOpenConversations = onOpenConversations,
                    onOpenLandlordRequests = onOpenLandlordRequests,
                    onOpenLandlord = onOpenLandlord,
                    onOpenMyRental = onOpenMyRental,
                    onOpenRentalCode = onOpenRentalCode,
                    onOpenLandlordAgreements = onOpenLandlordAgreements,
                    onLogout = { viewModel.onEvent(HomeEvent.LogoutClicked) },
                )
            }

            // NV-15: acceso a privacidad desde el área de cuenta, disponible
            // también como invitado (sin sesión no hay eliminación de cuenta).
            TheHouseCard(onClick = onOpenPrivacy) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    ActionIcon(Icons.Outlined.PrivacyTip)
                    Text(
                        text = "Privacidad",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun UniversitySection(
    state: HomeUiState,
    showPicker: Boolean,
    onTogglePicker: () -> Unit,
    onSelect: (String) -> Unit,
    onRetry: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(
            text = "Universidad",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        when (val universities = state.universities) {
            CatalogLoadState.Loading -> LoadingRow(text = "Cargando universidades…")
            CatalogLoadState.Empty -> EmptyMessage("Sin universidades disponibles.")
            is CatalogLoadState.Error -> ErrorMessage(
                message = universities.errorMessage() ?: "Algo salió mal.",
                onRetry = onRetry,
            )
            is CatalogLoadState.Success -> {
                val selected = universities.items.firstOrNull { it.id == state.selectedUniversityId }
                TheHouseCard {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                        ) {
                            ActionIcon(Icons.Rounded.LocationOn)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = selected?.name ?: "Selecciona tu universidad",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                if (selected != null) {
                                    Text(
                                        text = selected.city,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            TextButton(onClick = onTogglePicker) {
                                Text(if (selected != null) "Cambiar" else "Elegir")
                            }
                        }
                        if (showPicker) {
                            universities.items.forEach { university ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onSelect(university.id) }
                                        .heightIn(min = 48.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    RadioButton(
                                        selected = university.id == state.selectedUniversityId,
                                        onClick = { onSelect(university.id) },
                                    )
                                    Column {
                                        Text(
                                            text = university.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                        )
                                        Text(
                                            text = university.city,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SmartMatchPromo() {
    TheHouseCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            ActionIcon(Icons.Rounded.AutoAwesome)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Smart Match",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Ver compatibilidad en la pestaña Smart Match.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            StatusChip(label = "Nuevo", tone = StatusTone.Primary)
        }
    }
}

@Composable
private fun AccountActionsCard(
    unreadNotifications: Int,
    isLoggingOut: Boolean,
    onOpenProfile: () -> Unit,
    onOpenPreferences: () -> Unit,
    onOpenQuickSetup: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenRequests: () -> Unit,
    onOpenConversations: () -> Unit,
    onOpenLandlordRequests: () -> Unit,
    onOpenLandlord: () -> Unit,
    onOpenMyRental: () -> Unit,
    onOpenRentalCode: () -> Unit,
    onOpenLandlordAgreements: () -> Unit,
    onLogout: () -> Unit,
) {
    TheHouseCard {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(
                text = "Tu cuenta",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            AccountActionRow(
                icon = Icons.Outlined.Person,
                label = "Mi perfil",
                onClick = onOpenProfile,
            )
            AccountActionRow(
                icon = Icons.Outlined.Tune,
                label = "Preferencias",
                onClick = onOpenPreferences,
            )
            AccountActionRow(
                icon = Icons.Outlined.Home,
                label = "Configuración inicial",
                onClick = onOpenQuickSetup,
            )
            AccountActionRow(
                icon = Icons.Outlined.Notifications,
                label = "Notificaciones",
                badge = if (unreadNotifications > 0) unreadNotifications.toString() else null,
                onClick = onOpenNotifications,
            )
            AccountActionRow(Icons.Outlined.Description, "Mis solicitudes", onClick = onOpenRequests)
            AccountActionRow(Icons.AutoMirrored.Outlined.Chat, "Conversaciones", onClick = onOpenConversations)
            AccountActionRow(Icons.Outlined.Home, "Mi renta", onClick = onOpenMyRental)
            AccountActionRow(Icons.Outlined.VpnKey, "Introducir código", onClick = onOpenRentalCode)
            AccountActionRow(
                icon = Icons.Outlined.Apartment,
                label = "Mis alojamientos",
                supporting = "Modo arrendador",
                onClick = onOpenLandlord,
            )
            AccountActionRow(
                icon = Icons.Outlined.Description,
                label = "Solicitudes recibidas",
                supporting = "Modo arrendador",
                onClick = onOpenLandlordRequests,
            )
            AccountActionRow(
                icon = Icons.Outlined.Receipt,
                label = "Mis acuerdos",
                supporting = "Modo arrendador",
                onClick = onOpenLandlordAgreements,
            )
            AccountActionRow(
                icon = Icons.AutoMirrored.Outlined.Logout,
                label = if (isLoggingOut) "Cerrando sesión…" else "Cerrar sesión",
                onClick = onLogout,
                enabled = !isLoggingOut,
                danger = true,
            )
        }
    }
}

@Composable
private fun AccountActionRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    supporting: String? = null,
    badge: String? = null,
    enabled: Boolean = true,
    danger: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .heightIn(min = 52.dp)
            .padding(vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        ActionIcon(
            icon = icon,
            tint = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
            if (supporting != null) {
                Text(
                    text = supporting,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (badge != null) {
            StatusChip(label = badge, tone = StatusTone.Primary)
        }
    }
}

@Composable
private fun ActionIcon(
    icon: ImageVector,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
) {
    Row(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .padding(0.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(IconSizes.md),
        )
    }
}

@Composable
private fun LoadingRow(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(IconSizes.md),
            strokeWidth = 2.dp,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EmptyMessage(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ErrorMessage(message: String, onRetry: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
        TextButton(onClick = onRetry) {
            Text("Reintentar")
        }
    }
}