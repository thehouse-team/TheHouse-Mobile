package com.thehouse.app.presentation.rental

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thehouse.app.presentation.components.EmptyStateView
import com.thehouse.app.presentation.components.ErrorStateView
import com.thehouse.app.presentation.components.InlineMessage
import com.thehouse.app.presentation.components.InfoRow
import com.thehouse.app.presentation.components.LoadingView
import com.thehouse.app.presentation.components.StatusChip
import com.thehouse.app.presentation.components.StatusTone
import com.thehouse.app.presentation.components.TheHouseCard
import com.thehouse.app.presentation.components.TheHousePrimaryButton
import com.thehouse.app.presentation.components.TheHouseScreen
import com.thehouse.app.ui.theme.Spacing

@Composable
fun MyRentalDestination(
    factory: ViewModelProvider.Factory,
    onBack: () -> Unit,
    onRequireLogin: () -> Unit,
    onOpenConversation: (String) -> Unit,
) {
    val viewModel: MyRentalViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    MyRentalScreen(
        state = state,
        onBack = onBack,
        onRetry = { viewModel.onEvent(MyRentalEvent.Load) },
        onOpenConversation = onOpenConversation,
    )
}

@Composable
fun MyRentalScreen(
    state: MyRentalUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onOpenConversation: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    TheHouseScreen(
        title = "Mi renta",
        subtitle = "Tu contrato activo",
        onBack = onBack,
        modifier = modifier,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Spacing.xl, vertical = Spacing.lg)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            when {
                state.isLoading -> LoadingView(
                    message = "Cargando renta…",
                    modifier = Modifier.fillMaxWidth(),
                )
                state.emptyMessage != null -> EmptyStateView(
                    title = "Sin renta activa",
                    message = state.emptyMessage,
                    modifier = Modifier.fillMaxWidth(),
                )
                state.errorMessage != null -> ErrorStateView(
                    title = "No se pudo cargar",
                    message = state.errorMessage,
                    onRetry = onRetry,
                )
                state.rental != null -> {
                    val rental = state.rental ?: return@Column
                    TheHouseCard {
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                            StatusChip(label = "Activo", tone = StatusTone.Success)
                            Text(
                                text = rental.room.title,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = rental.room.city,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    TheHouseCard {
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            Text(
                                text = "Condiciones",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            InfoRow("Edificio", rental.building.name)
                            InfoRow("Arrendador", rental.landlord.name)
                            InfoRow(
                                "Renta",
                                "${rental.terms.monthlyRent} ${rental.terms.currency}",
                            )
                            InfoRow(
                                "Depósito",
                                "${rental.terms.deposit} ${rental.terms.currency}",
                            )
                            InfoRow("Inicio", rental.terms.startDate)
                            InfoRow("Fin", rental.terms.endDate)
                            InfoRow("Día de pago", rental.terms.paymentDay.toString())
                            rental.activatedAt?.let { activatedAt ->
                                InfoRow("Activado", activatedAt)
                            }
                        }
                    }

                    TheHouseCard {
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                            Text(
                                text = "Dirección",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "${rental.address.street} ${rental.address.number}, " +
                                    "${rental.address.neighborhood}, ${rental.address.city}, " +
                                    "${rental.address.state} ${rental.address.postalCode}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    rental.nextPayment?.let { nextPayment ->
                        TheHouseCard {
                            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                                Text(
                                    text = "Próximo pago (informativo)",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                InfoRow("Fecha", nextPayment.dueDate)
                                InfoRow(
                                    "Monto",
                                    "${nextPayment.amount} ${rental.terms.currency}",
                                )
                                InlineMessage(
                                    text = "Pago no disponible desde la aplicación.",
                                    tone = StatusTone.Neutral,
                                )
                            }
                        }
                    }

                    TheHousePrimaryButton(
                        text = "Hablar con arrendador",
                        onClick = { onOpenConversation(rental.conversationId) },
                    )
                }
            }
        }
    }
}
