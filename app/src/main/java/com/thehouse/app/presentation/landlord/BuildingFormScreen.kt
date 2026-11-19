package com.thehouse.app.presentation.landlord

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thehouse.app.domain.model.AllowedTenants
import com.thehouse.app.domain.model.BuildingType
import com.thehouse.app.domain.model.Service
import com.thehouse.app.presentation.components.ErrorStateView
import com.thehouse.app.presentation.components.InlineMessage
import com.thehouse.app.presentation.components.LoadingView
import com.thehouse.app.presentation.components.TheHouseCard
import com.thehouse.app.presentation.components.TheHousePrimaryButton
import com.thehouse.app.presentation.components.TheHouseScreen
import com.thehouse.app.presentation.components.TheHouseTextField
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
    TheHouseScreen(
        title = if (state.isEdit) "Editar edificio" else "Nuevo edificio",
        subtitle = "Datos, ubicación y servicios",
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
                state.errorMessage != null && !state.isEdit && state.errorCode != null ->
                    ErrorStateView(
                        title = "No se pudo cargar",
                        message = state.errorMessage,
                        onRetry = onReload,
                    )
                else -> {
                    FormSection(title = "Datos generales") {
                        TheHouseTextField(
                            value = state.name,
                            onValueChange = { onEvent(BuildingFormEvent.NameChanged(it)) },
                            label = "Nombre",
                            isError = state.nameError != null,
                            errorMessage = state.nameError,
                        )
                        Text(
                            text = "Tipo",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        BuildingType.entries.chunked(2).forEach { rowTypes ->
                            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                                rowTypes.forEach { type ->
                                    FilterChip(
                                        selected = state.type == type,
                                        onClick = { onEvent(BuildingFormEvent.TypeChanged(type)) },
                                        label = { Text(type.label) },
                                    )
                                }
                            }
                        }
                    }

                    FormSection(title = "Dirección") {
                        TheHouseTextField(
                            value = state.street,
                            onValueChange = { onEvent(BuildingFormEvent.StreetChanged(it)) },
                            label = "Calle",
                            isError = state.streetError != null,
                            errorMessage = state.streetError,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            TheHouseTextField(
                                value = state.number,
                                onValueChange = { onEvent(BuildingFormEvent.NumberChanged(it)) },
                                label = "Número",
                                modifier = Modifier.weight(1f),
                            )
                            TheHouseTextField(
                                value = state.neighborhood,
                                onValueChange = {
                                    onEvent(BuildingFormEvent.NeighborhoodChanged(it))
                                },
                                label = "Colonia",
                                modifier = Modifier.weight(2f),
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            TheHouseTextField(
                                value = state.city,
                                onValueChange = { onEvent(BuildingFormEvent.CityChanged(it)) },
                                label = "Ciudad",
                                isError = state.cityError != null,
                                errorMessage = state.cityError,
                                modifier = Modifier.weight(2f),
                            )
                            TheHouseTextField(
                                value = state.state,
                                onValueChange = { onEvent(BuildingFormEvent.StateChanged(it)) },
                                label = "Estado",
                                isError = state.stateError != null,
                                errorMessage = state.stateError,
                                modifier = Modifier.weight(2f),
                            )
                        }
                        TheHouseTextField(
                            value = state.postalCode,
                            onValueChange = { onEvent(BuildingFormEvent.PostalCodeChanged(it)) },
                            label = "C.P.",
                            isError = state.postalCodeError != null,
                            errorMessage = state.postalCodeError,
                        )
                    }

                    FormSection(title = "Ubicación") {
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            TheHouseTextField(
                                value = state.lat,
                                onValueChange = { onEvent(BuildingFormEvent.LatChanged(it)) },
                                label = "Latitud",
                                isError = state.latError != null,
                                errorMessage = state.latError,
                                modifier = Modifier.weight(1f),
                            )
                            TheHouseTextField(
                                value = state.lng,
                                onValueChange = { onEvent(BuildingFormEvent.LngChanged(it)) },
                                label = "Longitud",
                                isError = state.lngError != null,
                                errorMessage = state.lngError,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Text(
                            text = "Universidad más cercana",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        state.universities.forEach { university ->
                            FilterChip(
                                selected = state.universityId == university.id,
                                onClick = {
                                    onEvent(BuildingFormEvent.UniversityChanged(university.id))
                                },
                                label = { Text(university.name) },
                            )
                        }
                        state.universityError?.let {
                            InlineMessage(text = it, tone = com.thehouse.app.presentation.components.StatusTone.Warning)
                        }
                    }

                    FormSection(title = "Preferencias y servicios") {
                        Text(
                            text = "Quién puede rentar",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        AllowedTenants.entries.chunked(2).forEach { rowTenants ->
                            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                                rowTenants.forEach { tenants ->
                                    FilterChip(
                                        selected = state.allowedTenants == tenants,
                                        onClick = {
                                            onEvent(BuildingFormEvent.TenantsChanged(tenants))
                                        },
                                        label = { Text(tenants.label) },
                                    )
                                }
                            }
                        }
                        Text(
                            text = "Servicios",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Service.entries.chunked(2).forEach { rowServices ->
                            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                                rowServices.forEach { service ->
                                    FilterChip(
                                        selected = service in state.selectedServices,
                                        onClick = {
                                            onEvent(BuildingFormEvent.ServiceToggled(service))
                                        },
                                        label = { Text(service.label) },
                                    )
                                }
                            }
                        }
                    }

                    FormSection(title = "Reglas") {
                        TheHouseTextField(
                            value = state.rules,
                            onValueChange = { onEvent(BuildingFormEvent.RulesChanged(it)) },
                            label = "Reglas (opcional, máx. 2000)",
                            supportingText = "${state.rules.length}/2000",
                            singleLine = false,
                            minLines = 4,
                        )
                    }

                    state.errorMessage?.let { message ->
                        InlineMessage(text = message)
                    }

                    TheHousePrimaryButton(
                        text = when {
                            state.isSaving -> "Guardando…"
                            state.isEdit -> "Guardar cambios"
                            else -> "Crear edificio"
                        },
                        onClick = { onEvent(BuildingFormEvent.Submit) },
                        enabled = !state.isLoading && state.isValid,
                        loading = state.isSaving,
                    )
                }
            }
        }
    }
}

@Composable
private fun FormSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    TheHouseCard {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            content()
        }
    }
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
