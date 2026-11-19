package com.thehouse.app.presentation.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thehouse.app.presentation.components.ErrorStateView
import com.thehouse.app.presentation.components.LoadingView
import com.thehouse.app.presentation.components.TheHouseScreen
import com.thehouse.app.presentation.components.TheHouseTextField
import com.thehouse.app.ui.theme.Heights
import com.thehouse.app.ui.theme.IconSizes
import com.thehouse.app.ui.theme.Spacing
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
    TheHouseScreen(
        title = "Conversación",
        subtitle = if (state.isClosed) "Cerrada" else null,
        onBack = onBack,
        modifier = modifier,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            when {
                state.isLoadingInitial -> LoadingView(
                    message = "Cargando mensajes…",
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
                state.errorMessage != null && state.messages.isEmpty() -> ErrorStateView(
                    title = "No se pudieron cargar",
                    message = state.errorMessage,
                    onRetry = onRetry,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("conversation_messages"),
                        verticalArrangement = Arrangement.spacedBy(Spacing.md),
                    ) {
                        if (state.hasMoreHistory) {
                            item {
                                Text(
                                    text = "Hay mensajes anteriores.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = Spacing.xs),
                                )
                            }
                        }
                        items(state.messages, key = { it.id }) { message ->
                            MessageBubble(message = message)
                        }
                    }
                    state.errorMessage?.let { message ->
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        ) {
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = Spacing.md, vertical = Spacing.sm)
                                    .semantics {
                                        contentDescription = "Error: $message"
                                    },
                            )
                        }
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
    val shape = if (message.mine) {
        RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp,
            bottomStart = 16.dp,
            bottomEnd = 4.dp,
        )
    } else {
        RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp,
            bottomStart = 4.dp,
            bottomEnd = 16.dp,
        )
    }
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = alignment,
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Text(
            text = if (message.mine) "Tú" else message.senderRole,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Surface(
            shape = shape,
            color = if (message.mine) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            },
            contentColor = if (message.mine) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.widthIn(max = 320.dp),
        ) {
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .padding(horizontal = Spacing.md, vertical = Spacing.sm)
                    .semantics {
                        contentDescription =
                            (if (message.mine) "Tú: " else "Mensaje: ") + message.text
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
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        if (isClosed) {
            Text(
                text = "Esta conversación está cerrada.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.Bottom,
        ) {
            TheHouseTextField(
                value = text,
                onValueChange = onTextChanged,
                label = "Mensaje",
                placeholder = "Escribe un mensaje…",
                supportingText = "${text.length}/1000",
                enabled = !isClosed,
                singleLine = false,
                minLines = 1,
                maxLines = 4,
                modifier = Modifier.weight(1f),
            )
            FilledIconButton(
                onClick = onSend,
                enabled = enabled && text.isNotBlank(),
                modifier = Modifier.size(Heights.button),
                shape = MaterialTheme.shapes.medium,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Send,
                    contentDescription = "Enviar",
                    modifier = Modifier.size(IconSizes.md),
                )
            }
        }
    }
}

private fun formatInstant(instant: java.time.Instant): String =
    DateTimeFormatter.ofPattern("HH:mm")
        .withZone(ZoneId.systemDefault())
        .format(instant)
