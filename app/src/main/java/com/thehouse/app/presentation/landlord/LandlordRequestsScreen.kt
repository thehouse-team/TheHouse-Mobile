package com.thehouse.app.presentation.landlord

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thehouse.app.domain.model.RequestStatus
import com.thehouse.app.domain.model.RoomRequest
import com.thehouse.app.presentation.components.EmptyStateView
import com.thehouse.app.presentation.components.ErrorStateView
import com.thehouse.app.presentation.components.LoadingView
import com.thehouse.app.presentation.requests.RequestErrorMessages

private val filterOptions: List<Pair<RequestStatus?, String>> = listOf(
    null to "Todas",
    RequestStatus.PENDING to "Pendientes",
    RequestStatus.ACCEPTED to "Aceptadas",
    RequestStatus.REJECTED to "Rechazadas",
    RequestStatus.CANCELLED to "Canceladas",
)

@Composable
fun LandlordRequestsDestination(
    factory: ViewModelProvider.Factory,
    onBack: () -> Unit,
    onRequestClick: (String) -> Unit,
) {
    val viewModel: LandlordRequestsViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            if (effect is LandlordRequestsEffect.ShowMessage) Unit
        }
    }
    LandlordRequestsScreen(
        state = state,
        onBack = onBack,
        onFilterChanged = { viewModel.onEvent(LandlordRequestsEvent.FilterChanged(it)) },
        onAccept = { viewModel.onEvent(LandlordRequestsEvent.Accept(it)) },
        onRequestClick = onRequestClick,
        onRetry = { viewModel.onEvent(LandlordRequestsEvent.Load) },
    )
}

@Composable
fun LandlordRequestsScreen(
    state: LandlordRequestsUiState,
    onBack: () -> Unit,
    onFilterChanged: (RequestStatus?) -> Unit,
    onAccept: (String) -> Unit,
    onRequestClick: (String) -> Unit,
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
                "Solicitudes recibidas",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { heading() },
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                filterOptions.forEach { (status, label) ->
                    TextButton(
                        onClick = { onFilterChanged(status) },
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (state.statusFilter == status) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }
            }

            when {
                state.isLoading -> LoadingView(
                    message = "Cargando solicitudes…",
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
                state.errorMessage != null -> ErrorStateView(
                    title = "No se pudieron cargar",
                    message = state.errorMessage,
                    onRetry = onRetry,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
                state.items.isEmpty() -> EmptyStateView(
                    title = "Sin solicitudes",
                    message = "Aún no recibes solicitudes para tus cuartos.",
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
                else -> LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.items, key = { it.id }) { request ->
                        LandlordRequestCard(
                            request = request,
                            isBusy = state.busyId == request.id,
                            onClick = { onRequestClick(request.id) },
                            onAccept = { onAccept(request.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LandlordRequestCard(
    request: RoomRequest,
    isBusy: Boolean,
    onClick: () -> Unit,
    onAccept: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 64.dp)
            .padding(vertical = 8.dp)
            .semantics {
                contentDescription = "Solicitud de ${request.student?.name ?: "estudiante"} " +
                    "para ${request.room.title}"
            },
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            request.student?.name ?: "Estudiante",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            "${request.room.title} · ${RequestErrorMessages.statusLabel(request.status)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (request.status == RequestStatus.PENDING) {
            Button(
                onClick = onAccept,
                enabled = !isBusy,
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                if (isBusy) {
                    CircularProgressIndicator(modifier = Modifier.heightIn(max = 18.dp))
                } else {
                    Text("Aceptar")
                }
            }
        }
    }
}
