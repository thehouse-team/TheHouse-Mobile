package com.thehouse.app.presentation.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thehouse.app.domain.model.ReportReason
import com.thehouse.app.domain.model.ReportTargetType
import com.thehouse.app.presentation.components.InlineMessage
import com.thehouse.app.presentation.components.StatusTone
import com.thehouse.app.presentation.components.TheHouseCard
import com.thehouse.app.presentation.components.TheHousePrimaryButton
import com.thehouse.app.presentation.components.TheHouseScreen
import com.thehouse.app.presentation.components.TheHouseTextField
import com.thehouse.app.ui.theme.IconSizes
import com.thehouse.app.ui.theme.Spacing
import com.thehouse.app.ui.theme.theHouseColors

@Composable
fun ReportDestination(
    targetType: ReportTargetType,
    targetId: String,
    factory: com.thehouse.app.presentation.U2ViewModelFactory,
    onBack: () -> Unit,
) {
    val reportFactory = remember(targetType, targetId) {
        factory.report(targetType.api, targetId)
    }
    val viewModel: ReportViewModel = viewModel(factory = reportFactory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ReportScreen(
        state = state,
        onBack = onBack,
        onEvent = { viewModel.onEvent(it) },
    )
}

private fun reasonLabel(reason: ReportReason): String = when (reason) {
    ReportReason.FAKE -> "Es falso"
    ReportReason.FRAUD -> "Fraude"
    ReportReason.INAPPROPRIATE -> "Contenido inapropiado"
    ReportReason.SPAM -> "Spam"
    ReportReason.OTHER -> "Otro"
}

private fun targetLabel(targetType: ReportTargetType): String = when (targetType) {
    ReportTargetType.BUILDING -> "el edificio"
    ReportTargetType.ROOM -> "el cuarto"
    ReportTargetType.USER -> "la cuenta"
}

@Composable
fun ReportScreen(
    state: ReportUiState,
    onBack: () -> Unit,
    onEvent: (ReportEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showConfirm by remember { mutableStateOf(false) }
    TheHouseScreen(
        title = "Reportar contenido",
        subtitle = "Tu reporte será revisado por el equipo",
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
                state.submitted -> {
                    ReportSubmittedCard(onBack = onBack)
                }
                else -> {
                    TheHouseCard {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(IconSizes.lg),
                            )
                            Text(
                                text = "Vas a reportar ${targetLabel(state.targetType)}.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        Text(
                            text = "Motivo",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.semantics { heading() },
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                            contentPadding = PaddingValues(horizontal = Spacing.xs),
                        ) {
                            items(ReportReason.entries) { reason ->
                                FilterChip(
                                    selected = state.reason == reason,
                                    onClick = { onEvent(ReportEvent.ReasonChanged(reason)) },
                                    label = { Text(reasonLabel(reason)) },
                                )
                            }
                        }
                    }

                    TheHouseTextField(
                        value = state.details,
                        onValueChange = { onEvent(ReportEvent.DetailsChanged(it)) },
                        label = "Detalles (opcional)",
                        placeholder = "Describe lo que ocurre",
                        isError = state.detailsError != null,
                        errorMessage = state.detailsError,
                        supportingText = "${state.details.length}/${ReportUiState.MAX_DETAILS}",
                        singleLine = false,
                        minLines = 4,
                    )

                    state.errorMessage?.let { message ->
                        InlineMessage(text = message, tone = StatusTone.Error)
                    }

                    TheHousePrimaryButton(
                        text = if (state.isSubmitting) "Enviando…" else "Enviar reporte",
                        onClick = { showConfirm = true },
                        enabled = state.isValid,
                        loading = state.isSubmitting,
                    )
                }
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Confirmar reporte") },
            text = { Text("Se enviará tu reporte para revisión del equipo de TheHouse.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirm = false
                        onEvent(ReportEvent.Submit)
                    },
                ) { Text("Enviar") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("Cancelar") }
            },
        )
    }
}

@Composable
private fun ReportSubmittedCard(onBack: () -> Unit) {
    TheHouseCard {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.theHouseColors.success,
                modifier = Modifier.size(IconSizes.xl),
            )
            Text(
                text = "Reporte enviado",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Gracias. Tu reporte fue enviado y será revisado.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TheHousePrimaryButton(text = "Listo", onClick = onBack)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ReportScreenPreview() {
    com.thehouse.app.ui.theme.TheHouseTheme {
        ReportScreen(
            state = ReportUiState(
                targetType = ReportTargetType.BUILDING,
                targetId = "preview",
                reason = ReportReason.FRAUD,
            ),
            onBack = {},
            onEvent = {},
        )
    }
}
