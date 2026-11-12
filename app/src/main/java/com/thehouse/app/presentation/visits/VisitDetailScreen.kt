package com.thehouse.app.presentation.visits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thehouse.app.domain.model.VisitStatus
import com.thehouse.app.domain.usecase.VisitValidator
import java.time.format.DateTimeFormatter

@Composable
fun VisitDetailScreen(
    state: VisitDetailUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TextButton(onClick = onBack, modifier = Modifier.padding(horizontal = 16.dp)) {
                Text("Volver")
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when {
                state.isLoading -> CircularProgressIndicator()
                state.visit == null -> {
                    Text(VisitErrorMessages.map(state.errorCode, state.domainError))
                    Button(onClick = onRetry) { Text("Reintentar") }
                }
                else -> {
                    val visit = requireNotNull(state.visit)
                    Text("Detalle de visita", style = MaterialTheme.typography.headlineSmall)
                    Text(visit.property.title, style = MaterialTheme.typography.titleLarge)
                    Text(visit.property.city, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        visit.scheduledAt.atZone(VisitValidator.TIME_ZONE)
                            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text("Estado: ${visit.status.name.lowercase()}", style = MaterialTheme.typography.bodyLarge)
                    visit.message?.takeIf { it.isNotBlank() }?.let { Text(it) }
                    if (visit.status == VisitStatus.REQUESTED) {
                        OutlinedButton(
                            onClick = onCancel,
                            enabled = !state.isCancelling,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(if (state.isCancelling) "Cancelando..." else "Cancelar visita")
                        }
                    }
                    if (state.errorCode != null || state.domainError != null) {
                        Text(VisitErrorMessages.map(state.errorCode, state.domainError))
                    }
                }
            }
        }
    }
}