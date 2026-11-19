package com.thehouse.app.presentation.landlord

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
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thehouse.app.presentation.components.InlineMessage
import com.thehouse.app.presentation.components.StatusChip
import com.thehouse.app.presentation.components.StatusTone
import com.thehouse.app.presentation.components.TheHouseCard
import com.thehouse.app.presentation.components.TheHousePrimaryButton
import com.thehouse.app.presentation.components.TheHouseScreen
import com.thehouse.app.presentation.components.TheHouseTextField
import com.thehouse.app.ui.theme.Spacing

@Composable
fun CreateAgreementDestination(
    factory: ViewModelProvider.Factory,
    onBack: () -> Unit,
    onDone: () -> Unit,
) {
    val viewModel: CreateAgreementViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CreateAgreementScreen(
        state = state,
        onBack = onBack,
        onMonthlyRentChanged = { viewModel.onEvent(CreateAgreementEvent.MonthlyRentChanged(it)) },
        onDepositChanged = { viewModel.onEvent(CreateAgreementEvent.DepositChanged(it)) },
        onStartDateChanged = { viewModel.onEvent(CreateAgreementEvent.StartDateChanged(it)) },
        onDurationChanged = { viewModel.onEvent(CreateAgreementEvent.DurationMonthsChanged(it)) },
        onPaymentDayChanged = { viewModel.onEvent(CreateAgreementEvent.PaymentDayChanged(it)) },
        onSubmit = { viewModel.onEvent(CreateAgreementEvent.Submit) },
        onDone = onDone,
    )
}

@Composable
fun CreateAgreementScreen(
    state: CreateAgreementUiState,
    onBack: () -> Unit,
    onMonthlyRentChanged: (String) -> Unit,
    onDepositChanged: (String) -> Unit,
    onStartDateChanged: (String) -> Unit,
    onDurationChanged: (String) -> Unit,
    onPaymentDayChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TheHouseScreen(
        title = "Crear acuerdo",
        subtitle = "Define las condiciones de renta",
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
            val created = state.created
            if (created != null) {
                TheHouseCard {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        Text(
                            text = "Acuerdo creado correctamente",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        StatusChip(
                            label = agreementStatusLabel(created.status),
                            tone = agreementStatusTone(created.status),
                        )
                    }
                }

                created.closingCode?.let { code ->
                    TheHouseCard(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                            Text(
                                text = "Código de respaldo",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            Text(
                                text = code.backupCode,
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            Text(
                                text = "Expira: ${code.expiresAt}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            Text(
                                text = "Comparte este código con el estudiante. " +
                                    "No se volverá a mostrar en el detalle.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                }

                TheHousePrimaryButton(
                    text = "Continuar",
                    onClick = onDone,
                )
            } else {
                Text(
                    text = "Ingresa las condiciones que acordaste con el estudiante.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                TheHouseTextField(
                    value = state.monthlyRent,
                    onValueChange = onMonthlyRentChanged,
                    label = "Renta mensual (MXN)",
                )
                TheHouseTextField(
                    value = state.deposit,
                    onValueChange = onDepositChanged,
                    label = "Depósito (MXN)",
                )
                TheHouseTextField(
                    value = state.startDate,
                    onValueChange = onStartDateChanged,
                    label = "Fecha inicio (YYYY-MM-DD)",
                )
                TheHouseTextField(
                    value = state.durationMonths,
                    onValueChange = onDurationChanged,
                    label = "Duración (meses)",
                )
                TheHouseTextField(
                    value = state.paymentDay,
                    onValueChange = onPaymentDayChanged,
                    label = "Día de pago (1-28)",
                )

                state.errorMessage?.let { message ->
                    InlineMessage(text = message)
                }

                TheHousePrimaryButton(
                    text = "Crear acuerdo",
                    onClick = onSubmit,
                    enabled = !state.isSubmitting,
                    loading = state.isSubmitting,
                )
            }
        }
    }
}
