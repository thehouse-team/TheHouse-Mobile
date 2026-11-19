package com.thehouse.app.presentation.requests

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thehouse.app.domain.model.RequestStatus
import com.thehouse.app.domain.model.RoomRequest
import com.thehouse.app.presentation.components.EmptyStateView
import com.thehouse.app.presentation.components.ErrorStateView
import com.thehouse.app.presentation.components.LoadingView
import com.thehouse.app.presentation.components.StatusChip
import com.thehouse.app.presentation.components.StatusTone
import com.thehouse.app.presentation.components.TheHouseCard
import com.thehouse.app.presentation.components.TheHouseScreen
import com.thehouse.app.presentation.components.TheHouseSecondaryButton
import com.thehouse.app.ui.theme.Spacing

private val filterOptions: List<Pair<RequestStatus?, String>> = listOf(
    null to "Todas",
    RequestStatus.PENDING to "Pendientes",
    RequestStatus.ACCEPTED to "Aceptadas",
    RequestStatus.REJECTED to "Rechazadas",
    RequestStatus.CANCELLED to "Canceladas",
)

@Composable
fun RequestsDestination(
    factory: ViewModelProvider.Factory,
    onBack: () -> Unit,
    onRequestClick: (String) -> Unit,
) {
    val viewModel: RequestsViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            if (effect is RequestsEffect.ShowMessage) Unit
        }
    }
    RequestsScreen(
        state = state,
        onBack = onBack,
        onFilterChanged = { viewModel.onEvent(RequestsEvent.FilterChanged(it)) },
        onCancel = { viewModel.onEvent(RequestsEvent.Cancel(it)) },
        onRequestClick = onRequestClick,
        onRetry = { viewModel.onEvent(RequestsEvent.Load) },
    )
}

@Composable
fun RequestsScreen(
    state: RequestsUiState,
    onBack: () -> Unit,
    onFilterChanged: (RequestStatus?) -> Unit,
    onCancel: (String) -> Unit,
    onRequestClick: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TheHouseScreen(
        title = "Mis solicitudes",
        onBack = onBack,
        modifier = modifier,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Spacing.xl, vertical = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = Spacing.xs),
            ) {
                items(filterOptions) { (status, label) ->
                    FilterChip(
                        selected = state.statusFilter == status,
                        onClick = { onFilterChanged(status) },
                        label = { Text(label) },
                    )
                }
            }

            when {
                state.isLoading -> LoadingView(
                    message = "Cargando solicitudes…",
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
                state.errorMessage != null -> ErrorStateView(
                    title = "No se pudieron cargar",
                    message = state.errorMessage,
                    onRetry = onRetry,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
                state.items.isEmpty() -> EmptyStateView(
                    title = "Sin solicitudes",
                    message = "Aún no has enviado solicitudes.",
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
                else -> LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    items(state.items, key = { it.id }) { request ->
                        RequestCard(
                            request = request,
                            isCancelling = state.cancellingId == request.id,
                            onClick = { onRequestClick(request.id) },
                            onCancel = { onCancel(request.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RequestCard(
    request: RoomRequest,
    isCancelling: Boolean,
    onClick: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TheHouseCard(
        modifier = modifier.semantics {
            contentDescription =
                "Solicitud ${request.room.title} ${RequestErrorMessages.statusLabel(request.status)}"
        },
        onClick = onClick,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Text(
                    text = request.room.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
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
            if (request.status == RequestStatus.PENDING) {
                TheHouseSecondaryButton(
                    text = if (isCancelling) "Cancelando…" else "Cancelar solicitud",
                    onClick = onCancel,
                    enabled = !isCancelling,
                    loading = isCancelling,
                )
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
