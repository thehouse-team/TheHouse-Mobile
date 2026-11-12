package com.thehouse.app.presentation.requests

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thehouse.app.domain.model.QuickQuestion
import com.thehouse.app.presentation.components.ErrorStateView

private val quickQuestionLabels = mapOf(
    QuickQuestion.AVAILABLE_NOW to "¿Disponible ahora?",
    QuickQuestion.SERVICES_INCLUDED to "¿Qué servicios están incluidos?",
    QuickQuestion.DEPOSIT to "¿Cuánto es el depósito?",
    QuickQuestion.MINIMUM_CONTRACT to "¿Cuál es el contrato mínimo?",
    QuickQuestion.VISIT_POSSIBLE to "¿Se pueden hacer visitas?",
    QuickQuestion.PETS_ALLOWED to "¿Se aceptan mascotas?",
    QuickQuestion.ROOMMATES to "¿Hay roommates?",
)

/** Composer de solicitud: Room Detail → Solicitar → quickQuestions/message → enviar. */
@Composable
fun CreateRequestDestination(
    roomId: String,
    factory: ViewModelProvider.Factory,
    onBack: () -> Unit,
    onCreated: (com.thehouse.app.domain.model.RoomRequest) -> Unit,
) {
    val viewModel: CreateRequestViewModel = viewModel(
        key = "create-request:$roomId",
        factory = factory,
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is CreateRequestEffect.Created -> onCreated(effect.request)
                CreateRequestEffect.OpenConversation -> Unit
            }
        }
    }
    CreateRequestScreen(
        state = state,
        onBack = onBack,
        onToggleQuestion = { viewModel.onEvent(CreateRequestEvent.QuickQuestionToggled(it)) },
        onMessageChanged = { viewModel.onEvent(CreateRequestEvent.MessageChanged(it)) },
        onSend = { viewModel.onEvent(CreateRequestEvent.Send) },
    )
}

@Composable
fun CreateRequestScreen(
    state: CreateRequestUiState,
    onBack: () -> Unit,
    onToggleQuestion: (QuickQuestion) -> Unit,
    onMessageChanged: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TextButton(
                onClick = onBack,
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text("Volver")
            }
            Text(
                "Solicitar este cuarto",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                "Selecciona preguntas o escribe un mensaje al arrendador.",
                style = MaterialTheme.typography.bodyMedium,
            )

            quickQuestionLabels.forEach { (question, label) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = question in state.quickQuestions,
                        onCheckedChange = { onToggleQuestion(question) },
                        modifier = Modifier.heightIn(min = 48.dp),
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .semantics { contentDescription = label },
                    )
                }
            }

            OutlinedTextField(
                value = state.message,
                onValueChange = onMessageChanged,
                label = { Text("Mensaje (opcional)") },
                supportingText = {
                    Text("${state.message.length}/500")
                },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
            )

            state.errorMessage?.let { message ->
                ErrorStateView(title = "No se pudo enviar", message = message, onRetry = onSend)
            }

            Button(
                onClick = onSend,
                enabled = !state.isSending,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                if (state.isSending) {
                    CircularProgressIndicator(modifier = Modifier.heightIn(max = 20.dp))
                } else {
                    Text("Enviar solicitud")
                }
            }
        }
    }
}
