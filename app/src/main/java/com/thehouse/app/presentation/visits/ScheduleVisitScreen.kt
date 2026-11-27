package com.thehouse.app.presentation.visits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thehouse.app.domain.model.Visit
import com.thehouse.app.domain.usecase.VisitScheduleOptions
import com.thehouse.app.domain.usecase.VisitValidator
import com.thehouse.app.presentation.components.TheHouseCard
import com.thehouse.app.presentation.components.TheHousePrimaryButton
import com.thehouse.app.presentation.components.TheHouseScreen
import com.thehouse.app.presentation.components.TheHouseTextField
import com.thehouse.app.ui.theme.Spacing
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/** TestTags y textos estables del formulario de visitas (D20 hotfix). */
object VisitFormTags {
    const val DATE_BUTTON = "VISIT_DATE_BUTTON"
    const val SLOT = "VISIT_SLOT_"
    const val MESSAGE = "VISIT_MESSAGE"
    const val SUBMIT = "VISIT_SUBMIT"
    const val ERROR = "VISIT_ERROR"
}

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
        onDateSelected = { vm.onEvent(ScheduleVisitEvent.DateSelected(it)) },
        onTimeSelected = { vm.onEvent(ScheduleVisitEvent.TimeSelected(it)) },
        onMessageChanged = { vm.onEvent(ScheduleVisitEvent.MessageChanged(it)) },
        onSubmit = { vm.onEvent(ScheduleVisitEvent.Submit) },
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ScheduleVisitScreen(
    state: ScheduleVisitUiState,
    onBack: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onTimeSelected: (LocalTime) -> Unit,
    onMessageChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    // "Ahora" se calcula una vez por composición, no en cada recomposición.
    val now = remember { Instant.now() }
    val today = remember(now) { VisitScheduleOptions.earliestDate(now) }
    val latest = remember(now) { VisitScheduleOptions.latestDate(now) }
    val selectableDates = remember(today, latest) {
        object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val date = VisitScheduleOptions.dateFromPickerMillis(utcTimeMillis)
                return !date.isBefore(today) && !date.isAfter(latest)
            }
        }
    }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = state.date?.let { VisitScheduleOptions.pickerMillisFromDate(it) },
        selectableDates = selectableDates,
    )
    val allowedTimes = remember(state.date, now) {
        state.date?.let { VisitScheduleOptions.allowedTimesFor(it, now) }.orEmpty()
    }

    TheHouseScreen(
        title = "Agendar visita",
        onBack = onBack,
        modifier = modifier,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.xl, vertical = Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.lg),
            ) {
                Text(
                    text = "Horario de Ciudad de México · 08:00–20:00.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                TheHouseCard {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                        FilledTonalButton(
                            onClick = { showDatePicker = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag(VisitFormTags.DATE_BUTTON)
                                .semantics { contentDescription = "Seleccionar fecha de la visita" },
                        ) {
                            Text(
                                state.date?.let { VisitScheduleOptions.formatDate(it) }
                                    ?: "Seleccionar fecha",
                            )
                        }

                        Text(
                            text = "Horarios permitidos para solicitar una visita",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        if (state.date == null) {
                            Text(
                                text = "Elige primero una fecha para ver los horarios.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else if (allowedTimes.isEmpty()) {
                            Text(
                                text = "No hay horarios permitidos para esa fecha. Prueba con otro día.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        } else {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                            ) {
                                allowedTimes.forEach { time ->
                                    val label = time.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
                                    FilterChip(
                                        selected = state.time == time,
                                        onClick = { onTimeSelected(time) },
                                        label = { Text(label) },
                                        modifier = Modifier
                                            .heightIn(min = 48.dp)
                                            .testTag(VisitFormTags.SLOT + label)
                                            .semantics {
                                                contentDescription = "Visita a las $label"
                                                selected = state.time == time
                                            },
                                    )
                                }
                            }
                        }

                        TheHouseTextField(
                            value = state.message,
                            onValueChange = onMessageChanged,
                            label = "Mensaje (opcional, máx. 300)",
                            singleLine = false,
                            minLines = 3,
                            modifier = Modifier
                                .testTag(VisitFormTags.MESSAGE)
                                .semantics { contentDescription = "Mensaje opcional para la visita" },
                        )
                        Text(
                            text = "${state.message.length}/${VisitValidator.MAX_MESSAGE_LENGTH}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.semantics {
                                contentDescription = "${state.message.length} de ${VisitValidator.MAX_MESSAGE_LENGTH} caracteres"
                            },
                        )

                        if (state.date != null && state.time != null) {
                            Text(
                                text = "Resumen: ${VisitScheduleOptions.formatDate(state.date)} a las " +
                                    state.time.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")) +
                                    " (Ciudad de México).",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }

            // CTA fuera del scroll: siempre accesible, con teclado abierto o cerrado.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = Spacing.xl, vertical = Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                val errorText = state.formError
                    ?: state.errorCode?.let { VisitErrorMessages.map(it, state.domainError) }
                    ?: state.domainError?.let { VisitErrorMessages.map(null, it) }
                if (errorText != null) {
                    Text(
                        text = errorText,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .testTag(VisitFormTags.ERROR)
                            .semantics {
                                liveRegion = LiveRegionMode.Assertive
                                contentDescription = "Error: $errorText"
                            },
                    )
                }
                TheHousePrimaryButton(
                    text = "Confirmar visita",
                    onClick = onSubmit,
                    enabled = state.status != ScheduleVisitStatus.SUBMITTING,
                    loading = state.status == ScheduleVisitStatus.SUBMITTING,
                    testTag = VisitFormTags.SUBMIT,
                )
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            onDateSelected(VisitScheduleOptions.dateFromPickerMillis(it))
                        }
                        showDatePicker = false
                    },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDatePicker = false },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) { Text("Cancelar") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
