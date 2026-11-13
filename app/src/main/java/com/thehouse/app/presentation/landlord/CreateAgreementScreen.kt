package com.thehouse.app.presentation.landlord

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

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
    Scaffold(modifier = modifier) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) { Text("Volver") }
            Text("Crear acuerdo", style = MaterialTheme.typography.headlineSmall)
            val created = state.created
            if (created != null) {
                Text("Acuerdo creado correctamente.", style = MaterialTheme.typography.titleMedium)
                Text("Estado: ${created.status.api}")
                created.closingCode?.let { code ->
                    Text("Código de respaldo: ${code.backupCode}", style = MaterialTheme.typography.titleMedium)
                    Text("Expira: ${code.expiresAt}")
                    Text("Comparte este código con el estudiante. No se volverá a mostrar en el detalle.")
                }
                Button(onClick = onDone, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text("Continuar")
                }
            } else {
                OutlinedTextField(state.monthlyRent, onMonthlyRentChanged, label = { Text("Renta mensual (MXN)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(state.deposit, onDepositChanged, label = { Text("Depósito (MXN)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(state.startDate, onStartDateChanged, label = { Text("Fecha inicio (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(state.durationMonths, onDurationChanged, label = { Text("Duración (meses)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(state.paymentDay, onPaymentDayChanged, label = { Text("Día de pago (1-28)") }, modifier = Modifier.fillMaxWidth())
                state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Button(
                    onClick = onSubmit,
                    enabled = !state.isSubmitting,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) {
                    if (state.isSubmitting) CircularProgressIndicator(modifier = Modifier.heightIn(max = 18.dp))
                    else Text("Crear acuerdo")
                }
            }
        }
    }
}
