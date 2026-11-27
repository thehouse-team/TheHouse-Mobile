package com.thehouse.app.presentation.visits

import android.util.Log
import com.thehouse.app.BuildConfig
import com.thehouse.app.domain.error.DomainError
import com.thehouse.app.domain.error.VisitErrorCode
import com.thehouse.app.domain.error.VisitException
import com.thehouse.app.domain.model.Visit
import com.thehouse.app.domain.usecase.CreateVisitUseCase
import com.thehouse.app.domain.usecase.CreateRoomVisitUseCase
import com.thehouse.app.domain.usecase.VisitScheduleOptions
import com.thehouse.app.domain.usecase.VisitValidationResult
import com.thehouse.app.domain.usecase.VisitValidator
import com.thehouse.app.presentation.common.BaseViewModel
import com.thehouse.app.presentation.common.UiEffect
import com.thehouse.app.presentation.common.UiEvent
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.UUID

enum class ScheduleVisitStatus {
    IDLE,
    SELECTING,
    SUBMITTING,
    SUCCESS,
    ERROR,
}

data class ScheduleVisitUiState(
    val status: ScheduleVisitStatus = ScheduleVisitStatus.IDLE,
    val date: LocalDate? = null,
    val time: LocalTime? = null,
    val message: String = "",
    val errorCode: VisitErrorCode? = null,
    val domainError: DomainError? = null,
    /** Mensaje visible cuando falta un dato o los horarios son incompatibles. */
    val formError: String? = null,
    val visit: Visit? = null,
)

sealed interface ScheduleVisitEvent : UiEvent {
    data class DateSelected(val date: LocalDate) : ScheduleVisitEvent
    data class TimeSelected(val time: LocalTime) : ScheduleVisitEvent
    data class MessageChanged(val message: String) : ScheduleVisitEvent
    data object Submit : ScheduleVisitEvent
}

sealed interface ScheduleVisitEffect : UiEffect {
    data class Created(val visit: Visit) : ScheduleVisitEffect
}

/**
 * Instrumentación DEBUG segura del flujo de visitas: solo tokens de control,
 * sin tokens/PII/cuerpos. En tests JVM `android.util.Log` no está mockeado:
 * el sink por defecto es un no-op envuelto en runCatching (patrón D19.1).
 */
object VisitFlow {
    const val TAG = "VisitFlow"

    @Volatile
    var sink: (String) -> Unit = { message ->
        runCatching { Log.d(TAG, message) }
        Unit
    }

    fun log(event: String, vararg pairs: Pair<String, Any?>) {
        if (!BuildConfig.DEBUG) return
        val detail = pairs.joinToString(", ") { (k, v) -> "$k=$v" }
        sink("$event${if (detail.isBlank()) "" else " | $detail"}")
    }
}

class ScheduleVisitViewModel(
    private val propertyId: String,
    private val createVisit: CreateVisitUseCase,
    private val now: () -> Instant = { Instant.now() },
    private val newKey: () -> String = { UUID.randomUUID().toString() },
    /** D20: cuando se usa roomId real, NO se envía propertyId (XOR). */
    private val roomId: String? = null,
    private val createRoomVisit: CreateRoomVisitUseCase? = null,
) : BaseViewModel<ScheduleVisitUiState, ScheduleVisitEvent, ScheduleVisitEffect>(
    ScheduleVisitUiState(),
) {
    private var pendingKey: String? = null
    private var pendingIntent: String? = null
    private val targetId: String get() = roomId ?: propertyId

    override fun onEvent(event: ScheduleVisitEvent) {
        when (event) {
            is ScheduleVisitEvent.DateSelected -> {
                // Cambiar la fecha revalida la hora: un horario incompatible
                // con la nueva ventana no puede quedar seleccionado en silencio.
                val previousTime = uiState.value.time
                val validTime = VisitScheduleOptions.revalidateTime(event.date, previousTime, now())
                VisitFlow.log("date_selected", "date" to event.date, "time_kept" to (validTime != null))
                updateState {
                    it.copy(
                        date = event.date,
                        time = validTime,
                        status = ScheduleVisitStatus.SELECTING,
                        errorCode = null,
                        domainError = null,
                        formError = if (previousTime != null && validTime == null) {
                            "La hora anterior ya no es válida para esta fecha. Elige un horario permitido."
                        } else null,
                    )
                }
            }
            is ScheduleVisitEvent.TimeSelected -> {
                VisitFlow.log("time_selected", "time" to event.time)
                updateState {
                    it.copy(
                        time = event.time,
                        status = ScheduleVisitStatus.SELECTING,
                        errorCode = null,
                        domainError = null,
                        formError = null,
                    )
                }
            }
            is ScheduleVisitEvent.MessageChanged -> updateState {
                it.copy(message = event.message.take(VisitValidator.MAX_MESSAGE_LENGTH))
            }
            ScheduleVisitEvent.Submit -> submit()
        }
    }

    private fun submit() {
        val state = uiState.value
        if (state.status == ScheduleVisitStatus.SUBMITTING) return

        // Ya no hay return silencioso: el usuario ve por qué no puede confirmar.
        if (state.date == null || state.time == null) {
            VisitFlow.log("submit_blocked", "reason" to "missing_date_or_time")
            updateState {
                it.copy(
                    status = ScheduleVisitStatus.ERROR,
                    formError = "Selecciona la fecha y el horario de la visita.",
                    errorCode = null,
                    domainError = null,
                )
            }
            return
        }

        val zoned = ZonedDateTime.of(state.date, state.time, VisitValidator.TIME_ZONE)
        val validation = VisitValidator.validateSchedule(
            scheduledAt = zoned.toOffsetDateTime(),
            now = now(),
        )
        if (!validation.isValid) {
            VisitFlow.log("submit_blocked", "reason" to validation.errorCode)
            updateState {
                it.copy(
                    status = ScheduleVisitStatus.ERROR,
                    errorCode = validation.errorCode,
                    domainError = null,
                    formError = null,
                )
            }
            return
        }
        val messageValidation = VisitValidator.validateMessage(state.message.ifBlank { null })
        if (!messageValidation.isValid) {
            VisitFlow.log("submit_blocked", "reason" to "message")
            updateState {
                it.copy(
                    status = ScheduleVisitStatus.ERROR,
                    errorCode = messageValidation.errorCode,
                    domainError = null,
                    formError = "El mensaje no puede superar ${VisitValidator.MAX_MESSAGE_LENGTH} caracteres.",
                )
            }
            return
        }

        val apiScheduledAt = VisitValidator.formatForApi(zoned)
        val intent = "$targetId|$apiScheduledAt|${state.message}"
        if (pendingIntent != intent) {
            pendingKey = newKey()
            pendingIntent = intent
        }
        val key = pendingKey ?: newKey().also { pendingKey = it }
        val usesRoom = roomId != null && createRoomVisit != null

        VisitFlow.log(
            "submit_start", "flow" to if (usesRoom) "room" else "property",
            "scheduledAt" to apiScheduledAt, "keyStable" to (pendingIntent == intent),
        )
        updateState {
            it.copy(
                status = ScheduleVisitStatus.SUBMITTING,
                errorCode = null,
                domainError = null,
                formError = null,
            )
        }
        launch {
            val result = if (usesRoom) {
                createRoomVisit!!(
                    roomId = roomId!!,
                    scheduledAt = apiScheduledAt,
                    message = state.message.ifBlank { null },
                    idempotencyKey = key,
                )
            } else {
                createVisit(
                    propertyId = propertyId,
                    scheduledAt = apiScheduledAt,
                    message = state.message.ifBlank { null },
                    idempotencyKey = key,
                )
            }
            result.fold(
                onSuccess = { creation ->
                    pendingKey = null
                    pendingIntent = null
                    VisitFlow.log("submit_success", "replayed" to creation.replayed)
                    updateState {
                        it.copy(status = ScheduleVisitStatus.SUCCESS, visit = creation.visit)
                    }
                    sendEffect(ScheduleVisitEffect.Created(creation.visit))
                },
                onFailure = { throwable ->
                    val visitException = throwable as? VisitException
                    VisitFlow.log(
                        "submit_failure",
                        "domainError" to (visitException?.domainError?.toString() ?: "Unknown"),
                        "code" to (visitException?.code?.name ?: "UNKNOWN"),
                    )
                    updateState {
                        it.copy(
                            status = ScheduleVisitStatus.ERROR,
                            errorCode = visitException?.code ?: VisitErrorCode.UNKNOWN,
                            domainError = visitException?.domainError ?: DomainError.Unknown,
                        )
                    }
                },
            )
        }
    }

    fun validate(scheduledAt: ZonedDateTime): VisitValidationResult =
        VisitValidator.validateSchedule(scheduledAt.toOffsetDateTime(), now())
}
