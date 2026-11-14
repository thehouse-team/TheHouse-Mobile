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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thehouse.app.domain.model.AgreementStatus

@Composable
fun LandlordAgreementDetailDestination(
    factory: ViewModelProvider.Factory,
    onBack: () -> Unit,
) {
    val viewModel: LandlordAgreementDetailViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LandlordAgreementDetailScreen(
        state = state,
        onBack = onBack,
        onRetry = { viewModel.onEvent(LandlordAgreementDetailEvent.Load) },
        onIssueCode = { viewModel.onEvent(LandlordAgreementDetailEvent.IssueCode) },
        onCancel = { viewModel.onEvent(LandlordAgreementDetailEvent.Cancel) },
        onEnd = { reason, note -> viewModel.onEvent(LandlordAgreementDetailEvent.End(reason, note)) },
    )
}

@Composable
fun LandlordAgreementDetailScreen(
    state: LandlordAgreementDetailUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onIssueCode: () -> Unit,
    onCancel: () -> Unit,
    onEnd: (String?, String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var endReason by remember { mutableStateOf<String?>(null) }
    var endNote by remember { mutableStateOf("") }
    var showEnd by remember { mutableStateOf(false) }

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
            when {
                state.isLoading -> com.thehouse.app.presentation.components.LoadingView("Cargando…", modifier = Modifier.fillMaxWidth())
                state.errorMessage != null && state.agreement == null -> com.thehouse.app.presentation.components.ErrorStateView("No se pudo cargar", state.errorMessage, onRetry = onRetry)
                state.agreement != null -> {
                    val a = state.agreement
                    Text("Detalle de acuerdo", style = MaterialTheme.typography.headlineSmall)
                    Text("${a.room.title} · ${a.room.city}")
                    Text("Estado: ${a.status.api}")
                    a.student?.name?.let { Text("Estudiante: $it") }
                    Text("Renta: ${a.terms.monthlyRent} ${a.terms.currency} · Depósito: ${a.terms.deposit}")
                    Text("Inicio: ${a.terms.startDate} · Fin: ${a.terms.endDate} · Día pago: ${a.terms.paymentDay}")
                    Text("Creado: ${a.createdAt ?: "-"}")
                    a.closingCode?.let { code ->
                        Text("Código de respaldo: ${code.backupCode}", style = MaterialTheme.typography.titleMedium)
                        Text("Expira: ${code.expiresAt}")
                    } ?: Text("Código: no disponible en el detalle. Genera uno nuevo si es necesario.")
                    state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    if (a.status == AgreementStatus.PENDING) {
                        Button(onClick = onIssueCode, enabled = !state.isBusy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                            if (state.isBusy) CircularProgressIndicator(modifier = Modifier.heightIn(max = 18.dp)) else Text("Generar nuevo código")
                        }
                        OutlinedButton(onClick = onCancel, enabled = !state.isBusy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Cancelar acuerdo") }
                    }
                    if (a.status == AgreementStatus.ACTIVE) {
                        if (!showEnd) {
                            OutlinedButton(onClick = { showEnd = true }, enabled = !state.isBusy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Finalizar acuerdo") }
                        } else {
                            Text("Motivo (opcional)")
                            RowButtons(listOf("contract_finished", "early_termination", "other"), endReason) { endReason = it }
                            OutlinedTextField(endNote, { endNote = it.take(300) }, label = { Text("Nota (opcional, máx 300)") }, modifier = Modifier.fillMaxWidth())
                            Button(onClick = { onEnd(endReason, endNote); showEnd = false }, enabled = !state.isBusy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Confirmar finalización") }
                            TextButton(onClick = { showEnd = false }) { Text("Cancelar") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RowButtons(options: List<String>, selected: String?, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEach { opt ->
            OutlinedButton(onClick = { onSelect(opt) }, modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp)) {
                Text((if (selected == opt) "● " else "○ ") + opt)
            }
        }
    }
}
