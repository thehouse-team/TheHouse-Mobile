package com.thehouse.app.presentation.preferences

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.LocationCity
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thehouse.app.data.remote.dto.preferences.PreferencesContract
import com.thehouse.app.presentation.U2ViewModelFactory
import com.thehouse.app.presentation.components.ErrorStateView
import com.thehouse.app.presentation.components.InlineMessage
import com.thehouse.app.presentation.components.LoadingView
import com.thehouse.app.presentation.components.TheHouseCard
import com.thehouse.app.presentation.components.TheHousePrimaryButton
import com.thehouse.app.presentation.components.TheHouseTextField
import com.thehouse.app.preferences.flow.PreferencesFlowLogger
import com.thehouse.app.ui.theme.IconSizes
import com.thehouse.app.ui.theme.Spacing

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

@OptIn(ExperimentalMaterial3Api::class)
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
    Scaffold(
        modifier = modifier.fillMaxSize().imePadding(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Preferencias",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.testTag("preferences_title"),
                        )
                        Text(
                            text = "Personaliza tu búsqueda",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    androidx.compose.material3.IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Volver",
                        )
                    }
                },
            )
        },
        // CTA principal fijo: siempre visible y accesible aunque el teclado esté abierto.
        bottomBar = {
            if (!state.requiresAuth && state.loadError == null && !state.isLoading) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.xl, vertical = Spacing.md),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    state.saveError?.let { InlineMessage(text = it, tone = com.thehouse.app.presentation.components.StatusTone.Error) }
                    state.message?.let { InlineMessage(text = it, tone = com.thehouse.app.presentation.components.StatusTone.Success) }
                    TheHousePrimaryButton(
                        text = "Guardar preferencias",
                        onClick = { onEvent(PreferencesEvent.Save) },
                        loading = state.isSaving,
                        enabled = !state.isSaving && !state.hasValidationErrors,
                        testTag = "preferences_save_button",
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
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
                    onOpenProfile = onOpenProfile,
                    onOpenQuickSetup = onOpenQuickSetup,
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
    onOpenProfile: () -> Unit,
    onOpenQuickSetup: () -> Unit,
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

        if (state.missingUniversity) {
            InlineMessage(
                text = "Completa tu universidad para mejorar tus recomendaciones.",
                tone = com.thehouse.app.presentation.components.StatusTone.Warning,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                androidx.compose.material3.TextButton(onClick = onOpenProfile) {
                    Text("Ir a Mi perfil")
                }
                androidx.compose.material3.TextButton(onClick = onOpenQuickSetup) {
                    Text("Configuración inicial")
                }
            }
        }
        if (state.missingHere.isNotEmpty()) {
            InlineMessage(
                text = "Completa: ${state.missingHere.joinToString(", ")} para mejorar tus recomendaciones.",
                tone = com.thehouse.app.presentation.components.StatusTone.Warning,
            )
        }

        PreferencesCard(
            title = "Presupuesto",
            subtitle = "Rango mensual aproximado en ${PreferencesContract.CURRENCY}",
            icon = Icons.Outlined.Savings,
        ) {
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
            Text(
                text = "Moneda: ${PreferencesContract.CURRENCY}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        PreferencesCard(
            title = "Ubicación y transporte",
            subtitle = "Distancia y forma de traslado",
            icon = Icons.Outlined.LocationCity,
        ) {
            TheHouseTextField(
                value = state.maxDistanceKm,
                onValueChange = { onEvent(PreferencesEvent.MaxDistanceKmChanged(it)) },
                label = "Distancia máxima (km)",
                placeholder = "Ej. 5",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = state.maxDistanceError != null,
                errorMessage = state.maxDistanceError,
            )
            PreferencesDropdown(
                label = "Transporte principal",
                value = state.transport,
                options = PreferencesContract.TRANSPORT_CODES.map(PreferencesContract::codeToLabel),
                onSelect = { onEvent(PreferencesEvent.TransportChanged(it)) },
                leadingIcon = Icons.AutoMirrored.Outlined.DirectionsWalk,
            )
        }

        PreferencesCard(
            title = "Convivencia y espacio",
            subtitle = "Cómo te gustaría vivir",
            icon = Icons.Outlined.Groups,
        ) {
            PreferencesDropdown(
                label = "Vivir con",
                value = state.livingWith,
                options = PreferencesContract.LIVING_WITH_CODES.map(PreferencesContract::codeToLabel),
                onSelect = { onEvent(PreferencesEvent.LivingWithChanged(it)) },
                leadingIcon = Icons.Outlined.Groups,
            )
            PreferencesDropdown(
                label = "Tipo de lugar",
                value = state.placeType,
                options = PreferencesContract.PLACE_TYPE_CODES.map(PreferencesContract::codeToLabel),
                onSelect = { onEvent(PreferencesEvent.PlaceTypeChanged(it)) },
                leadingIcon = Icons.Outlined.Checkroom,
            )
            PreferencesDropdown(
                label = "Mascotas",
                value = state.pets,
                options = listOf("Sí", "No"),
                onSelect = { onEvent(PreferencesEvent.PetsChanged(it)) },
                leadingIcon = Icons.Outlined.Pets,
            )
        }

        PreferencesCard(
            title = "Servicios esenciales",
            subtitle = "Selecciona los que necesitas",
            icon = Icons.Outlined.Checkroom,
        ) {
            MultiSelectDropdown(
                label = "Servicios esenciales",
                value = state.essentialServices,
                options = PreferencesContract.ESSENTIAL_SERVICES_CODES.map(PreferencesContract::codeToLabel),
                onSelect = { onEvent(PreferencesEvent.EssentialServicesChanged(it)) },
            )
        }

        PreferencesCard(
            title = "Tipo de propiedad",
            subtitle = "Selecciona los que te interesan",
            icon = Icons.Outlined.LocationCity,
        ) {
            MultiSelectDropdown(
                label = "Tipos de propiedad",
                value = state.propertyTypes,
                options = PreferencesContract.PROPERTY_TYPE_CODES.map(PreferencesContract::codeToLabel),
                onSelect = { onEvent(PreferencesEvent.PropertyTypesChanged(it)) },
            )
        }
    }
}

@Composable
private fun PreferencesCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit,
) {
    TheHouseCard {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(IconSizes.lg),
                )
                Column {
                    Text(text = title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
                content = content,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PreferencesDropdown(
    label: String,
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    leadingIcon: ImageVector? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = value.trim().takeIf { it.isNotBlank() }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
    ) {
        TheHouseTextField(
            value = selected ?: "",
            onValueChange = {},
            label = label,
            placeholder = "Sin especificar",
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            leadingIcon = leadingIcon,
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Sin especificar") },
                onClick = {
                    onSelect("")
                    expanded = false
                },
            )
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MultiSelectDropdown(
    label: String,
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = value.split(',')
        .map { it.trim() }
        .filter { it.isNotBlank() }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
    ) {
        TheHouseTextField(
            value = selected.joinToString(", "),
            onValueChange = {},
            label = label,
            placeholder = "Sin especificar",
            readOnly = true,
            singleLine = false,
            minLines = 2,
            maxLines = 3,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (option in selected) "☑ $option" else "☐ $option",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    },
                    onClick = {
                        val next = if (option in selected) selected - option else selected + option
                        onSelect(next.joinToString(", "))
                    },
                )
            }
            DropdownMenuItem(
                text = { Text("Limpiar selección") },
                onClick = {
                    onSelect("")
                    expanded = false
                },
            )
        }
    }
}
