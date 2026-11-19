package com.thehouse.app.presentation.visits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thehouse.app.domain.model.Visit
import com.thehouse.app.domain.model.VisitStatus
import com.thehouse.app.domain.usecase.VisitValidator
import com.thehouse.app.presentation.components.EmptyStateView
import com.thehouse.app.presentation.components.ErrorStateView
import com.thehouse.app.presentation.components.LoadingView
import com.thehouse.app.presentation.components.StatusChip
import com.thehouse.app.presentation.components.StatusTone
import com.thehouse.app.presentation.components.TheHouseCard
import com.thehouse.app.presentation.components.TheHouseHeader
import com.thehouse.app.presentation.components.TheHouseSecondaryButton
import com.thehouse.app.ui.theme.IconSizes
import com.thehouse.app.ui.theme.Spacing
import java.time.format.DateTimeFormatter

@Composable
fun VisitsScreen(
    state: VisitsUiState,
    onFilterChanged: (VisitStatus?) -> Unit,
    onCancel: (String) -> Unit,
    onVisitClick: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Spacing.xl, vertical = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            TheHouseHeader(
                title = "Mis visitas",
                subtitle = "Agenda y da seguimiento a tus recorridos.",
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                FilterChip(
                    selected = state.statusFilter == null,
                    onClick = { onFilterChanged(null) },
                    label = { Text("Todas") },
                )
                FilterChip(
                    selected = state.statusFilter == VisitStatus.REQUESTED,
                    onClick = { onFilterChanged(VisitStatus.REQUESTED) },
                    label = { Text("Solicitadas") },
                )
                FilterChip(
                    selected = state.statusFilter == VisitStatus.CANCELLED,
                    onClick = { onFilterChanged(VisitStatus.CANCELLED) },
                    label = { Text("Canceladas") },
                )
            }
            when {
                state.isLoading -> LoadingView(
                    message = "Cargando visitas…",
                    modifier = Modifier.weight(1f),
                )
                state.errorCode != null || state.domainError != null -> ErrorStateView(
                    title = "No se pudieron cargar",
                    message = VisitErrorMessages.map(state.errorCode, state.domainError),
                    modifier = Modifier.weight(1f),
                    onRetry = onRetry,
                )
                state.items.isEmpty() -> EmptyStateView(
                    title = "Sin visitas",
                    message = "No tienes visitas en esta categoría.",
                    modifier = Modifier.weight(1f),
                )
                else -> LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    items(state.items, key = { it.id }) { visit ->
                        VisitCard(
                            visit = visit,
                            cancelling = state.cancellingId == visit.id,
                            onCancel = { onCancel(visit.id) },
                            onClick = { onVisitClick(visit.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VisitCard(
    visit: Visit,
    cancelling: Boolean,
    onCancel: () -> Unit,
    onClick: () -> Unit,
) {
    TheHouseCard(onClick = onClick) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = visit.property.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                StatusChip(
                    label = statusLabel(visit.status),
                    tone = if (visit.status == VisitStatus.CANCELLED) {
                        StatusTone.Neutral
                    } else {
                        StatusTone.Primary
                    },
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(IconSizes.sm),
                )
                Text(
                    text = visit.property.city,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Icon(
                    imageVector = Icons.Outlined.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(IconSizes.sm),
                )
                Text(
                    text = visit.scheduledAt.atZone(VisitValidator.TIME_ZONE)
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            if (visit.status == VisitStatus.REQUESTED) {
                TheHouseSecondaryButton(
                    text = if (cancelling) "Cancelando…" else "Cancelar visita",
                    onClick = onCancel,
                    enabled = !cancelling,
                    loading = cancelling,
                )
            }
        }
    }
}

private fun statusLabel(status: VisitStatus): String = when (status) {
    VisitStatus.REQUESTED -> "Solicitada"
    VisitStatus.CANCELLED -> "Cancelada"
}
