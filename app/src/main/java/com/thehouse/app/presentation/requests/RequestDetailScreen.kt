package com.thehouse.app.presentation.requests

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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thehouse.app.presentation.components.ErrorStateView
import com.thehouse.app.presentation.components.LoadingView
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
            if (effect is RequestDetailEffect.OpenConversation) Unit
        }
    }
    RequestDetailScreen(
        state = state,
        onBack = onBack,
        onRetry = { viewModel.onEvent(RequestDetailEvent.Load) },
        onCancel = { viewModel.onEvent(RequestDetailEvent.Cancel) },
        onOpenConversation = { conversationId ->
            state.request?.let { request ->
                if (request.conversationId == conversationId) onOpenConversation(conversationId)
            }
        },
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
    onOpenConversation: (String) -> Unit,
    onReportRoom: (String) -> Unit,
    onReportBuilding: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
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
                        request.room.title,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        "${request.room.city} · ${RequestErrorMessages.statusLabel(request.status)}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    request.message?.takeIf { it.isNotBlank() }?.let { message ->
                        Text("Mensaje", style = MaterialTheme.typography.titleMedium)
                        Text(message, style = MaterialTheme.typography.bodyLarge)
                    }
                    if (request.quickQuestions.isNotEmpty()) {
                        Text("Preguntas", style = MaterialTheme.typography.titleMedium)
                        request.quickQuestions.forEach { question ->
                            Text("· ${question.api}", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    request.landlord?.let { landlord ->
                        Text("Arrendador: ${landlord.name}", style = MaterialTheme.typography.bodyLarge)
                    }
                    request.student?.let { student ->
                        Text("Estudiante: ${student.name}", style = MaterialTheme.typography.bodyLarge)
                        student.university?.let { university ->
                            Text("Universidad: $university", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    request.visit?.let { visit ->
                        Text(
                            "Visita: ${formatInstant(visit.scheduledAt)} (${visit.status})",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                    request.rejection?.let { rejection ->
                        Text("Motivo: ${rejection.reason}", style = MaterialTheme.typography.bodyLarge)
                        rejection.note?.takeIf { it.isNotBlank() }?.let { note ->
                            Text("Nota: $note", style = MaterialTheme.typography.bodyLarge)
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
                    Text("Reportar", style = MaterialTheme.typography.titleMedium)
                    OutlinedButton(
                        onClick = { onReportRoom(request.room.id) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) {
                        Text("Reportar cuarto")
                    }
                    OutlinedButton(
                        onClick = { onReportBuilding(request.room.buildingId) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) {
                        Text("Reportar edificio")
                    }
                    if (request.status == com.thehouse.app.domain.model.RequestStatus.PENDING) {
                        OutlinedButton(
                            onClick = onCancel,
                            enabled = !state.isCancelling,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        ) {
                            if (state.isCancelling) {
                                CircularProgressIndicator(modifier = Modifier.heightIn(max = 18.dp))
                            } else {
                                Text("Cancelar solicitud")
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatInstant(instant: java.time.Instant): String =
    DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")
        .withZone(ZoneId.systemDefault())
        .format(instant)
