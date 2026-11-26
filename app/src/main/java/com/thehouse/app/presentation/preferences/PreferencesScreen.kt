package com.thehouse.app.presentation.preferences

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thehouse.app.presentation.U2ViewModelFactory
import com.thehouse.app.presentation.components.ErrorStateView
import com.thehouse.app.presentation.components.InlineMessage
import com.thehouse.app.presentation.components.LoadingView
import com.thehouse.app.presentation.components.SectionHeader
import com.thehouse.app.presentation.components.StatusTone
import com.thehouse.app.presentation.components.TheHouseCard
import com.thehouse.app.presentation.components.TheHousePrimaryButton
import com.thehouse.app.presentation.components.TheHouseScreen
import com.thehouse.app.presentation.components.TheHouseTextField
import com.thehouse.app.ui.theme.Spacing
import com.thehouse.app.preferences.flow.PreferencesFlowLogger

@Composable
fun PreferencesDestination(
    factory: U2ViewModelFactory,
    onBack: () -> Unit,
    onRequireLogin: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    onOpenQuickSetup: () -> Unit = {},
    missingFields: List<String> = emptyList(),
) {
    val viewModel: PreferencesViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.onEvent(PreferencesEvent.Load)
    }

    LaunchedEffect(missingFields) {
        if (missingFields.isNotEmpty()) {
            viewModel.onEvent(PreferencesEvent.MissingFieldsChanged(missingFields))
        }
    }

    PreferencesScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        onRequireLogin = onRequireLogin,
        onOpenProfile = onOpenProfile,
        onOpenQuickSetup = onOpenQuickSetup,
    )
}

@Composable
fun PreferencesScreen(
    state: PreferencesUiState,
    onEvent: (PreferencesEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onRequireLogin: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    onOpenQuickSetup: () -> Unit = {},
) {
    PreferencesFlowLogger.enterScreen()
    TheHouseScreen(
        title = "Preferencias",
        subtitle = "Personaliza tu búsqueda",
        onBack = onBack,
        modifier = modifier,
    ) { padding ->
        when {
            state.requiresAuth -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    ErrorStateView(
                        title = "Necesitas iniciar sesión",
                        message = "Inicia sesión para personalizar tus preferencias.",
                        onRetry = onRequireLogin,
                    )
                }
            }

            state.isLoading && state.loadError == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    LoadingView(message = "Cargando preferencias…")
                }
            }

            state.loadError != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    ErrorStateView(
                        title = "No pudimos cargar tus preferencias",
                        message = state.loadError,
                        onRetry = { onEvent(PreferencesEvent.Retry) },
                    )
                }
            }

            else -> {
                PreferencesForm(
                    state = state,
                    onEvent = onEvent,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = Spacing.xl, vertical = Spacing.lg),
                )
            }
        }
    }
}

@Composable
private fun PreferencesForm(
    state: PreferencesUiState,
    onEvent: (PreferencesEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = Modifier.testTag("preferences_screen"),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        Text(
            text = "Estos datos se usan para recomendaciones y búsqueda. Puedes dejar en blanco lo que no aplique.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        state.message?.let { InlineMessage(text = it, tone = StatusTone.Success) }
        state.saveError?.let { InlineMessage(text = it, tone = StatusTone.Error) }

        if (state.missingUniversity) {
            InlineMessage(
                text = "Completa tu universidad en Mi perfil o Configuración inicial para mejorar tus recomendaciones.",
                tone = StatusTone.Warning,
            )
        }
        if (state.missingHere.isNotEmpty()) {
            InlineMessage(
                text = "Completa: ${state.missingHere.joinToString(", ")} para mejorar tus recomendaciones.",
                tone = StatusTone.Warning,
            )
        }

        TheHouseCard {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                SectionHeader(title = "Presupuesto", subtitle = "Rango mensual aproximado")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    TheHouseTextField(
                        value = state.budgetMin,
                        onValueChange = { onEvent(PreferencesEvent.BudgetMinChanged(it)) },
                        label = "Mínimo",
                        placeholder = "0",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = state.budgetMinError != null,
                        errorMessage = state.budgetMinError,
                        modifier = Modifier.weight(1f),
                    )
                    TheHouseTextField(
                        value = state.budgetMax,
                        onValueChange = { onEvent(PreferencesEvent.BudgetMaxChanged(it)) },
                        label = "Máximo",
                        placeholder = "0",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = state.budgetMaxError != null,
                        errorMessage = state.budgetMaxError,
                        modifier = Modifier.weight(1f),
                    )
                }
                TheHouseTextField(
                    value = state.budgetCurrency,
                    onValueChange = { onEvent(PreferencesEvent.BudgetCurrencyChanged(it)) },
                    label = "Moneda",
                    placeholder = "MXN",
                )
            }
        }

        TheHouseCard {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                SectionHeader(title = "Ubicación", subtitle = "Distancia y traslado")
                TheHouseTextField(
                    value = state.maxDistanceKm,
                    onValueChange = { onEvent(PreferencesEvent.MaxDistanceKmChanged(it)) },
                    label = "Distancia máxima (km)",
                    placeholder = "Ej. 5",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = state.maxDistanceError != null,
                    errorMessage = state.maxDistanceError,
                )
                TheHouseTextField(
                    value = state.transport,
                    onValueChange = { onEvent(PreferencesEvent.TransportChanged(it)) },
                    label = "Transporte principal",
                    placeholder = "Caminar, autobús, bicicleta…",
                )
            }
        }

        TheHouseCard {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                SectionHeader(title = "Convivencia y espacio", subtitle = "Cómo te gustaría vivir")
                TheHouseTextField(
                    value = state.livingWith,
                    onValueChange = { onEvent(PreferencesEvent.LivingWithChanged(it)) },
                    label = "Vivir con",
                    placeholder = "Solo, roomies…",
                )
                TheHouseTextField(
                    value = state.placeType,
                    onValueChange = { onEvent(PreferencesEvent.PlaceTypeChanged(it)) },
                    label = "Tipo de lugar",
                    placeholder = "Cuarto, departamento…",
                )
                TheHouseTextField(
                    value = state.pets,
                    onValueChange = { onEvent(PreferencesEvent.PetsChanged(it)) },
                    label = "Mascotas",
                    placeholder = "Sí o No",
                )
            }
        }

        TheHouseCard {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                SectionHeader(title = "Servicios y tipos", subtitle = "Separa varios valores con comas")
                TheHouseTextField(
                    value = state.essentialServices,
                    onValueChange = { onEvent(PreferencesEvent.EssentialServicesChanged(it)) },
                    label = "Servicios esenciales",
                    placeholder = "Internet, agua, luz…",
                    singleLine = false,
                    minLines = 2,
                    maxLines = 4,
                )
                TheHouseTextField(
                    value = state.propertyTypes,
                    onValueChange = { onEvent(PreferencesEvent.PropertyTypesChanged(it)) },
                    label = "Tipos de propiedad",
                    placeholder = "Cuarto, estudio, departamento…",
                    singleLine = false,
                    minLines = 2,
                    maxLines = 4,
                )
            }
        }

        TheHousePrimaryButton(
            text = "Guardar preferencias",
            onClick = { onEvent(PreferencesEvent.Save) },
            loading = state.isSaving,
            enabled = !state.isSaving && !state.hasValidationErrors,
        )
    }
}
