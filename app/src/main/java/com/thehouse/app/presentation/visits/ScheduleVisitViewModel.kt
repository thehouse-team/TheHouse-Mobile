package com.thehouse.app.presentation.visits

import com.thehouse.app.domain.error.DomainError
import com.thehouse.app.domain.error.VisitErrorCode
import com.thehouse.app.domain.error.VisitException
import com.thehouse.app.domain.model.Visit
import com.thehouse.app.domain.usecase.CreateVisitUseCase
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

class ScheduleVisitViewModel(
    private val propertyId: String,
    private val createVisit: CreateVisitUseCase,
    private val now: () -> Instant = { Instant.now() },
    private val newKey: () -> String = { UUID.randomUUID().toString() },
) : BaseViewModel<ScheduleVisitUiState, ScheduleVisitEvent, ScheduleVisitEffect>(
    ScheduleVisitUiState(),
) {
    private var pendingKey: String? = null
    private var pendingIntent: String? = null

    override fun onEvent(event: ScheduleVisitEvent) {
        when (event) {
            is ScheduleVisitEvent.DateSelected -> updateState {
                it.copy(date = event.date, status = ScheduleVisitStatus.SELECTING, errorCode = null, domainError = null)
            }
            is ScheduleVisitEvent.TimeSelected -> updateState {
                it.copy(time = event.time, status = ScheduleVisitStatus.SELECTING, errorCode = null, domainError = null)
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
        val date = state.date ?: return
        val time = state.time ?: return

        val zoned = ZonedDateTime.of(date, time, VisitValidator.TIME_ZONE)
        val validation = VisitValidator.validateSchedule(
            scheduledAt = zoned.toOffsetDateTime(),
            now = now(),
        )
        if (!validation.isValid) {
            updateState {
                it.copy(
                    status = ScheduleVisitStatus.ERROR,
                    errorCode = validation.errorCode,
                    domainError = null,
                )
            }
            return
        }
        val messageValidation = VisitValidator.validateMessage(state.message.ifBlank { null })
        if (!messageValidation.isValid) {
            updateState {
                it.copy(status = ScheduleVisitStatus.ERROR, errorCode = messageValidation.errorCode)
            }
            return
        }

        val apiScheduledAt = VisitValidator.formatForApi(zoned)
        val intent = "$propertyId|$apiScheduledAt|${state.message}"
        if (pendingIntent != intent) {
            pendingKey = newKey()
            pendingIntent = intent
        }
        val key = pendingKey ?: newKey().also { pendingKey = it }

        updateState { it.copy(status = ScheduleVisitStatus.SUBMITTING, errorCode = null, domainError = null) }
        launch {
            val result = createVisit(
                propertyId = propertyId,
                scheduledAt = apiScheduledAt,
                message = state.message.ifBlank { null },
                idempotencyKey = key,
            )
            result.fold(
                onSuccess = { creation ->
                    pendingKey = null
                    pendingIntent = null
                    updateState {
                        it.copy(status = ScheduleVisitStatus.SUCCESS, visit = creation.visit)
                    }
                    sendEffect(ScheduleVisitEffect.Created(creation.visit))
                },
                onFailure = { throwable ->
                    val visitException = throwable as? VisitException
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