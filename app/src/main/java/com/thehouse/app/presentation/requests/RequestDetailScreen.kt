package com.thehouse.app.presentation.requests

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thehouse.app.domain.model.RequestStatus
import com.thehouse.app.presentation.components.ErrorStateView
import com.thehouse.app.presentation.components.InfoRow
import com.thehouse.app.presentation.components.LoadingView
import com.thehouse.app.presentation.components.StatusChip
import com.thehouse.app.presentation.components.StatusTone
import com.thehouse.app.presentation.components.TheHouseCard
import com.thehouse.app.presentation.components.TheHousePrimaryButton
import com.thehouse.app.presentation.components.TheHouseScreen
import com.thehouse.app.presentation.components.TheHouseSecondaryButton
import com.thehouse.app.ui.theme.Spacing
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun RequestDetailDestination(
    requestId: String,
    factory: ViewModelProvider.Factory,
    onBack: () -> Unit,
    onOpenConversation: (String) -> Unit,
    onReportRoom: (String) -> Unit,
    onReportBuilding: (String) -> Unit,
) {
    val viewModel: RequestDetailViewModel = viewModel(
        key = "request-detail:$requestId",
        factory = factory,
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is RequestDetailEffect.OpenConversation -> onOpenConversation(effect.conversationId)
            }
        }
    }
    RequestDetailScreen(
        state = state,
        onBack = onBack,
        onRetry = { viewModel.onEvent(RequestDetailEvent.Load) },
        onCancel = { viewModel.onEvent(RequestDetailEvent.Cancel) },
        onOpenConversation = { viewModel.onEvent(RequestDetailEvent.OpenConversation) },
        onReportRoom = { roomId -> onReportRoom(roomId) },
        onReportBuilding = { buildingId -> onReportBuilding(buildingId) },
    )
}

@Composable
fun RequestDetailScreen(
    state: RequestDetailUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
    onOpenConversation: () -> Unit,
    onReportRoom: (String) -> Unit,
    onReportBuilding: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    TheHouseScreen(
        title = "Detalle de solicitud",
        onBack = onBack,
        modifier = modifier,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Spacing.xl, vertical = Spacing.lg)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            when {
                state.isLoading -> LoadingView(
                    message = "Cargando solicitud…",
                    modifier = Modifier.fillMaxWidth(),
                )
                state.errorMessage != null -> ErrorStateView(
                    title = "No se pudo cargar",
                    message = state.errorMessage,
                    onRetry = onRetry,
                )
                state.request == null -> Unit
                else -> {
                    val request = state.request ?: return@Column
                    TheHouseCard {
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = request.room.title,
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .weight(1f)
                                        .semantics { heading() },
                                )
                                StatusChip(
                                    label = RequestErrorMessages.statusLabel(request.status),
                                    tone = requestTone(request.status),
                                )
                            }
                            Text(
                                text = request.room.city,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    request.message?.takeIf { it.isNotBlank() }?.let { message ->
                        TheHouseCard {
                            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                                Text(
                                    text = "Mensaje",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = message,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    if (request.quickQuestions.isNotEmpty()) {
                        TheHouseCard {
                            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                                Text(
                                    text = "Preguntas",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                request.quickQuestions.forEach { question ->
                                    Text(
                                        text = "· ${QuickQuestionLabels.label(question)}",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }

                    TheHouseCard {
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            Text(
                                text = "Detalles",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            request.landlord?.let { landlord ->
                                InfoRow("Arrendador", landlord.name)
                            }
                            request.student?.let { student ->
                                InfoRow("Estudiante", student.name)
                                student.university?.let { university ->
                                    InfoRow("Universidad", university)
                                }
                            }
                            request.visit?.let { visit ->
                                InfoRow(
                                    "Visita",
                                    "${formatInstant(visit.scheduledAt)} (${visit.status})",
                                )
                            }
                            request.rejection?.let { rejection ->
                                InfoRow("Motivo", rejection.reason)
                                rejection.note?.takeIf { it.isNotBlank() }?.let { note ->
                                    InfoRow("Nota", note)
                                }
                            }
                        }
                    }

                    request.conversationId?.takeIf { it.isNotBlank() }?.let {
                        TheHousePrimaryButton(
                            text = "Abrir conversación",
                            onClick = onOpenConversation,
                        )
                    }

                    TheHouseCard {
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            Text(
                                text = "Reportar",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            TheHouseSecondaryButton(
                                text = "Reportar cuarto",
                                onClick = { onReportRoom(request.room.id) },
                            )
                            TheHouseSecondaryButton(
                                text = "Reportar edificio",
                                onClick = { onReportBuilding(request.room.buildingId) },
                            )
                        }
                    }

                    if (request.status == RequestStatus.PENDING) {
                        TheHouseSecondaryButton(
                            text = if (state.isCancelling) "Cancelando…" else "Cancelar solicitud",
                            onClick = onCancel,
                            enabled = !state.isCancelling,
                            loading = state.isCancelling,
                            contentColor = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
}

private fun requestTone(status: RequestStatus): StatusTone = when (status) {
    RequestStatus.PENDING -> StatusTone.Warning
    RequestStatus.ACCEPTED -> StatusTone.Success
    RequestStatus.REJECTED -> StatusTone.Error
    RequestStatus.CANCELLED -> StatusTone.Neutral
}

private fun formatInstant(instant: java.time.Instant): String =
    DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")
        .withZone(ZoneId.systemDefault())
        .format(instant)
