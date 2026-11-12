package com.thehouse.app.presentation.visits

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thehouse.app.domain.model.Visit
import com.thehouse.app.domain.model.VisitStatus
import com.thehouse.app.domain.usecase.VisitValidator
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
    Scaffold(modifier = modifier) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Mis visitas", style = MaterialTheme.typography.headlineMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onFilterChanged(VisitStatus.REQUESTED) }) {
                    Text("Solicitadas")
                }
                OutlinedButton(onClick = { onFilterChanged(VisitStatus.CANCELLED) }) {
                    Text("Canceladas")
                }
            }
            when {
                state.isLoading -> CircularProgressIndicator()
                state.errorCode != null || state.domainError != null -> {
                    Text(VisitErrorMessages.map(state.errorCode, state.domainError))
                    Button(onClick = onRetry) { Text("Reintentar") }
                }
                state.items.isEmpty() -> Text("No tienes visitas en esta categoría.")
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.items, key = { it.id }) { visit ->
                        VisitRow(
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
private fun VisitRow(
    visit: Visit,
    cancelling: Boolean,
    onCancel: () -> Unit,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(visit.property.title, style = MaterialTheme.typography.titleMedium)
        Text(visit.property.city, style = MaterialTheme.typography.bodySmall)
        Text(
            visit.scheduledAt.atZone(VisitValidator.TIME_ZONE)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(visit.status.name.lowercase(), style = MaterialTheme.typography.bodySmall)
        if (visit.status == VisitStatus.REQUESTED) {
            OutlinedButton(onClick = onCancel, enabled = !cancelling) {
                Text(if (cancelling) "Cancelando..." else "Cancelar")
            }
        }
    }
}