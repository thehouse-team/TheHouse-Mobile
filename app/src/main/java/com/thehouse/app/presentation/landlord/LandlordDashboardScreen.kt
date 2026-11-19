package com.thehouse.app.presentation.landlord

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thehouse.app.domain.model.DashboardSummary
import com.thehouse.app.domain.model.LandlordRoomStats
import com.thehouse.app.domain.model.RentalMode
import com.thehouse.app.presentation.components.EmptyStateView
import com.thehouse.app.presentation.components.ErrorStateView
import com.thehouse.app.presentation.components.InlineMessage
import com.thehouse.app.presentation.components.LoadingView
import com.thehouse.app.presentation.components.SectionHeader
import com.thehouse.app.presentation.components.StatTile
import com.thehouse.app.presentation.components.StatusChip
import com.thehouse.app.presentation.components.StatusTone
import com.thehouse.app.presentation.components.TheHouseCard
import com.thehouse.app.presentation.components.TheHousePrimaryButton
import com.thehouse.app.presentation.components.TheHouseScreen
import com.thehouse.app.presentation.components.TheHouseSecondaryButton
import com.thehouse.app.presentation.components.TheHouseTextField
import com.thehouse.app.ui.theme.Spacing

/**
 * Destino del dashboard de arrendador. Si el usuario aún no es arrendador,
 * muestra la pantalla de alta del perfil (modo arrendador).
 */
@Composable
fun LandlordDashboardDestination(
    factory: ViewModelProvider.Factory,
    onBack: () -> Unit,
    onCreateBuilding: () -> Unit,
    onBuildingClick: (String) -> Unit,
    onOpenLandlordRequests: () -> Unit,
    onActivated: () -> Unit,
) {
    val dashboardVm: LandlordDashboardViewModel = viewModel(factory = factory)
    val dashboardState by dashboardVm.uiState.collectAsStateWithLifecycle()

    if (dashboardState.notLandlord) {
        val profileVm: LandlordProfileViewModel = viewModel(
            key = "landlord-profile",
            factory = factory,
        )
        val profileState by profileVm.uiState.collectAsStateWithLifecycle()
        LaunchedEffect(Unit) {
            profileVm.effects.collect { effect ->
                if (effect is LandlordProfileEffect.Activated) onActivated()
            }
        }
        LandlordProfileScreen(
            state = profileState,
            onBack = onBack,
            onEvent = { profileVm.onEvent(it) },
        )
    } else {
        LandlordDashboardScreen(
            state = dashboardState,
            onBack = onBack,
            onCreateBuilding = onCreateBuilding,
            onBuildingClick = onBuildingClick,
            onOpenLandlordRequests = onOpenLandlordRequests,
            onRetry = { dashboardVm.onEvent(LandlordDashboardEvent.Retry) },
        )
    }
}

@Composable
fun LandlordDashboardScreen(
    state: LandlordDashboardUiState,
    onBack: () -> Unit,
    onCreateBuilding: () -> Unit,
    onBuildingClick: (String) -> Unit,
    onOpenLandlordRequests: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TheHouseScreen(
        title = "Mis alojamientos",
        subtitle = "Gestiona tus edificios y cuartos",
        onBack = onBack,
        modifier = modifier,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Spacing.xl, vertical = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            state.summary?.let { summary -> SummaryGrid(summary = summary) }

            when {
                state.isLoading -> LoadingView(
                    message = "Cargando alojamientos…",
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
                state.errorMessage != null -> ErrorStateView(
                    title = "No se pudieron cargar",
                    message = state.errorMessage,
                    onRetry = onRetry,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
                else -> Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Spacing.lg),
                ) {
                    if (state.roomStats.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            SectionHeader(
                                title = "Estadísticas por cuarto",
                                subtitle = "Rendimiento de tus espacios",
                            )
                            RoomsStatsGrid(
                                rooms = state.roomStats,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }

                    if (state.buildingCount == 0) {
                        EmptyStateView(
                            title = "Sin edificios",
                            message = "Registra tu primer edificio y agrega cuartos para publicar.",
                            ctaLabel = "Crear edificio",
                            onCtaClick = onCreateBuilding,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(Spacing.md),
                        ) {
                            item {
                                SectionHeader(
                                    title = "Edificios",
                                    subtitle = "${state.buildingCount} registrados",
                                )
                            }
                            items(state.buildings, key = { it.id }) { building ->
                                TheHouseCard(
                                    modifier = Modifier.semantics {
                                        contentDescription =
                                            "Edificio ${building.name}, " +
                                            LandlordErrorMessages.statusLabel(building.status.api)
                                    },
                                    onClick = { onBuildingClick(building.id) },
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                                        ) {
                                            Text(
                                                text = building.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.onSurface,
                                            )
                                            Text(
                                                text = "${building.roomsAvailable}/" +
                                                    "${building.roomsTotal} disponibles",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                        StatusChip(
                                            label = LandlordErrorMessages
                                                .statusLabel(building.status.api),
                                            tone = buildingTone(building.status.api),
                                        )
                                    }
                                }
                            }
                            item {
                                TheHouseSecondaryButton(
                                    text = "Solicitudes recibidas",
                                    onClick = onOpenLandlordRequests,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryGrid(
    summary: DashboardSummary,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            SummaryTile(
                label = "Edificios",
                value = summary.buildings.toString(),
                modifier = Modifier.weight(1f),
            )
            SummaryTile(
                label = "Cuartos",
                value = summary.rooms.toString(),
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            SummaryTile(
                label = "Disponibles",
                value = summary.available.toString(),
                modifier = Modifier.weight(1f),
            )
            SummaryTile(
                label = "Ocupación",
                value = "${summary.occupancyPercent.toString().trimEnd('0', '.')}%",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SummaryTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    StatTile(
        label = label,
        value = value,
        modifier = modifier.semantics { contentDescription = "$label: $value" },
    )
}

@Composable
fun RoomsStatsGrid(
    rooms: List<LandlordRoomStats>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        rooms.forEach { room ->
            TheHouseCard(
                modifier = Modifier.semantics {
                    contentDescription =
                        "Cuarto ${room.number}, ${room.buildingName}, estado ${room.status}"
                },
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "${room.buildingName} · Cuarto ${room.number}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        StatusChip(
                            label = LandlordErrorMessages.statusLabel(room.status),
                            tone = roomTone(room.status),
                        )
                    }
                    Text(
                        text = "Precio: ${room.price}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "Vistas: ${room.views} · Favoritos: ${room.favorites} · " +
                            "Solicitudes: ${room.requests}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "Visitas: ${room.visits} · Acuerdos: ${room.agreements} · " +
                            "Conversión: ${room.conversion}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    val ratingText = room.rating?.let {
                        "Calificación: ${it.average} (${it.count})"
                    } ?: "Calificación: sin datos"
                    Text(
                        text = ratingText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
fun LandlordProfileScreen(
    state: LandlordProfileUiState,
    onBack: () -> Unit,
    onEvent: (LandlordProfileEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    TheHouseScreen(
        title = "Activa el modo arrendador",
        subtitle = "Publica y administra tus espacios",
        onBack = onBack,
        modifier = modifier,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.xl, vertical = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            Text(
                text = "Completa tus datos para publicar edificios y recibir solicitudes.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            TheHouseTextField(
                value = state.fullName,
                onValueChange = { onEvent(LandlordProfileEvent.FullNameChanged(it)) },
                label = "Nombre completo",
                isError = state.fullName.isNotEmpty() && state.fullNameError != null,
                errorMessage = state.fullNameError,
            )
            TheHouseTextField(
                value = state.phone,
                onValueChange = { onEvent(LandlordProfileEvent.PhoneChanged(it)) },
                label = "Teléfono (+52XXXXXXXXXX)",
                isError = state.phone.isNotEmpty() && state.phoneError != null,
                errorMessage = state.phoneError,
            )

            Text(
                text = "Tipo de renta",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                FilterChip(
                    selected = state.rentalMode == RentalMode.INDIVIDUAL,
                    onClick = {
                        onEvent(LandlordProfileEvent.RentalModeChanged(RentalMode.INDIVIDUAL))
                    },
                    label = { Text("Individual") },
                )
                FilterChip(
                    selected = state.rentalMode == RentalMode.AGENCY,
                    onClick = {
                        onEvent(LandlordProfileEvent.RentalModeChanged(RentalMode.AGENCY))
                    },
                    label = { Text("Agencia") },
                )
            }
            if (state.rentalMode == RentalMode.AGENCY) {
                TheHouseTextField(
                    value = state.rfc,
                    onValueChange = { onEvent(LandlordProfileEvent.RfcChanged(it)) },
                    label = "RFC (12 caracteres)",
                    isError = state.rfc.isNotEmpty() && state.rfcError != null,
                    errorMessage = state.rfcError,
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = state.acceptedTerms,
                        role = Role.Checkbox,
                        onValueChange = { onEvent(LandlordProfileEvent.TermsToggled(it)) },
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Checkbox(
                    checked = state.acceptedTerms,
                    onCheckedChange = null,
                )
                Text(
                    text = "Acepto los términos del arrendador",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
            }

            state.errorMessage?.let { message ->
                InlineMessage(text = message)
            }

            TheHousePrimaryButton(
                text = if (state.isSaving) "Activando…" else "Activar modo arrendador",
                onClick = { onEvent(LandlordProfileEvent.Submit) },
                enabled = state.isValid,
                loading = state.isSaving,
            )
        }
    }
}

private fun buildingTone(status: String): StatusTone = when (status) {
    "verified" -> StatusTone.Success
    "in_review", "draft" -> StatusTone.Warning
    "rejected", "suspended" -> StatusTone.Error
    else -> StatusTone.Neutral
}

private fun roomTone(status: String): StatusTone = when (status) {
    "available" -> StatusTone.Success
    "rented" -> StatusTone.Primary
    "paused" -> StatusTone.Warning
    else -> StatusTone.Neutral
}
