package com.thehouse.app.presentation.requests

import com.thehouse.app.domain.error.RequestErrorCode
import com.thehouse.app.domain.error.RequestException
import com.thehouse.app.domain.model.QuickQuestion
import com.thehouse.app.domain.model.RoomRequest
import com.thehouse.app.domain.usecase.CreateRequestUseCase
import com.thehouse.app.presentation.common.BaseViewModel
import com.thehouse.app.presentation.common.UiEffect
import com.thehouse.app.presentation.common.UiEvent

data class CreateRequestUiState(
    val roomId: String = "",
    val quickQuestions: Set<QuickQuestion> = emptySet(),
    val message: String = "",
    val isSending: Boolean = false,
    val errorCode: RequestErrorCode? = null,
    val errorMessage: String? = null,
)

sealed interface CreateRequestEvent : UiEvent {
    data class QuickQuestionToggled(val question: QuickQuestion) : CreateRequestEvent
    data class MessageChanged(val value: String) : CreateRequestEvent
    data object Send : CreateRequestEvent
}

sealed interface CreateRequestEffect : UiEffect {
    data class Created(val request: RoomRequest) : CreateRequestEffect
    data object OpenConversation : CreateRequestEffect
}

class CreateRequestViewModel(
    private val roomId: String,
    private val createRequest: CreateRequestUseCase,
) : BaseViewModel<CreateRequestUiState, CreateRequestEvent, CreateRequestEffect>(
    CreateRequestUiState(roomId = roomId),
) {

    override fun onEvent(event: CreateRequestEvent) {
        when (event) {
            is CreateRequestEvent.QuickQuestionToggled -> updateState {
                val updated = if (event.question in it.quickQuestions) {
                    it.quickQuestions - event.question
                } else {
                    it.quickQuestions + event.question
                }
                it.copy(
                    quickQuestions = updated,
                    errorCode = null,
                    errorMessage = null,
                )
            }
            is CreateRequestEvent.MessageChanged -> updateState {
                // Contrato: message 1–500.
                it.copy(message = event.value.take(500), errorCode = null, errorMessage = null)
            }
            CreateRequestEvent.Send -> send()
        }
    }

    private fun send() {
        val state = uiState.value
        if (state.isSending) return
        val hasQuickQuestions = state.quickQuestions.isNotEmpty()
        val message = state.message.trim()
        // Regla de contrato: al menos una quickQuestion O un message 1–500.
        if (!hasQuickQuestions && message.isEmpty()) {
            updateState {
                it.copy(
                    errorCode = null,
                    errorMessage = "Escribe un mensaje o selecciona al menos una pregunta.",
                )
            }
            return
        }
        if (message.length > 500) return
        updateState { it.copy(isSending = true, errorCode = null, errorMessage = null) }
        launch {
            val result = createRequest(
                roomId = roomId,
                quickQuestions = state.quickQuestions.toList(),
                message = message.takeIf { it.isNotBlank() },
            )
            result.fold(
                onSuccess = { request ->
                    updateState { it.copy(isSending = false) }
                    sendEffect(CreateRequestEffect.Created(request))
                },
                onFailure = { throwable ->
                    val exception = throwable as? RequestException
                    updateState {
                        it.copy(
                            isSending = false,
                            errorCode = exception?.code ?: RequestErrorCode.UNKNOWN,
                            errorMessage = RequestErrorMessages.map(
                                exception?.code,
                                exception?.domainError,
                            ),
                        )
                    }
                },
            )
        }
    }
}
