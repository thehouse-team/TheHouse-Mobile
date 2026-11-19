package com.thehouse.app.presentation.visits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thehouse.app.domain.model.VisitStatus
import com.thehouse.app.domain.usecase.VisitValidator
import com.thehouse.app.presentation.components.ErrorStateView
import com.thehouse.app.presentation.components.LoadingView
import com.thehouse.app.presentation.components.StatusChip
import com.thehouse.app.presentation.components.StatusTone
import com.thehouse.app.presentation.components.TheHouseCard
import com.thehouse.app.presentation.components.TheHouseScreen
import com.thehouse.app.presentation.components.TheHouseSecondaryButton
import com.thehouse.app.ui.theme.IconSizes
import com.thehouse.app.ui.theme.Spacing
import java.time.format.DateTimeFormatter

@Composable
fun VisitDetailScreen(
    state: VisitDetailUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TheHouseScreen(
        title = "Detalle de visita",
        onBack = onBack,
        modifier = modifier,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.xl, vertical = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            when {
                state.isLoading -> LoadingView(
                    message = "Cargando visita…",
                    modifier = Modifier.fillMaxWidth(),
                )
                state.visit == null -> ErrorStateView(
                    title = "No se pudo cargar",
                    message = VisitErrorMessages.map(state.errorCode, state.domainError),
                    onRetry = onRetry,
                )
                else -> {
                    val visit = requireNotNull(state.visit)
                    TheHouseCard {
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = visit.property.title,
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                )
                                StatusChip(
                                    label = if (visit.status == VisitStatus.CANCELLED) {
                                        "Cancelada"
                                    } else {
                                        "Solicitada"
                                    },
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
                                    style = MaterialTheme.typography.bodyLarge,
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
                            visit.message?.takeIf { it.isNotBlank() }?.let { message ->
                                Text(
                                    text = "Mensaje",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = message,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (visit.status == VisitStatus.REQUESTED) {
                                TheHouseSecondaryButton(
                                    text = if (state.isCancelling) "Cancelando…" else "Cancelar visita",
                                    onClick = onCancel,
                                    enabled = !state.isCancelling,
                                    loading = state.isCancelling,
                                )
                            }
                        }
                    }
                    if (state.errorCode != null || state.domainError != null) {
                        Text(
                            text = VisitErrorMessages.map(state.errorCode, state.domainError),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
}
