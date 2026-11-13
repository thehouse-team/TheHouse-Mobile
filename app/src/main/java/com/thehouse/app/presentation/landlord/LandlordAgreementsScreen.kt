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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.thehouse.app.domain.model.AgreementStatus
import com.thehouse.app.presentation.components.ErrorStateView
import com.thehouse.app.presentation.components.LoadingView

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
    Scaffold(modifier = modifier) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) { Text("Volver") }
            Text("Mis acuerdos", style = MaterialTheme.typography.headlineSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onFilterChanged(null) }) { Text("Todos") }
                OutlinedButton(onClick = { onFilterChanged(AgreementStatus.PENDING) }) { Text("Pendientes") }
                OutlinedButton(onClick = { onFilterChanged(AgreementStatus.ACTIVE) }) { Text("Activos") }
            }
            when {
                state.isLoading -> LoadingView("Cargando acuerdos…", modifier = Modifier.fillMaxWidth())
                state.errorMessage != null -> ErrorStateView("No se pudo cargar", state.errorMessage, onRetry = onRetry)
                state.items.isEmpty() -> Text("No hay acuerdos para mostrar.", style = MaterialTheme.typography.bodyLarge)
                else -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f, fill = false)) {
                        items(state.items, key = { it.id }) { a ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenDetail(a.id) }
                                    .padding(vertical = 8.dp),
                            ) {
                                Text("${a.room.title} · ${a.status.api}", style = MaterialTheme.typography.titleMedium)
                                Text("${a.terms.monthlyRent} ${a.terms.currency} · Inicio ${a.terms.startDate}")
                                a.student?.name?.let { Text("Estudiante: $it") }
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onPrev, enabled = state.page > 1) { Text("Anterior") }
                        Text("Página ${state.page}")
                        OutlinedButton(onClick = onNext, enabled = state.totalPages > 0 && state.page < state.totalPages) { Text("Siguiente") }
                    }
                }
            }
        }
    }
}
