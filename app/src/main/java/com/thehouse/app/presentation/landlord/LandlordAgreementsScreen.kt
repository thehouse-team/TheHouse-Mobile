package com.thehouse.app.presentation.landlord

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thehouse.app.domain.model.AgreementStatus
import com.thehouse.app.presentation.components.EmptyStateView
import com.thehouse.app.presentation.components.ErrorStateView
import com.thehouse.app.presentation.components.LoadingView
import com.thehouse.app.presentation.components.StatusChip
import com.thehouse.app.presentation.components.TheHouseCard
import com.thehouse.app.presentation.components.TheHouseScreen
import com.thehouse.app.presentation.components.TheHouseSecondaryButton
import com.thehouse.app.ui.theme.Spacing

private val filterOptions: List<Pair<AgreementStatus?, String>> = listOf(
    null to "Todos",
    AgreementStatus.PENDING to "Pendientes",
    AgreementStatus.ACTIVE to "Activos",
)

@Composable
fun LandlordAgreementsDestination(
    factory: ViewModelProvider.Factory,
    onBack: () -> Unit,
    onOpenDetail: (String) -> Unit,
) {
    val viewModel: LandlordAgreementsViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LandlordAgreementsScreen(
        state = state,
        onBack = onBack,
        onRetry = { viewModel.onEvent(LandlordAgreementsEvent.Retry) },
        onFilterChanged = { viewModel.onEvent(LandlordAgreementsEvent.FilterChanged(it)) },
        onOpenDetail = onOpenDetail,
        onPrev = { viewModel.onEvent(LandlordAgreementsEvent.PrevPage) },
        onNext = { viewModel.onEvent(LandlordAgreementsEvent.NextPage) },
    )
}

@Composable
fun LandlordAgreementsScreen(
    state: LandlordAgreementsUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onFilterChanged: (AgreementStatus?) -> Unit,
    onOpenDetail: (String) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TheHouseScreen(
        title = "Mis acuerdos",
        subtitle = "Acuerdos de renta con estudiantes",
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
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                filterOptions.forEach { (status, label) ->
                    FilterChip(
                        selected = state.selectedStatus == status,
                        onClick = { onFilterChanged(status) },
                        label = { Text(label) },
                    )
                }
            }

            when {
                state.isLoading -> LoadingView(
                    message = "Cargando acuerdos…",
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
                state.errorMessage != null -> ErrorStateView(
                    title = "No se pudo cargar",
                    message = state.errorMessage,
                    onRetry = onRetry,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
                state.items.isEmpty() -> EmptyStateView(
                    title = "Sin acuerdos",
                    message = "No hay acuerdos para mostrar.",
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
                else -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(Spacing.md),
                        contentPadding = PaddingValues(bottom = Spacing.md),
                    ) {
                        items(state.items, key = { it.id }) { agreement ->
                            TheHouseCard(
                                onClick = { onOpenDetail(agreement.id) },
                                contentPadding = PaddingValues(Spacing.md),
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = agreement.room.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f),
                                        )
                                        StatusChip(
                                            label = agreementStatusLabel(agreement.status),
                                            tone = agreementStatusTone(agreement.status),
                                        )
                                    }
                                    Text(
                                        text = "${agreement.terms.monthlyRent} " +
                                            "${agreement.terms.currency} · Inicio " +
                                            agreement.terms.startDate,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    agreement.student?.name?.let { studentName ->
                                        Text(
                                            text = "Estudiante: $studentName",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TheHouseSecondaryButton(
                            text = "Anterior",
                            onClick = onPrev,
                            enabled = state.page > 1,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "Página ${state.page}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        TheHouseSecondaryButton(
                            text = "Siguiente",
                            onClick = onNext,
                            enabled = state.totalPages > 0 && state.page < state.totalPages,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}
