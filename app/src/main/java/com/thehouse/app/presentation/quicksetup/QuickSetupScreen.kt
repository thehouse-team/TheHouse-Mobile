package com.thehouse.app.presentation.quicksetup

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thehouse.app.domain.model.University
import com.thehouse.app.presentation.U2ViewModelFactory
import com.thehouse.app.presentation.components.ErrorStateView
import com.thehouse.app.presentation.components.InfoRow
import com.thehouse.app.presentation.components.InlineMessage
import com.thehouse.app.presentation.components.LoadingView
import com.thehouse.app.presentation.components.SectionHeader
import com.thehouse.app.presentation.components.StatusChip
import com.thehouse.app.presentation.components.StatusTone
import com.thehouse.app.presentation.components.TheHouseCard
import com.thehouse.app.presentation.components.TheHousePrimaryButton
import com.thehouse.app.presentation.components.TheHouseScreen
import com.thehouse.app.presentation.components.TheHouseSecondaryButton
import com.thehouse.app.presentation.components.TheHouseTextField
import com.thehouse.app.ui.theme.Heights
import com.thehouse.app.ui.theme.Spacing

@Composable
fun QuickSetupDestination(
    factory: U2ViewModelFactory,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    onOpenPreferences: () -> Unit,
) {
    val viewModel: QuickSetupViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.onEvent(QuickSetupEvent.Load)
    }

    QuickSetupScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        onContinue = onContinue,
        onOpenPreferences = onOpenPreferences,
    )
}

@Composable
fun QuickSetupScreen(
    state: QuickSetupUiState,
    onEvent: (QuickSetupEvent) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    onOpenPreferences: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showUniversityPicker by remember { mutableStateOf(false) }

    TheHouseScreen(
        title = "Configuración inicial",
        subtitle = "Personaliza tu cuenta en pocos pasos",
        onBack = onBack,
        modifier = modifier,
    ) { padding ->
        when {
            state.isLoading && state.profile == null && state.loadError == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    LoadingView(message = "Cargando configuración…")
                }
            }

            state.loadError != null && state.profile == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    ErrorStateView(
                        title = "No pudimos cargar la configuración",
                        message = state.loadError,
                        onRetry = { onEvent(QuickSetupEvent.Retry) },
                    )
                }
            }

            else -> {
                QuickSetupContent(
                    state = state,
                    onEvent = onEvent,
                    onContinue = onContinue,
                    onOpenPreferences = onOpenPreferences,
                    onOpenUniversityPicker = { showUniversityPicker = true },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = Spacing.xl, vertical = Spacing.lg),
                )
            }
        }
    }

    if (showUniversityPicker) {
        UniversityPickerDialog(
            selectedId = state.selectedUniversityId,
            universities = state.universities,
            isLoading = state.universitiesLoading,
            error = state.universitiesError,
            isSaving = state.isSaving,
            onDismiss = { showUniversityPicker = false },
            onRetry = { onEvent(QuickSetupEvent.RetryUniversities) },
            onSelect = {
                onEvent(QuickSetupEvent.UniversitySelected(it))
                showUniversityPicker = false
            },
        )
    }
}

@Composable
private fun QuickSetupContent(
    state: QuickSetupUiState,
    onEvent: (QuickSetupEvent) -> Unit,
    onContinue: () -> Unit,
    onOpenPreferences: () -> Unit,
    onOpenUniversityPicker: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedUniversity = state.universities.firstOrNull { it.id == state.selectedUniversityId }
        ?: state.profile?.university

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        state.message?.let { InlineMessage(text = it, tone = StatusTone.Success) }
        state.actionError?.let { InlineMessage(text = it, tone = StatusTone.Error) }

        TheHouseCard {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                SectionHeader(
                    title = "Estado",
                    subtitle = if (state.isCompleted) {
                        "Tu configuración inicial ya está completa."
                    } else {
                        "Completa los datos básicos para empezar."
                    },
                )
                StatusChip(
                    label = if (state.isCompleted) "Completa" else "Pendiente",
                    tone = if (state.isCompleted) StatusTone.Success else StatusTone.Warning,
                )
            }
        }

        TheHouseCard {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                SectionHeader(title = "Tu nombre", subtitle = "Opcional, puedes editarlo después")
                TheHouseTextField(
                    value = state.nameDraft,
                    onValueChange = { onEvent(QuickSetupEvent.NameChanged(it)) },
                    label = "Nombre",
                    placeholder = "Tu nombre",
                    enabled = !state.isSaving,
                    singleLine = true,
                )
            }
        }

        TheHouseCard {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                SectionHeader(
                    title = "Universidad",
                    subtitle = "Selecciona tu universidad para personalizar resultados",
                )
                InfoRow(
                    label = "Seleccionada",
                    value = selectedUniversity?.name ?: "Sin seleccionar",
                )
                if (selectedUniversity != null && selectedUniversity.city.isNotBlank()) {
                    InfoRow(label = "Ciudad", value = selectedUniversity.city)
                }
                if (state.universitiesError != null && state.universities.isEmpty()) {
                    InlineMessage(text = state.universitiesError, tone = StatusTone.Error)
                }
                TheHouseSecondaryButton(
                    text = if (selectedUniversity == null) "Seleccionar universidad" else "Cambiar universidad",
                    onClick = onOpenUniversityPicker,
                    icon = Icons.Outlined.School,
                    loading = state.universitiesLoading && state.universities.isEmpty(),
                )
            }
        }

        TheHouseCard(onClick = onOpenPreferences) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Tune,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Preferencias de búsqueda",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Configura presupuesto, distancia y otros filtros.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        TheHousePrimaryButton(
            text = "Guardar cambios",
            onClick = { onEvent(QuickSetupEvent.Save) },
            loading = state.isSaving,
        )

        TheHouseSecondaryButton(
            text = if (state.isCompleted) "Continuar" else "Ir al inicio",
            onClick = onContinue,
            enabled = !state.isSaving,
        )
    }
}

@Composable
private fun UniversityPickerDialog(
    selectedId: String?,
    universities: List<University>,
    isLoading: Boolean,
    error: String?,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onSelect: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text("Selecciona tu universidad") },
        text = {
            Box(modifier = Modifier.fillMaxWidth()) {
                when {
                    isLoading -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = Spacing.lg),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CircularProgressIndicator(modifier = Modifier.padding(end = Spacing.xs))
                            Text("Cargando universidades…")
                        }
                    }

                    error != null -> {
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            InlineMessage(text = error, tone = StatusTone.Error)
                            TheHouseSecondaryButton(
                                text = "Reintentar",
                                onClick = onRetry,
                                enabled = !isSaving,
                            )
                        }
                    }

                    universities.isEmpty() -> {
                        Text(
                            text = "No hay universidades disponibles.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 360.dp),
                            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                        ) {
                            items(universities, key = { it.id }) { university ->
                                val selected = university.id == selectedId
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable(enabled = !isSaving) { onSelect(university.id) }
                                        .heightIn(min = Heights.button)
                                        .padding(vertical = Spacing.xs),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    RadioButton(
                                        selected = selected,
                                        onClick = if (isSaving) null else ({ onSelect(university.id) }),
                                        enabled = !isSaving,
                                    )
                                    Column(modifier = Modifier.padding(start = Spacing.xs)) {
                                        Text(
                                            text = university.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        if (university.city.isNotBlank()) {
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
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text(if (isSaving) "Guardando…" else "Cerrar")
            }
        },
    )
}
