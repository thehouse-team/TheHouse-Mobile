package com.thehouse.app.presentation.chat

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
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
fun ConversationDestination(
    conversationId: String,
    initialClosed: Boolean,
    factory: ViewModelProvider.Factory,
    onBack: () -> Unit,
) {
    val viewModel: ConversationViewModel = viewModel(
        key = "conversation:$conversationId",
        factory = factory,
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // El polling vive mientras la pantalla está activa; se detiene al salir.
    LaunchedEffect(viewModel) {
        viewModel.onEvent(ConversationEvent.Load)
    }
    DisposableEffect(viewModel) {
        onDispose { viewModel.onEvent(ConversationEvent.StopPolling) }
    }
    ConversationScreen(
        state = state,
        onBack = onBack,
        onRetry = { viewModel.onEvent(ConversationEvent.Load) },
        onComposerChanged = { viewModel.onEvent(ConversationEvent.ComposerChanged(it)) },
        onSend = { viewModel.onEvent(ConversationEvent.Send) },
    )
}

@Composable
fun ConversationScreen(
    state: ConversationUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onComposerChanged: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
    }
    Scaffold(modifier = modifier) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Volver")
            }

            when {
                state.isLoadingInitial -> LoadingView(
                    message = "Cargando mensajes…",
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
                state.errorMessage != null && state.messages.isEmpty() -> ErrorStateView(
                    title = "No se pudieron cargar",
                    message = state.errorMessage,
                    onRetry = onRetry,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("conversation_messages"),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (state.hasMoreHistory) {
                            item {
                                Text(
                                    "Hay mensajes anteriores.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 4.dp),
                                )
                            }
                        }
                        items(state.messages, key = { it.id }) { message ->
                            MessageBubble(message = message)
                        }
                    }
                    state.errorMessage?.let { message ->
                        Text(
                            message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.semantics {
                                contentDescription = "Error: $message"
                            },
                        )
                    }
                    Composer(
                        text = state.composerText,
                        enabled = !state.isClosed && !state.isSending,
                        isClosed = state.isClosed,
                        onTextChanged = onComposerChanged,
                        onSend = onSend,
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: com.thehouse.app.domain.model.ChatMessage,
    modifier: Modifier = Modifier,
) {
    // mine/counterpart diferenciados por alineación Y etiqueta (no solo color).
    val alignment = if (message.mine) Alignment.End else Alignment.Start
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = alignment,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = if (message.mine) "Tú" else message.senderRole,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box(
            modifier = Modifier
                .background(
                    color = if (message.mine) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = RoundedCornerShape(12.dp),
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.semantics {
                    contentDescription = (if (message.mine) "Tú: " else "Mensaje: ") + message.text
                },
            )
        }
        message.createdAt?.let { createdAt ->
            Text(
                text = formatInstant(createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Composer(
    text: String,
    enabled: Boolean,
    isClosed: Boolean,
    onTextChanged: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (isClosed) {
            Text(
                "Esta conversación está cerrada.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChanged,
                enabled = !isClosed,
                placeholder = { Text("Escribe un mensaje…") },
                supportingText = { Text("${text.length}/1000") },
                modifier = Modifier.weight(1f),
                maxLines = 4,
            )
            if (isClosed) {
                OutlinedButton(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text("Enviar")
                }
            } else {
                Button(
                    onClick = onSend,
                    // send disabled si vacío o mientras envía.
                    enabled = enabled && text.isNotBlank(),
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(if (text.isNotBlank() && enabled) "Enviar" else "Enviar")
                }
            }
        }
    }
}

private fun formatInstant(instant: java.time.Instant): String =
    DateTimeFormatter.ofPattern("HH:mm")
        .withZone(ZoneId.systemDefault())
        .format(instant)
