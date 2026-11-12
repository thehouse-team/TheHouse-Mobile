package com.thehouse.app.presentation.landlord

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thehouse.app.domain.model.AllowedTenants
import com.thehouse.app.domain.model.BuildingType
import com.thehouse.app.domain.model.Service
import com.thehouse.app.presentation.components.ErrorStateView
import com.thehouse.app.presentation.components.LoadingView
import com.thehouse.app.ui.theme.Heights
import com.thehouse.app.ui.theme.Spacing

@Composable
fun BuildingFormDestination(
    buildingId: String?,
    factory: com.thehouse.app.presentation.U2ViewModelFactory,
    onBack: () -> Unit,
    onSaved: (String) -> Unit,
) {
    val formFactory = remember(buildingId) { factory.buildingForm(buildingId) }
    val viewModel: BuildingFormViewModel = viewModel(factory = formFactory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            if (effect is BuildingFormEffect.Saved) onSaved(effect.buildingId)
        }
    }
    BuildingFormScreen(
        state = state,
        onBack = onBack,
        onEvent = { viewModel.onEvent(it) },
        onReload = { viewModel.onEvent(BuildingFormEvent.Reload) },
    )
}

@Composable
fun BuildingFormScreen(
    state: BuildingFormUiState,
    onBack: () -> Unit,
    onEvent: (BuildingFormEvent) -> Unit,
    onReload: () -> Unit,
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
                if (state.isEdit) "Editar edificio" else "Nuevo edificio",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { heading() },
            )

            when {
                state.isLoading -> LoadingView(
                    message = "Cargando edificio…",
                    modifier = Modifier.fillMaxWidth(),
                )
                state.versionConflict -> ErrorStateView(
                    title = "Conflicto de edición",
                    message = LandlordErrorMessages.map(
                        com.thehouse.app.domain.error.LandlordException(
                            com.thehouse.app.domain.error.DomainError.Conflict,
                            com.thehouse.app.domain.error.LandlordErrorCode.VERSION_CONFLICT,
                        ),
                    ),
                    retryLabel = "Recargar datos",
                    onRetry = onReload,
                )
                state.errorMessage != null && !state.isEdit && state.errorCode != null -> ErrorStateView(
                    title = "No se pudo cargar",
                    message = state.errorMessage,
                    onRetry = onReload,
                )
                else -> {
                    OutlinedTextField(
                        value = state.name,
                        onValueChange = { onEvent(BuildingFormEvent.NameChanged(it)) },
                        label = { Text("Nombre") },
                        isError = state.nameError != null,
                        supportingText = { state.nameError?.let { Text(it) } },
                        modifier = Modifier.fillMaxWidth().heightIn(min = Heights.input),
                        singleLine = true,
                    )

                    Text("Tipo", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    ) {
                        BuildingType.entries.forEach { type ->
                            FilterChip(
                                selected = state.type == type,
                                onClick = { onEvent(BuildingFormEvent.TypeChanged(type)) },
                                label = { Text(type.label) },
                            )
                        }
                    }

                    GroupLabel("Dirección")
                    OutlinedTextField(
                        value = state.street,
                        onValueChange = { onEvent(BuildingFormEvent.StreetChanged(it)) },
                        label = { Text("Calle") },
                        isError = state.streetError != null,
                        supportingText = { state.streetError?.let { Text(it) } },
                        modifier = Modifier.fillMaxWidth().heightIn(min = Heights.input),
                        singleLine = true,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        OutlinedTextField(
                            value = state.number,
                            onValueChange = { onEvent(BuildingFormEvent.NumberChanged(it)) },
                            label = { Text("Número") },
                            modifier = Modifier.weight(1f).heightIn(min = Heights.input),
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = state.neighborhood,
                            onValueChange = { onEvent(BuildingFormEvent.NeighborhoodChanged(it)) },
                            label = { Text("Colonia") },
                            modifier = Modifier.weight(2f).heightIn(min = Heights.input),
                            singleLine = true,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        OutlinedTextField(
                            value = state.city,
                            onValueChange = { onEvent(BuildingFormEvent.CityChanged(it)) },
                            label = { Text("Ciudad") },
                            isError = state.cityError != null,
                            supportingText = { state.cityError?.let { Text(it) } },
                            modifier = Modifier.weight(2f).heightIn(min = Heights.input),
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = state.state,
                            onValueChange = { onEvent(BuildingFormEvent.StateChanged(it)) },
                            label = { Text("Estado") },
                            isError = state.stateError != null,
                            supportingText = { state.stateError?.let { Text(it) } },
                            modifier = Modifier.weight(2f).heightIn(min = Heights.input),
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = state.postalCode,
                            onValueChange = { onEvent(BuildingFormEvent.PostalCodeChanged(it)) },
                            label = { Text("C.P.") },
                            isError = state.postalCodeError != null,
                            supportingText = { state.postalCodeError?.let { Text(it) } },
                            modifier = Modifier.weight(1f).heightIn(min = Heights.input),
                            singleLine = true,
                        )
                    }

                    GroupLabel("Ubicación")
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        OutlinedTextField(
                            value = state.lat,
                            onValueChange = { onEvent(BuildingFormEvent.LatChanged(it)) },
                            label = { Text("Latitud") },
                            isError = state.latError != null,
                            supportingText = { state.latError?.let { Text(it) } },
                            modifier = Modifier.weight(1f).heightIn(min = Heights.input),
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = state.lng,
                            onValueChange = { onEvent(BuildingFormEvent.LngChanged(it)) },
                            label = { Text("Longitud") },
                            isError = state.lngError != null,
                            supportingText = { state.lngError?.let { Text(it) } },
                            modifier = Modifier.weight(1f).heightIn(min = Heights.input),
                            singleLine = true,
                        )
                    }

                    Text("Universidad más cercana", style = MaterialTheme.typography.labelLarge)
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        state.universities.forEach { university ->
                            FilterChip(
                                selected = state.universityId == university.id,
                                onClick = { onEvent(BuildingFormEvent.UniversityChanged(university.id)) },
                                label = { Text(university.name) },
                            )
                        }
                        state.universityError?.let {
                            Text(it, color = MaterialTheme.colorScheme.error)
                        }
                    }

                    Text("Quién puede rentar", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        AllowedTenants.entries.forEach { tenants ->
                            FilterChip(
                                selected = state.allowedTenants == tenants,
                                onClick = { onEvent(BuildingFormEvent.TenantsChanged(tenants)) },
                                label = { Text(tenants.label) },
                            )
                        }
                    }

                    Text("Servicios", style = MaterialTheme.typography.labelLarge)
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        Service.entries.chunked(2).forEach { rowServices ->
                            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                                rowServices.forEach { service ->
                                    FilterChip(
                                        selected = service in state.selectedServices,
                                        onClick = { onEvent(BuildingFormEvent.ServiceToggled(service)) },
                                        label = { Text(service.label) },
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = state.rules,
                        onValueChange = { onEvent(BuildingFormEvent.RulesChanged(it)) },
                        label = { Text("Reglas (opcional, máx. 2000)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp),
                    )
                    Text(
                        "${state.rules.length}/2000",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    state.errorMessage?.let {
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }

                    Button(
                        onClick = { onEvent(BuildingFormEvent.Submit) },
                        enabled = !state.isSaving && !state.isLoading && state.isValid,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = Heights.button),
                    ) {
                        Text(
                            when {
                                state.isSaving -> "Guardando…"
                                state.isEdit -> "Guardar cambios"
                                else -> "Crear edificio"
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupLabel(label: String) {
    Text(label, style = MaterialTheme.typography.titleSmall)
}

private val BuildingType.label: String
    get() = when (this) {
        BuildingType.ROOMS_BUILDING -> "Edificio de cuartos"
        BuildingType.HOUSE_ROOMS -> "Casa con cuartos"
        BuildingType.FULL_APARTMENT -> "Departamento completo"
    }

private val AllowedTenants.label: String
    get() = when (this) {
        AllowedTenants.WOMEN -> "Mujeres"
        AllowedTenants.MEN -> "Hombres"
        AllowedTenants.ANY -> "Cualquiera"
    }

private val Service.label: String
    get() = when (this) {
        Service.WIFI -> "Wifi"
        Service.WATER -> "Agua"
        Service.ELECTRICITY -> "Luz"
        Service.GAS -> "Gas"
        Service.HOT_WATER -> "Agua caliente"
        Service.LAUNDRY -> "Lavandería"
        Service.KITCHEN -> "Cocina"
        Service.FURNISHED -> "Amueblado"
        Service.PARKING -> "Estacionamiento"
        Service.SECURITY -> "Seguridad"
        Service.CLEANING -> "Limpieza"
    }
