package com.thehouse.app.presentation.rental

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thehouse.app.presentation.components.ErrorStateView
import com.thehouse.app.presentation.components.LoadingView
import com.thehouse.app.presentation.landlord.AgreementErrorMessages

@Composable
fun AgreementPreviewDestination(
    factory: ViewModelProvider.Factory,
    onBack: () -> Unit,
    onNavigateToMyRental: () -> Unit,
) {
    val viewModel: AgreementPreviewViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                AgreementPreviewEffect.NavigateToMyRental -> onNavigateToMyRental()
            }
        }
    }
    AgreementPreviewScreen(
        state = state,
        onBack = onBack,
        onRetry = { viewModel.onEvent(AgreementPreviewEvent.Load) },
        onAcceptedTermsChanged = { viewModel.onEvent(AgreementPreviewEvent.AcceptedTermsChanged(it)) },
        onAccept = { viewModel.onEvent(AgreementPreviewEvent.Accept) },
    )
}

@Composable
fun AgreementPreviewScreen(
    state: AgreementPreviewUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onAcceptedTermsChanged: (Boolean) -> Unit,
    onAccept: () -> Unit,
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
            Text("Revisar acuerdo", style = MaterialTheme.typography.headlineSmall)
            when {
                state.isLoading -> LoadingView("Cargando acuerdo…", modifier = Modifier.fillMaxWidth())
                state.errorMessage != null && state.preview == null -> ErrorStateView(
                    title = "No se pudo cargar",
                    message = state.errorMessage,
                    onRetry = onRetry,
                )
                state.preview != null -> {
                    val p = state.preview
                    Text("${p.room.title} · ${p.room.city}", style = MaterialTheme.typography.titleMedium)
                    Text("Edificio: ${p.building.name}", style = MaterialTheme.typography.bodyLarge)
                    p.building.rules?.takeIf { it.isNotBlank() }?.let { Text("Reglas: $it") }
                    Text("Arrendador: ${p.landlord.name}", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Renta mensual: ${p.terms.monthlyRent} ${p.terms.currency} · Depósito: ${p.terms.deposit} ${p.terms.currency}",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        "Inicio: ${p.terms.startDate} · Duración: ${p.terms.durationMonths} meses · Fin: ${p.terms.endDate}",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text("Día de pago: ${p.terms.paymentDay}", style = MaterialTheme.typography.bodyLarge)
                    Text("Código expira: ${p.codeExpiresAt}", style = MaterialTheme.typography.bodyMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = state.acceptedTerms, onCheckedChange = onAcceptedTermsChanged)
                        Text("Acepto los términos del acuerdo")
                    }
                    state.retryAfterSeconds?.let { sec ->
                        Text(AgreementErrorMessages.retryHint(sec) ?: "Espera e intenta de nuevo.")
                    }
                    state.errorMessage?.let { msg ->
                        Text(msg, color = MaterialTheme.colorScheme.error)
                    }
                    Button(
                        onClick = onAccept,
                        enabled = state.acceptedTerms && !state.isAccepting && state.retryAfterSeconds == null,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) {
                        if (state.isAccepting) CircularProgressIndicator(modifier = Modifier.heightIn(max = 18.dp))
                        else Text("Aceptar acuerdo")
                    }
                }
            }
        }
    }
}
