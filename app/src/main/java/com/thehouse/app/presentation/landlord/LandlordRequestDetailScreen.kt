package com.thehouse.app.presentation.landlord

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thehouse.app.domain.error.RequestErrorCode
import com.thehouse.app.domain.model.RequestStatus
import com.thehouse.app.presentation.components.ErrorStateView
import com.thehouse.app.presentation.components.LoadingView
import com.thehouse.app.presentation.requests.RequestErrorMessages

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
                is LandlordRequestDetailEffect.OpenConversation -> onOpenConversation(effect.conversationId)
            }
        }
    }
    LandlordRequestDetailScreen(
        state = state,
        onBack = onBack,
        onRetry = { viewModel.onEvent(LandlordRequestDetailEvent.Load) },
        onAccept = { viewModel.onEvent(LandlordRequestDetailEvent.Accept) },
        onReject = { reason, note -> viewModel.onEvent(LandlordRequestDetailEvent.Reject(reason, note)) },
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
    onOpenConversation: (String) -> Unit = {},
    onCreateAgreement: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var showRejectPanel by remember { mutableStateOf(false) }
    var selectedReason by remember { mutableStateOf("not_available") }
    var note by remember { mutableStateOf("") }

    Scaffold(modifier = modifier) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Volver")
            }
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
                    Text(
                        "Solicitud de ${request.student?.name ?: "estudiante"}",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        "${request.room.title} · ${RequestErrorMessages.statusLabel(request.status)}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    request.student?.university?.let { university ->
                        Text("Universidad: $university", style = MaterialTheme.typography.bodyLarge)
                    }
                    request.message?.takeIf { it.isNotBlank() }?.let { message ->
                        Text("Mensaje", style = MaterialTheme.typography.titleMedium)
                        Text(message, style = MaterialTheme.typography.bodyLarge)
                    }
                    request.rejection?.let { rejection ->
                        Text("Motivo: ${rejection.reason}", style = MaterialTheme.typography.bodyLarge)
                        rejection.note?.takeIf { it.isNotBlank() }?.let { note ->
                            Text("Nota: $note", style = MaterialTheme.typography.bodyLarge)
                        }
                    }

                    if (request.status == RequestStatus.ACCEPTED) {
                        OutlinedButton(
                            onClick = onCreateAgreement,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        ) {
                            Text("Crear acuerdo")
                        }
                    }

                    request.conversationId?.takeIf { it.isNotBlank() }?.let { conversationId ->
                        Button(
                            onClick = { onOpenConversation(conversationId) },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        ) {
                            Text("Abrir conversación")
                        }
                    }

                    state.errorMessage?.let { message ->
                        ErrorStateView(title = "No se pudo realizar", message = message, onRetry = onRetry)
                    }

                    if (request.status == RequestStatus.PENDING && !showRejectPanel) {
                        Button(
                            onClick = onAccept,
                            enabled = state.busyId == null,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        ) {
                            if (state.busyId != null) {
                                CircularProgressIndicator(modifier = Modifier.heightIn(max = 18.dp))
                            } else {
                                Text("Aceptar")
                            }
                        }
                        OutlinedButton(
                            onClick = { showRejectPanel = true },
                            enabled = state.busyId == null,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        ) {
                            Text("Rechazar")
                        }
                    }

                    if (showRejectPanel && request.status == RequestStatus.PENDING) {
                        Text("Motivo del rechazo", style = MaterialTheme.typography.titleMedium)
                        rejectionReasons.forEach { (reason, label) ->
                            OutlinedButton(
                                onClick = { selectedReason = reason },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            ) {
                                Text(
                                    (if (selectedReason == reason) "● " else "○ ") + label,
                                )
                            }
                        }
                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it.take(300) },
                            label = {
                                Text(
                                    if (selectedReason == "other") "Nota (obligatoria)" else "Nota (opcional)",
                                )
                            },
                            supportingText = { Text("${note.length}/300") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                        )
                        val noteError = selectedReason == "other" && note.isBlank()
                        Button(
                            onClick = {
                                onReject(
                                    selectedReason,
                                    note.takeIf { it.isNotBlank() },
                                )
                                showRejectPanel = false
                                note = ""
                            },
                            enabled = state.busyId == null && !noteError,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        ) {
                            Text("Confirmar rechazo")
                        }
                        TextButton(
                            onClick = { showRejectPanel = false },
                            modifier = Modifier.heightIn(min = 48.dp),
                        ) {
                            Text("Cancelar")
                        }
                    }
                }
            }
        }
    }
}
