package com.thehouse.app.presentation.landlord

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thehouse.app.domain.model.RequestStatus
import com.thehouse.app.presentation.components.Avatar
import com.thehouse.app.presentation.components.ErrorStateView
import com.thehouse.app.presentation.components.InlineMessage
import com.thehouse.app.presentation.components.InfoRow
import com.thehouse.app.presentation.components.LoadingView
import com.thehouse.app.presentation.components.StatusChip
import com.thehouse.app.presentation.components.StatusTone
import com.thehouse.app.presentation.components.TheHouseCard
import com.thehouse.app.presentation.components.TheHousePrimaryButton
import com.thehouse.app.presentation.components.TheHouseScreen
import com.thehouse.app.presentation.components.TheHouseSecondaryButton
import com.thehouse.app.presentation.components.TheHouseTextAction
import com.thehouse.app.presentation.components.TheHouseTextField
import com.thehouse.app.presentation.requests.RequestErrorMessages
import com.thehouse.app.ui.theme.Spacing

private val rejectionReasons = mapOf(
    "not_available" to "No disponible",
    "profile_mismatch" to "Perfil no compatible",
    "other" to "Otro",
)

@Composable
fun LandlordRequestDetailDestination(
    requestId: String,
    factory: ViewModelProvider.Factory,
    onBack: () -> Unit,
    onOpenConversation: (String) -> Unit,
    onCreateAgreement: (String) -> Unit = {},
) {
    val viewModel: LandlordRequestDetailViewModel = viewModel(
        key = "landlord-request-detail:$requestId",
        factory = factory,
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is LandlordRequestDetailEffect.OpenConversation ->
                    onOpenConversation(effect.conversationId)
            }
        }
    }
    LandlordRequestDetailScreen(
        state = state,
        onBack = onBack,
        onRetry = { viewModel.onEvent(LandlordRequestDetailEvent.Load) },
        onAccept = { viewModel.onEvent(LandlordRequestDetailEvent.Accept) },
        onReject = { reason, note ->
            viewModel.onEvent(LandlordRequestDetailEvent.Reject(reason, note))
        },
        onOpenConversation = onOpenConversation,
        onCreateAgreement = { onCreateAgreement(requestId) },
    )
}

@Composable
fun LandlordRequestDetailScreen(
    state: LandlordRequestDetailUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onAccept: () -> Unit,
    onReject: (reason: String, note: String?) -> Unit,
    modifier: Modifier = Modifier,
    onOpenConversation: (String) -> Unit = {},
    onCreateAgreement: () -> Unit = {},
) {
    var showRejectPanel by remember { mutableStateOf(false) }
    var selectedReason by remember { mutableStateOf("not_available") }
    var note by remember { mutableStateOf("") }

    TheHouseScreen(
        title = "Solicitud recibida",
        subtitle = state.request?.student?.name,
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
                    val studentName = request.student?.name ?: "Estudiante"

                    TheHouseCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Avatar(name = studentName)
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                            ) {
                                Text(
                                    text = studentName,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = request.room.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            StatusChip(
                                label = RequestErrorMessages.statusLabel(request.status),
                                tone = requestTone(request.status),
                            )
                        }
                        request.student?.university?.let { university ->
                            InfoRow("Universidad", university)
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

                    request.rejection?.let { rejection ->
                        TheHouseCard {
                            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                                Text(
                                    text = "Rechazo",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                InfoRow("Motivo", rejection.reason)
                                rejection.note?.takeIf { it.isNotBlank() }?.let { rejectionNote ->
                                    InfoRow("Nota", rejectionNote)
                                }
                            }
                        }
                    }

                    if (request.status == RequestStatus.ACCEPTED) {
                        TheHousePrimaryButton(
                            text = "Crear acuerdo",
                            onClick = onCreateAgreement,
                        )
                    }

                    request.conversationId?.takeIf { it.isNotBlank() }?.let { conversationId ->
                        TheHouseSecondaryButton(
                            text = "Abrir conversación",
                            onClick = { onOpenConversation(conversationId) },
                        )
                    }

                    state.errorMessage?.let { message ->
                        InlineMessage(text = message)
                    }

                    if (request.status == RequestStatus.PENDING && !showRejectPanel) {
                        TheHousePrimaryButton(
                            text = "Aceptar",
                            onClick = onAccept,
                            enabled = state.busyId == null,
                            loading = state.busyId != null,
                        )
                        TheHouseSecondaryButton(
                            text = "Rechazar",
                            onClick = { showRejectPanel = true },
                            enabled = state.busyId == null,
                            contentColor = MaterialTheme.colorScheme.error,
                        )
                    }

                    if (showRejectPanel && request.status == RequestStatus.PENDING) {
                        TheHouseCard {
                            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                                Text(
                                    text = "Motivo del rechazo",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                                    rejectionReasons.forEach { (reason, label) ->
                                        FilterChip(
                                            selected = selectedReason == reason,
                                            onClick = { selectedReason = reason },
                                            label = { Text(label) },
                                        )
                                    }
                                }
                                TheHouseTextField(
                                    value = note,
                                    onValueChange = { note = it.take(300) },
                                    label = if (selectedReason == "other") {
                                        "Nota (obligatoria)"
                                    } else {
                                        "Nota (opcional)"
                                    },
                                    supportingText = "${note.length}/300",
                                    singleLine = false,
                                    minLines = 2,
                                )
                                val noteError = selectedReason == "other" && note.isBlank()
                                if (noteError) {
                                    InlineMessage(
                                        text = "Escribe una nota para el motivo «Otro».",
                                        tone = StatusTone.Warning,
                                    )
                                }
                                TheHousePrimaryButton(
                                    text = "Confirmar rechazo",
                                    onClick = {
                                        onReject(
                                            selectedReason,
                                            note.takeIf { it.isNotBlank() },
                                        )
                                        showRejectPanel = false
                                        note = ""
                                    },
                                    enabled = state.busyId == null && !noteError,
                                    loading = state.busyId != null,
                                )
                                TheHouseTextAction(
                                    text = "Cancelar",
                                    onClick = { showRejectPanel = false },
                                )
                            }
                        }
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
