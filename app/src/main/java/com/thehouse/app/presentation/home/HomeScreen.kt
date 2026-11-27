package com.thehouse.app.presentation.home

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.AddHome
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thehouse.app.presentation.U2ViewModelFactory
import com.thehouse.app.presentation.catalog.CatalogLoadState
import com.thehouse.app.presentation.catalog.RoomSearchState
import com.thehouse.app.presentation.catalog.errorMessage
import com.thehouse.app.presentation.components.PropertyCard
import com.thehouse.app.presentation.components.RoomCard
import com.thehouse.app.presentation.components.SectionHeader
import com.thehouse.app.presentation.components.StatusChip
import com.thehouse.app.presentation.components.StatusTone
import com.thehouse.app.presentation.components.TheHouseCard
import com.thehouse.app.presentation.components.TheHouseSearchBar
import com.thehouse.app.presentation.components.TheHouseTextAction
import com.thehouse.app.ui.theme.Elevations
import com.thehouse.app.ui.theme.IconSizes
import com.thehouse.app.ui.theme.Spacing

/**
 * Home visual (D4/D5 + D19.1 + D20). Diseño aprobado preservado: un solo header
 * en el shell, saludo, universidad, buscador, Smart Match, recomendados, bottom nav.
 * D20: los recomendados son habitaciones reales de `rooms/search` con roomId real.
 */
@Composable
fun HomeScreen(
    displayName: String,
    onLoggedOut: () -> Unit,
    onRequireLogin: () -> Unit,
    onNavigateToExplore: () -> Unit,
    onNavigateToSmartMatch: () -> Unit = {},
    onPropertyClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    factory: U2ViewModelFactory,
    roomsEnabled: Boolean = true,
    onRoomClick: (String) -> Unit = {},
    onFavoriteRoomClick: (String) -> Unit = {},
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
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showUniversityPicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                HomeEffect.LoggedOut -> onLoggedOut()
                HomeEffect.RequireLogin -> onRequireLogin()
                is HomeEffect.Message -> Unit
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
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Spacing.xl, vertical = Spacing.lg)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
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
                    modifier = Modifier.semantics { contentDescription = "Saludo de inicio" },
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

            SmartMatchPromo(onClick = onNavigateToSmartMatch)

            PublishRentCta(onClick = { viewModel.onEvent(HomeEvent.PublishClicked) })

            SectionHeader(
                title = "Recomendados para ti",
                subtitle = "Opciones reales cerca de tu universidad",
                actionLabel = "Ver todas",
                onAction = onNavigateToExplore,
            )

            if (!roomsEnabled) {
                when (val properties = state.properties) {
                    CatalogLoadState.Loading -> LoadingRow(text = "Buscando rentas…")
                    CatalogLoadState.Empty -> EmptyMessage("Sin propiedades disponibles.")
                    is CatalogLoadState.Error -> ErrorMessage(
                        message = properties.errorMessage() ?: "Algo salió mal.",
                        onRetry = { viewModel.onEvent(HomeEvent.RetryPropertiesClicked) },
                    )
                    is CatalogLoadState.Success -> {
                        state.featuredProperty?.let { featured ->
                            PropertyCard(
                                property = featured,
                                onFavoriteClick = { viewModel.onEvent(HomeEvent.FavoriteClicked) },
                                onContactClick = { viewModel.onEvent(HomeEvent.ContactClicked) },
                                onPropertyClick = { onPropertyClick(featured.id) },
                            )
                        }
                    }
                }
            } else {
                when (val rooms = state.rooms) {
                    RoomSearchState.Loading -> LoadingRow(text = "Buscando alojamientos…")
                    RoomSearchState.Empty -> {
                        EmptyMessage("Selecciona una universidad para ver alojamientos cerca de ti.")
                    }
                    is RoomSearchState.Error -> ErrorMessage(
                        message = rooms.errorMessage() ?: "Algo salió mal.",
                        onRetry = { viewModel.onEvent(HomeEvent.RefreshRoomsClicked) },
                    )
                    is RoomSearchState.Content -> {
                        rooms.items.take(3).forEach { room ->
                            RoomCard(
                                room = room,
                                onRoomClick = { onRoomClick(room.roomId) },
                                onFavoriteClick = {
                                    viewModel.onEvent(HomeEvent.FavoriteRoomClicked(room.roomId))
                                    onFavoriteRoomClick(room.roomId)
                                },
                                isFavorite = room.roomId in state.favoriteRoomIds,
                            )
                        }
                        if (rooms.page < rooms.totalPages) {
                            TextButton(onClick = { viewModel.onEvent(HomeEvent.LoadMoreRoomsClicked) }) {
                                Text("Ver más alojamientos")
                            }
                        }
                    }
                }
            }

            // Acceso secundario: disponible pero sin competir con el contenido principal.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Icon(
                    imageVector = Icons.Outlined.PrivacyTip,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(IconSizes.sm),
                )
                TheHouseTextAction(text = "Privacidad", onClick = onOpenPrivacy)
            }
        }
    }
}

/**
 * CTA destacado de "Publicar mi renta". Tarjeta de acento coherente con el lenguaje visual
 * de TheHouse (sin FAB, sin nuevo estilo): superficie con borde primario suave e icono.
 * Solo presenta la acción; el evento sigue siendo `HomeEvent.PublishClicked`.
 */
@Composable
private fun PublishRentCta(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Publicar mi renta" },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
        shadowElevation = Elevations.card,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            ActionIcon(
                icon = Icons.Rounded.AddHome,
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(
                    text = "Publicar mi renta",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Comparte tu espacio con estudiantes cerca de ti.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(IconSizes.md),
            )
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
                                        Text(text = university.name, style = MaterialTheme.typography.bodyLarge)
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
private fun SmartMatchPromo(onClick: () -> Unit) {
    TheHouseCard(
        onClick = onClick,
        modifier = Modifier.testTag("home_smart_match_promo"),
    ) {
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
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        CircularProgressIndicator(modifier = Modifier.size(IconSizes.md), strokeWidth = 2.dp)
        Text(text = text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EmptyMessage(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun ErrorMessage(message: String, onRetry: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(text = message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        TextButton(onClick = onRetry) { Text("Reintentar") }
    }
}
