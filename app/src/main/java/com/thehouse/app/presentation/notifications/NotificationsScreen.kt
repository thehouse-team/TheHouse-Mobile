package com.thehouse.app.presentation.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thehouse.app.domain.model.AppNotification
import com.thehouse.app.domain.model.NotificationTarget
import com.thehouse.app.domain.model.NotificationTargetKind
import com.thehouse.app.presentation.U2ViewModelFactory
import com.thehouse.app.presentation.components.EmptyStateView
import com.thehouse.app.presentation.components.ErrorStateView
import com.thehouse.app.presentation.components.LoadingView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun NotificationsDestination(
    factory: U2ViewModelFactory,
    onBack: () -> Unit,
    onNavigateToRequest: (String) -> Unit,
    onNavigateToConversation: (String) -> Unit,
    onNavigateToVisit: (String) -> Unit,
    onNavigateToAgreement: () -> Unit,
    onNavigateToRental: () -> Unit,
    onNavigateToBuilding: (String) -> Unit,
) {
    val viewModel: NotificationsViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                is NotificationsEffect.NavigateToTarget -> handleTarget(
                    target = effect.target,
                    onNavigateToRequest = onNavigateToRequest,
                    onNavigateToConversation = onNavigateToConversation,
                    onNavigateToVisit = onNavigateToVisit,
                    onNavigateToAgreement = onNavigateToAgreement,
                    onNavigateToRental = onNavigateToRental,
                    onNavigateToBuilding = onNavigateToBuilding,
                )
                is NotificationsEffect.TargetUnavailable -> Unit
            }
        }
    }

    LaunchedEffect(Unit) { viewModel.onEvent(NotificationsEvent.Load) }

    // Polling ~30 s solo con app activa/foreground + refresh al volver.
    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        viewModel.onEvent(NotificationsEvent.StartPolling)
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        viewModel.onEvent(NotificationsEvent.StopPolling)
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onEvent(NotificationsEvent.ForegroundRefresh)
    }

    NotificationsScreen(
        state = state,
        onBack = onBack,
        onFilterChanged = { viewModel.onEvent(NotificationsEvent.FilterChanged(it)) },
        onRetry = { viewModel.onEvent(NotificationsEvent.Retry) },
        onOpen = { viewModel.onEvent(NotificationsEvent.OpenNotification(it)) },
        onMarkAllRead = { viewModel.onEvent(NotificationsEvent.MarkAllRead) },
        onLoadMore = { viewModel.onEvent(NotificationsEvent.LoadMore) },
    )
}

private fun handleTarget(
    target: NotificationTarget,
    onNavigateToRequest: (String) -> Unit,
    onNavigateToConversation: (String) -> Unit,
    onNavigateToVisit: (String) -> Unit,
    onNavigateToAgreement: () -> Unit,
    onNavigateToRental: () -> Unit,
    onNavigateToBuilding: (String) -> Unit,
) {
    when (target.kind) {
        NotificationTargetKind.REQUEST -> onNavigateToRequest(target.id)
        NotificationTargetKind.CONVERSATION -> onNavigateToConversation(target.id)
        NotificationTargetKind.VISIT -> onNavigateToVisit(target.id)
        // "Agreement Detail real": sin ruta de detalle de acuerdo por id en cliente estudiante;
        // se dirige a Mi renta (flujo real disponible). Documentado en evidencia NV-14.
        NotificationTargetKind.AGREEMENT -> onNavigateToAgreement()
        NotificationTargetKind.RENTAL -> onNavigateToRental()
        NotificationTargetKind.BUILDING -> onNavigateToBuilding(target.id)
        NotificationTargetKind.UNKNOWN -> Unit
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    state: NotificationsUiState,
    onBack: () -> Unit,
    onFilterChanged: (NotificationFilter) -> Unit,
    onRetry: () -> Unit,
    onOpen: (AppNotification) -> Unit,
    onMarkAllRead: () -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(listState, state.items.size, state.isLoading, state.totalPages, state.page) {
        snapshotFlow {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= (state.items.size - 3).coerceAtLeast(0)
        }
            .distinctUntilChanged()
            .collectLatest { nearEnd ->
                if (nearEnd && !state.isLoading && state.page < state.totalPages) onLoadMore()
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notificaciones") },
                navigationIcon = {
                    TextButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text("Volver")
                    }
                },
                actions = {
                    TextButton(
                        onClick = onMarkAllRead,
                        enabled = !state.markingAll,
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Text(if (state.markingAll) "Marcando..." else "Marcar todo")
                    }
                },
            )
        },
        modifier = modifier,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                NotificationFilter.entries.forEach { filter ->
                    AssistChip(
                        onClick = { onFilterChanged(filter) },
                        label = { Text(filter.label) },
                        modifier = Modifier.heightIn(min = 48.dp),
                        leadingIcon = if (state.filter == filter) {
                            {
                                Box(
                                    Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                )
                            }
                        } else null,
                    )
                }
            }

            when {
                state.isLoading && state.items.isEmpty() -> {
                    LoadingView("Cargando notificaciones", modifier = Modifier.padding(24.dp))
                }
                state.errorMessage != null && state.items.isEmpty() -> {
                    ErrorStateView(
                        title = "No se pudieron cargar las notificaciones",
                        message = state.errorMessage,
                        onRetry = onRetry,
                        modifier = Modifier.padding(24.dp),
                    )
                }
                state.items.isEmpty() -> {
                    EmptyStateView(
                        title = "Sin notificaciones",
                        message = "No tienes notificaciones por ahora.",
                        modifier = Modifier.padding(24.dp),
                    )
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(state.items, key = { it.id }) { notification ->
                            NotificationRow(
                                notification = notification,
                                marking = state.markingId == notification.id,
                                onClick = { onOpen(notification) },
                            )
                        }
                        if (state.isLoading && state.items.isNotEmpty()) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.Center,
                                ) { CircularProgressIndicator(modifier = Modifier.size(24.dp)) }
                            }
                        }
                        if (state.page < state.totalPages && !state.isLoading) {
                            item {
                                OutlinedButton(
                                    onClick = onLoadMore,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                        .heightIn(min = 48.dp),
                                ) { Text("Cargar más") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(
    notification: AppNotification,
    marking: Boolean,
    onClick: () -> Unit,
) {
    val statusText = if (notification.read) "Leída" else "No leída"
    val countText = if (notification.count > 1) ", ${notification.count} mensajes agrupados" else ""
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !marking, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .heightIn(min = 48.dp)
            .semantics {
                contentDescription =
                    "$statusText. ${notification.title}. ${notification.body}$countText"
            },
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(10.dp)
                .clip(CircleShape)
                .background(
                    if (notification.read) MaterialTheme.colorScheme.outlineVariant
                    else MaterialTheme.colorScheme.primary,
                ),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = notification.title.ifBlank { "Notificación" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (notification.read) FontWeight.Normal else FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                if (notification.count > 1) {
                    Text(
                        text = notification.count.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            if (notification.body.isNotBlank()) {
                Text(
                    text = notification.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (notification.read) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface,
                )
            }
            val dateText = notification.createdAt?.let {
                runCatching {
                    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
                        .withZone(ZoneId.systemDefault())
                        .format(it)
                }.getOrNull()
            }
            if (!dateText.isNullOrBlank()) {
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
