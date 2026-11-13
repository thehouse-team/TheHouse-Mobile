package com.thehouse.app.presentation.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thehouse.app.domain.model.ReportReason
import com.thehouse.app.domain.model.ReportTargetType
import com.thehouse.app.presentation.components.LoadingView
import com.thehouse.app.ui.theme.Heights
import com.thehouse.app.ui.theme.Spacing

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
                "Reportar contenido",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { heading() },
            )

            when {
                state.submitted -> {
                    Text(
                        "Gracias. Tu reporte fue enviado y será revisado.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Button(
                        onClick = onBack,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = Heights.button),
                    ) { Text("Listo") }
                }
                else -> {
                    Text(
                        "Vas a reportar ${targetLabel(state.targetType)}.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text("Motivo", style = MaterialTheme.typography.labelLarge)
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        ReportReason.entries.chunked(2).forEach { rowReasons ->
                            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                                rowReasons.forEach { reason ->
                                    FilterChip(
                                        selected = state.reason == reason,
                                        onClick = { onEvent(ReportEvent.ReasonChanged(reason)) },
                                        label = { Text(reasonLabel(reason)) },
                                    )
                                }
                            }
                        }
                    }
                    OutlinedTextField(
                        value = state.details,
                        onValueChange = { onEvent(ReportEvent.DetailsChanged(it)) },
                        label = { Text("Detalles (opcional)") },
                        isError = state.detailsError != null,
                        supportingText = {
                            Text("${state.details.length}/${ReportUiState.MAX_DETAILS}")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp),
                    )
                    state.errorMessage?.let {
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Button(
                        onClick = { showConfirm = true },
                        enabled = !state.isSubmitting && state.isValid,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = Heights.button),
                    ) {
                        Text(if (state.isSubmitting) "Enviando…" else "Enviar reporte")
                    }
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
