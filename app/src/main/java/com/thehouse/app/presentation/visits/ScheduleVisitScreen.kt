package com.thehouse.app.presentation.visits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thehouse.app.domain.model.Visit
import com.thehouse.app.presentation.components.TheHouseCard
import com.thehouse.app.presentation.components.TheHousePrimaryButton
import com.thehouse.app.presentation.components.TheHouseScreen
import com.thehouse.app.presentation.components.TheHouseTextField
import com.thehouse.app.ui.theme.Spacing
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
    var timeText by remember {
        mutableStateOf(state.time?.format(DateTimeFormatter.ofPattern("HH:mm")).orEmpty())
    }
    TheHouseScreen(
        title = "Agendar visita",
        onBack = onBack,
        modifier = modifier,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.xl, vertical = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            Text(
                text = "Horario de atención: 08:00–20:00 (America/Mexico_City).",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TheHouseCard {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                    TheHouseTextField(
                        value = dateText,
                        onValueChange = {
                            dateText = it
                            onDateChanged(it)
                        },
                        label = "Fecha (AAAA-MM-DD)",
                    )
                    TheHouseTextField(
                        value = timeText,
                        onValueChange = {
                            timeText = it
                            onTimeChanged(it)
                        },
                        label = "Hora (HH:mm)",
                    )
                    TheHouseTextField(
                        value = state.message,
                        onValueChange = onMessageChanged,
                        label = "Mensaje (opcional, máx. 300)",
                        singleLine = false,
                        minLines = 3,
                    )
                    if (state.errorCode != null || state.domainError != null) {
                        Text(
                            text = VisitErrorMessages.map(state.errorCode, state.domainError),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    TheHousePrimaryButton(
                        text = "Confirmar visita",
                        onClick = onSubmit,
                        enabled = state.status != ScheduleVisitStatus.SUBMITTING,
                        loading = state.status == ScheduleVisitStatus.SUBMITTING,
                    )
                }
            }
        }
    }
}
