package com.thehouse.app.presentation.visits

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thehouse.app.domain.model.Visit
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun ScheduleVisitDestination(
    propertyId: String,
    factory: ViewModelProvider.Factory,
    onBack: () -> Unit,
    onCreated: (Visit) -> Unit,
) {
    val vm: ScheduleVisitViewModel = viewModel(key = "schedule-visit:$propertyId", factory = factory)
    val state by vm.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(vm, onCreated) {
        vm.effects.collect { effect ->
            when (effect) {
                is ScheduleVisitEffect.Created -> onCreated(effect.visit)
            }
        }
    }
    ScheduleVisitScreen(
        state = state,
        onBack = onBack,
        onDateChanged = { text ->
            runCatching { LocalDate.parse(text, DateTimeFormatter.ISO_LOCAL_DATE) }
                .getOrNull()?.let { vm.onEvent(ScheduleVisitEvent.DateSelected(it)) }
        },
        onTimeChanged = { text ->
            runCatching { LocalTime.parse(text, DateTimeFormatter.ofPattern("HH:mm")) }
                .getOrNull()?.let { vm.onEvent(ScheduleVisitEvent.TimeSelected(it)) }
        },
        onMessageChanged = { vm.onEvent(ScheduleVisitEvent.MessageChanged(it)) },
        onSubmit = { vm.onEvent(ScheduleVisitEvent.Submit) },
    )
}

@Composable
fun ScheduleVisitScreen(
    state: ScheduleVisitUiState,
    onBack: () -> Unit,
    onDateChanged: (String) -> Unit,
    onTimeChanged: (String) -> Unit,
    onMessageChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var dateText by remember { mutableStateOf(state.date?.toString().orEmpty()) }
    var timeText by remember { mutableStateOf(state.time?.format(DateTimeFormatter.ofPattern("HH:mm")).orEmpty()) }
    Scaffold(
        modifier = modifier,
        topBar = {
            TextButton(onClick = onBack, modifier = Modifier.padding(horizontal = 16.dp)) {
                Text("Volver")
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Agendar visita", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Horario de atención: 08:00–20:00 (America/Mexico_City).",
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedTextField(
                value = dateText,
                onValueChange = {
                    dateText = it
                    onDateChanged(it)
                },
                label = { Text("Fecha (AAAA-MM-DD)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = timeText,
                onValueChange = {
                    timeText = it
                    onTimeChanged(it)
                },
                label = { Text("Hora (HH:mm)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.message,
                onValueChange = onMessageChanged,
                label = { Text("Mensaje (opcional, máx. 300)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
            )
            if (state.errorCode != null || state.domainError != null) {
                Text(
                    text = VisitErrorMessages.map(state.errorCode, state.domainError),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            Button(
                onClick = onSubmit,
                enabled = state.status != ScheduleVisitStatus.SUBMITTING,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) {
                if (state.status == ScheduleVisitStatus.SUBMITTING) {
                    CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                    Text("Enviando...")
                } else {
                    Text("Confirmar visita")
                }
            }
        }
    }
}