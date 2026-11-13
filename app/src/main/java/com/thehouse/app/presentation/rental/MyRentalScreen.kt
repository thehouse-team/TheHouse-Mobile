package com.thehouse.app.presentation.rental

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import com.thehouse.app.presentation.components.ErrorStateView
import com.thehouse.app.presentation.components.LoadingView

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
            Text("Mi renta", style = MaterialTheme.typography.headlineSmall)
            when {
                state.isLoading -> LoadingView("Cargando renta…", modifier = Modifier.fillMaxWidth())
                state.emptyMessage != null -> Text(state.emptyMessage, style = MaterialTheme.typography.bodyLarge)
                state.errorMessage != null -> ErrorStateView(
                    title = "No se pudo cargar",
                    message = state.errorMessage,
                    onRetry = onRetry,
                )
                state.rental != null -> {
                    val r = state.rental
                    Text("${r.room.title} · ${r.room.city}", style = MaterialTheme.typography.titleMedium)
                    Text("Edificio: ${r.building.name}")
                    Text("Arrendador: ${r.landlord.name}")
                    Text("Renta: ${r.terms.monthlyRent} ${r.terms.currency} · Depósito: ${r.terms.deposit} ${r.terms.currency}")
                    Text("Inicio: ${r.terms.startDate} · Fin: ${r.terms.endDate} · Día de pago: ${r.terms.paymentDay}")
                    r.activatedAt?.let { Text("Activado: $it") }
                    Text(
                        "Dirección: ${r.address.street} ${r.address.number}, ${r.address.neighborhood}, ${r.address.city}, ${r.address.state} ${r.address.postalCode}",
                    )
                    r.nextPayment?.let { np ->
                        Text(
                            "Próximo pago (informativo): ${np.dueDate} · ${np.amount} ${r.terms.currency}",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            "Pago no disponible desde la aplicación.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Button(
                        onClick = { onOpenConversation(r.conversationId) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) {
                        Text("Hablar con arrendador")
                    }
                }
            }
        }
    }
}
