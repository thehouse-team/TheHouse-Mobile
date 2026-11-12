package com.thehouse.app.presentation.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Badge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thehouse.app.domain.model.Conversation
import com.thehouse.app.domain.model.ConversationStatus
import com.thehouse.app.presentation.components.EmptyStateView
import com.thehouse.app.presentation.components.ErrorStateView
import com.thehouse.app.presentation.components.LoadingView

@Composable
fun ConversationsDestination(
    factory: ViewModelProvider.Factory,
    onBack: () -> Unit,
    onConversationClick: (String) -> Unit,
) {
    val viewModel: ConversationsViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // Carga al entrar a la pantalla (cada re-entrada tras volver del chat);
    // el ViewModel deduplica si ya está cargando.
    LaunchedEffect(viewModel) {
        viewModel.onEvent(ConversationsEvent.Load)
    }
    ConversationsScreen(
        state = state,
        onBack = onBack,
        onConversationClick = onConversationClick,
        onRetry = { viewModel.onEvent(ConversationsEvent.Load) },
    )
}

@Composable
fun ConversationsScreen(
    state: ConversationsUiState,
    onBack: () -> Unit,
    onConversationClick: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Volver")
            }
            Text(
                "Conversaciones",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { heading() },
            )
            when {
                state.isLoading -> LoadingView(
                    message = "Cargando conversaciones…",
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
                state.errorMessage != null -> ErrorStateView(
                    title = "No se pudieron cargar",
                    message = state.errorMessage,
                    onRetry = onRetry,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
                state.items.isEmpty() -> EmptyStateView(
                    title = "Sin conversaciones",
                    message = "Cuando tengas mensajes con arrendadores aparecerán aquí.",
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
                else -> LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.items, key = { it.id }) { conversation ->
                        ConversationCard(
                            conversation = conversation,
                            onClick = { onConversationClick(conversation.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationCard(
    conversation: Conversation,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 64.dp)
            .padding(vertical = 8.dp)
            .semantics {
                contentDescription = "Conversación sobre ${conversation.room.title} con " +
                    "${conversation.counterpart.name}"
            },
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                conversation.room.title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            if (conversation.unreadCount > 0) {
                Badge(
                    modifier = Modifier.semantics {
                        contentDescription = "${conversation.unreadCount} mensajes sin leer"
                    },
                ) {
                    Text(conversation.unreadCount.toString())
                }
            }
        }
        Text(
            conversation.counterpart.name,
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            if (conversation.status == ConversationStatus.CLOSED) "Cerrada" else "Abierta",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
