package com.thehouse.app.presentation.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.thehouse.app.domain.push.NotificationDestination
import com.thehouse.app.domain.push.NotificationDestinationResolver
import com.thehouse.app.presentation.U2ViewModelFactory
import com.thehouse.app.presentation.components.EmptyStateView
import com.thehouse.app.presentation.components.ErrorStateView
import com.thehouse.app.presentation.components.LoadingView
import com.thehouse.app.presentation.components.TheHouseCard
import com.thehouse.app.presentation.components.TheHouseScreen
import com.thehouse.app.presentation.components.TheHouseSecondaryButton
import com.thehouse.app.presentation.components.TheHouseTextAction
import com.thehouse.app.ui.theme.Spacing
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged

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

    val permission = rememberNotificationPermissionUi()

    NotificationsScreen(
        state = state,
        onBack = onBack,
        onFilterChanged = { viewModel.onEvent(NotificationsEvent.FilterChanged(it)) },
        onRetry = { viewModel.onEvent(NotificationsEvent.Retry) },
        onOpen = { viewModel.onEvent(NotificationsEvent.OpenNotification(it)) },
        onMarkAllRead = { viewModel.onEvent(NotificationsEvent.MarkAllRead) },
        onLoadMore = { viewModel.onEvent(NotificationsEvent.LoadMore) },
        permissionStatus = permission.status,
        onEnableNotifications = permission.onEnable,
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
    when (val destination = NotificationDestinationResolver.fromTarget(target)) {
        is NotificationDestination.Request -> onNavigateToRequest(destination.id)
        is NotificationDestination.Conversation -> onNavigateToConversation(destination.id)
        is NotificationDestination.Visit -> onNavigateToVisit(destination.id)
        // "Agreement Detail real": sin ruta de detalle de acuerdo por id en cliente estudiante;
        // se dirige a Mi renta (flujo real disponible). Documentado en evidencia NV-14.
        NotificationDestination.Agreement -> onNavigateToAgreement()
        NotificationDestination.Rental -> onNavigateToRental()
        is NotificationDestination.Building -> onNavigateToBuilding(destination.id)
        NotificationDestination.NotificationCenter -> Unit
    }
}

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
    permissionStatus: NotificationPermissionStatus = NotificationPermissionStatus.GRANTED,
    onEnableNotifications: () -> Unit = {},
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

    TheHouseScreen(
        title = "Notificaciones",
        subtitle = state.unreadCount?.let { count ->
            if (count.unread == 1) "1 sin leer" else "${count.unread} sin leer"
        },
        onBack = onBack,
        actions = {
            TheHouseTextAction(
                text = if (state.markingAll) "Marcando…" else "Marcar todo",
                onClick = onMarkAllRead,
                enabled = !state.markingAll,
            )
        },
        modifier = modifier,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            NotificationPermissionCard(
                status = permissionStatus,
                onEnable = onEnableNotifications,
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                contentPadding = PaddingValues(
                    horizontal = Spacing.xl,
                    vertical = Spacing.sm,
                ),
            ) {
                items(NotificationFilter.entries) { filter ->
                    FilterChip(
                        selected = state.filter == filter,
                        onClick = { onFilterChanged(filter) },
                        label = { Text(filter.label) },
                    )
                }
            }

            when {
                state.isLoading && state.items.isEmpty() -> {
                    LoadingView(
                        message = "Cargando notificaciones…",
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    )
                }
                state.errorMessage != null && state.items.isEmpty() -> {
                    ErrorStateView(
                        title = "No se pudieron cargar las notificaciones",
                        message = state.errorMessage,
                        onRetry = onRetry,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    )
                }
                state.items.isEmpty() -> {
                    EmptyStateView(
                        title = "Sin notificaciones",
                        message = "No tienes notificaciones por ahora.",
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    )
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                        contentPadding = PaddingValues(
                            start = Spacing.xl,
                            end = Spacing.xl,
                            top = Spacing.xs,
                            bottom = Spacing.lg,
                        ),
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
                                        .padding(Spacing.lg),
                                    horizontalArrangement = Arrangement.Center,
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                        if (state.page < state.totalPages && !state.isLoading) {
                            item {
                                TheHouseSecondaryButton(
                                    text = "Cargar más",
                                    onClick = onLoadMore,
                                )
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
    val countText = if (notification.count > 1) {
        ", ${notification.count} mensajes agrupados"
    } else {
        ""
    }
    TheHouseCard(
        modifier = Modifier.semantics {
            contentDescription = "$statusText. ${notification.title}. " +
                "${notification.body}$countText"
        },
        onClick = { if (!marking) onClick() },
        contentPadding = PaddingValues(Spacing.md),
        containerColor = if (notification.read) {
            MaterialTheme.colorScheme.surfaceContainerLowest
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(
                        if (notification.read) {
                            MaterialTheme.colorScheme.outlineVariant
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    ),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = notification.title.ifBlank { "Notificación" },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (notification.read) {
                            FontWeight.Normal
                        } else {
                            FontWeight.SemiBold
                        },
                        color = MaterialTheme.colorScheme.onSurface,
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
                        color = if (notification.read) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
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
}

/**
 * Tarjeta contextual de permiso. No se muestra en Android < 13 ni con permiso
 * concedido. El texto visible actúa como rationale antes del diálogo del sistema.
 */
@Composable
private fun NotificationPermissionCard(
    status: NotificationPermissionStatus,
    onEnable: () -> Unit,
) {
    if (status == NotificationPermissionStatus.GRANTED) return

    val permanentlyDenied = status == NotificationPermissionStatus.PERMANENTLY_DENIED
    val title = if (permanentlyDenied) {
        "Notificaciones desactivadas"
    } else {
        "Activa las notificaciones"
    }
    val message = if (permanentlyDenied) {
        "Puedes habilitarlas desde los ajustes del sistema para recibir avisos de " +
            "solicitudes, visitas y novedades."
    } else {
        "Recibe avisos de solicitudes, visitas y novedades. Puedes seguir usando la app " +
            "sin activarlas."
    }
    val action = if (permanentlyDenied) "Abrir ajustes" else "Activar"

    TheHouseCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.xl, vertical = Spacing.sm),
        containerColor = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            TheHouseTextAction(
                text = action,
                onClick = onEnable,
            )
        }
    }
}
