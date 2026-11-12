package com.thehouse.app.presentation.landlord

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thehouse.app.domain.model.RentalMode
import com.thehouse.app.presentation.components.EmptyStateView
import com.thehouse.app.presentation.components.ErrorStateView
import com.thehouse.app.presentation.components.LoadingView
import com.thehouse.app.ui.theme.Heights
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
    Scaffold(modifier = modifier) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            TextButton(onClick = onBack, modifier = Modifier.heightIn(min = Heights.button)) {
                Text("Volver")
            }
            Text(
                "Mis alojamientos",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { heading() },
            )

            state.summary?.let { summary ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    SummaryItem("Edificios", summary.buildings.toString())
                    SummaryItem("Cuartos", summary.rooms.toString())
                    SummaryItem("Disponibles", summary.available.toString())
                    SummaryItem(
                        "Ocupación",
                        "${summary.occupancyPercent.toString().trimEnd('0', '.')}%",
                    )
                }
            }

            when {
                state.isLoading -> LoadingView(
                    message = "Cargando alojamientos…",
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
                state.buildingCount == 0 -> EmptyStateView(
                    title = "Sin edificios",
                    message = "Registra tu primer edificio y agrega cuartos para publicar.",
                    ctaLabel = "Crear edificio",
                    onCtaClick = onCreateBuilding,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
                state.errorMessage != null -> ErrorStateView(
                    title = "No se pudieron cargar",
                    message = state.errorMessage,
                    onRetry = onRetry,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
                else -> LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    items(state.buildings, key = { it.id }) { building ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onBuildingClick(building.id) }
                                .heightIn(min = 48.dp)
                                .semantics {
                                    contentDescription =
                                        "Edificio ${building.name}, ${LandlordErrorMessages.statusLabel(building.status.api)}"
                                },
                        ) {
                            Text(
                                building.name,
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                "${LandlordErrorMessages.statusLabel(building.status.api)} · " +
                                    "${building.roomsAvailable}/${building.roomsTotal} disponibles",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    item {
                        OutlinedButton(
                            onClick = onOpenLandlordRequests,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = Heights.button),
                        ) {
                            Text("Solicitudes recibidas")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryItem(label: String, value: String) {
    Column(
        modifier = Modifier.semantics { contentDescription = "$label: $value" },
    ) {
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun LandlordProfileScreen(
    state: LandlordProfileUiState,
    onBack: () -> Unit,
    onEvent: (LandlordProfileEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            TextButton(onClick = onBack, modifier = Modifier.heightIn(min = Heights.button)) {
                Text("Volver")
            }
            Text(
                "Activa el modo arrendador",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { heading() },
            )

            OutlinedTextField(
                value = state.fullName,
                onValueChange = { onEvent(LandlordProfileEvent.FullNameChanged(it)) },
                label = { Text("Nombre completo") },
                isError = state.fullName.isNotEmpty() && state.fullNameError != null,
                supportingText = {
                    if (state.fullName.isNotEmpty() && state.fullNameError != null) {
                        Text(state.fullNameError)
                    }
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = Heights.input),
                singleLine = true,
            )
            OutlinedTextField(
                value = state.phone,
                onValueChange = { onEvent(LandlordProfileEvent.PhoneChanged(it)) },
                label = { Text("Teléfono (+52XXXXXXXXXX)") },
                isError = state.phone.isNotEmpty() && state.phoneError != null,
                supportingText = {
                    if (state.phone.isNotEmpty() && state.phoneError != null) {
                        Text(state.phoneError)
                    }
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = Heights.input),
                singleLine = true,
            )

            Text("Tipo de renta", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                OutlinedButton(
                    onClick = { onEvent(LandlordProfileEvent.RentalModeChanged(RentalMode.INDIVIDUAL)) },
                    modifier = Modifier.weight(1f).heightIn(min = Heights.button),
                ) {
                    Text(
                        "Individual",
                        color = if (state.rentalMode == RentalMode.INDIVIDUAL) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
                OutlinedButton(
                    onClick = { onEvent(LandlordProfileEvent.RentalModeChanged(RentalMode.AGENCY)) },
                    modifier = Modifier.weight(1f).heightIn(min = Heights.button),
                ) {
                    Text(
                        "Agencia",
                        color = if (state.rentalMode == RentalMode.AGENCY) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
            if (state.rentalMode == RentalMode.AGENCY) {
                OutlinedTextField(
                    value = state.rfc,
                    onValueChange = { onEvent(LandlordProfileEvent.RfcChanged(it)) },
                    label = { Text("RFC (12 caracteres)") },
                    isError = state.rfc.isNotEmpty() && state.rfcError != null,
                    supportingText = {
                        if (state.rfc.isNotEmpty() && state.rfcError != null) {
                            Text(state.rfcError)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = Heights.input),
                    singleLine = true,
                )
            }

            Row(
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = state.acceptedTerms,
                    onCheckedChange = { onEvent(LandlordProfileEvent.TermsToggled(it)) },
                )
                Text(
                    "Acepto los términos del arrendador",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            state.errorMessage?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Button(
                onClick = { onEvent(LandlordProfileEvent.Submit) },
                enabled = !state.isSaving && state.isValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = Heights.button),
            ) {
                Text(if (state.isSaving) "Activando…" else "Activar modo arrendador")
            }
        }
    }
}
