package com.thehouse.app.presentation.profile

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
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Person
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thehouse.app.domain.model.University
import com.thehouse.app.presentation.U2ViewModelFactory
import com.thehouse.app.presentation.components.Avatar
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
fun ProfileDestination(
    factory: U2ViewModelFactory,
    onBack: () -> Unit,
    onOpenPreferences: () -> Unit,
    onOpenQuickSetup: () -> Unit,
) {
    val viewModel: ProfileViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.onEvent(ProfileEvent.Load)
    }

    ProfileScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        onOpenPreferences = onOpenPreferences,
        onOpenQuickSetup = onOpenQuickSetup,
    )
}

@Composable
fun ProfileScreen(
    state: ProfileUiState,
    onEvent: (ProfileEvent) -> Unit,
    onBack: () -> Unit,
    onOpenPreferences: () -> Unit,
    onOpenQuickSetup: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TheHouseScreen(
        title = "Mi perfil",
        subtitle = "Tu información y configuración",
        onBack = onBack,
        modifier = modifier,
    ) { padding ->
        when {
            state.isLoading && state.profile == null && state.error == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    LoadingView(message = "Cargando perfil…")
                }
            }

            state.error != null && state.profile == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    ErrorStateView(
                        title = "No pudimos cargar tu perfil",
                        message = state.error,
                        onRetry = { onEvent(ProfileEvent.Retry) },
                    )
                }
            }

            else -> {
                ProfileContent(
                    state = state,
                    onEvent = onEvent,
                    onOpenPreferences = onOpenPreferences,
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

    if (state.showUniversityPicker) {
        UniversityPickerDialog(
            selectedId = state.profile?.university?.id,
            universities = state.universities,
            isLoading = state.universitiesLoading,
            error = state.universitiesError,
            isSaving = state.isSavingUniversity,
            onDismiss = { onEvent(ProfileEvent.DismissUniversityPicker) },
            onRetry = { onEvent(ProfileEvent.RetryUniversities) },
            onSelect = { onEvent(ProfileEvent.SelectUniversity(it)) },
        )
    }
}

@Composable
private fun ProfileContent(
    state: ProfileUiState,
    onEvent: (ProfileEvent) -> Unit,
    onOpenPreferences: () -> Unit,
    onOpenQuickSetup: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        state.message?.let { InlineMessage(text = it, tone = StatusTone.Success) }
        state.actionError?.let { InlineMessage(text = it, tone = StatusTone.Error) }

        ProfileHeaderCard(state = state)

        QuickSetupCard(
            completed = state.quickSetupCompleted,
            onOpenQuickSetup = onOpenQuickSetup,
        )

        NameCard(state = state, onEvent = onEvent)

        UniversityCard(state = state, onEvent = onEvent)

        PreferencesCard(onOpenPreferences = onOpenPreferences)
    }
}

@Composable
private fun ProfileHeaderCard(state: ProfileUiState) {
    val displayName = state.profile?.name?.takeIf { it.isNotBlank() } ?: "Tu perfil"
    TheHouseCard {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Avatar(name = displayName)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    state.email?.takeIf { it.isNotBlank() }?.let { email ->
                        Text(
                            text = email,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            val university = state.profile?.university
            InfoRow(
                label = "Universidad",
                value = university?.name ?: "Sin seleccionar",
            )
            if (university != null && university.city.isNotBlank()) {
                InfoRow(label = "Ciudad", value = university.city)
            }
        }
    }
}

@Composable
private fun QuickSetupCard(
    completed: Boolean,
    onOpenQuickSetup: () -> Unit,
) {
    TheHouseCard {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            SectionHeader(
                title = "Configuración inicial",
                subtitle = if (completed) {
                    "Tu configuración básica está completa."
                } else {
                    "Completa tu perfil para personalizar tu experiencia."
                },
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusChip(
                    label = if (completed) "Completa" else "Pendiente",
                    tone = if (completed) StatusTone.Success else StatusTone.Warning,
                )
                if (!completed) {
                    TheHouseSecondaryButton(
                        text = "Completar",
                        onClick = onOpenQuickSetup,
                        modifier = Modifier.fillMaxWidth(0.55f),
                    )
                } else {
                    TheHouseSecondaryButton(
                        text = "Ver",
                        onClick = onOpenQuickSetup,
                        modifier = Modifier.fillMaxWidth(0.4f),
                    )
                }
            }
        }
    }
}

@Composable
private fun NameCard(
    state: ProfileUiState,
    onEvent: (ProfileEvent) -> Unit,
) {
    TheHouseCard {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            SectionHeader(
                title = "Nombre",
                subtitle = "Cómo te identificas en TheHouse",
            )
            if (state.isEditingName) {
                TheHouseTextField(
                    value = state.nameDraft,
                    onValueChange = { onEvent(ProfileEvent.NameChanged(it)) },
                    label = "Nombre",
                    placeholder = "Tu nombre",
                    enabled = !state.isSavingName,
                    singleLine = true,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    TheHouseSecondaryButton(
                        text = "Cancelar",
                        onClick = { onEvent(ProfileEvent.CancelEditName) },
                        modifier = Modifier.weight(1f),
                        enabled = !state.isSavingName,
                    )
                    TheHousePrimaryButton(
                        text = "Guardar",
                        onClick = { onEvent(ProfileEvent.SaveName) },
                        modifier = Modifier.weight(1f),
                        loading = state.isSavingName,
                    )
                }
            } else {
                InfoRow(
                    label = "Nombre actual",
                    value = state.profile?.name?.takeIf { it.isNotBlank() } ?: "Sin nombre",
                )
                TheHouseSecondaryButton(
                    text = "Editar nombre",
                    onClick = { onEvent(ProfileEvent.EditName) },
                    icon = Icons.Outlined.Edit,
                )
            }
        }
    }
}

@Composable
private fun UniversityCard(
    state: ProfileUiState,
    onEvent: (ProfileEvent) -> Unit,
) {
    val university = state.profile?.university
    TheHouseCard {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            SectionHeader(
                title = "Universidad",
                subtitle = "Se usa para personalizar tu búsqueda",
            )
            if (university != null) {
                InfoRow(label = "Nombre", value = university.name)
                if (university.city.isNotBlank()) {
                    InfoRow(label = "Ciudad", value = university.city)
                }
            } else {
                Text(
                    text = "Aún no has seleccionado una universidad.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TheHouseSecondaryButton(
                text = if (university == null) "Seleccionar universidad" else "Cambiar universidad",
                onClick = { onEvent(ProfileEvent.OpenUniversityPicker) },
                icon = Icons.Outlined.School,
                loading = state.isSavingUniversity,
            )
        }
    }
}

@Composable
private fun PreferencesCard(onOpenPreferences: () -> Unit) {
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
                    text = "Preferencias",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Ajusta tu búsqueda y recomendaciones.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
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
