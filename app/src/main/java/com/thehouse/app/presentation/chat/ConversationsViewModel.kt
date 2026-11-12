package com.thehouse.app.presentation.chat

import com.thehouse.app.domain.error.ChatErrorCode
import com.thehouse.app.domain.error.ChatException
import com.thehouse.app.domain.model.Conversation
import com.thehouse.app.domain.usecase.GetConversationsUseCase
import com.thehouse.app.presentation.common.BaseViewModel
import com.thehouse.app.presentation.common.UiEffect
import com.thehouse.app.presentation.common.UiEvent
import com.thehouse.app.presentation.requests.RequestErrorMessages

data class ConversationsUiState(
    val isLoading: Boolean = false,
    val items: List<Conversation> = emptyList(),
    val errorCode: ChatErrorCode? = null,
    val errorMessage: String? = null,
)

sealed interface ConversationsEvent : UiEvent {
    /** Recarga al entrar a la pantalla (lifecycle), no desde recomposición. */
    data object Load : ConversationsEvent
}

sealed interface ConversationsEffect : UiEffect

class ConversationsViewModel(
    private val getConversations: GetConversationsUseCase,
) : BaseViewModel<ConversationsUiState, ConversationsEvent, ConversationsEffect>(
    ConversationsUiState(),
) {

    override fun onEvent(event: ConversationsEvent) {
        when (event) {
            ConversationsEvent.Load -> load()
        }
    }

    fun load() {
        if (uiState.value.isLoading) return
        updateState { it.copy(isLoading = true, errorCode = null, errorMessage = null) }
        launch {
            getConversations().fold(
                onSuccess = { items ->
                    updateState { it.copy(isLoading = false, items = items) }
                },
                onFailure = { throwable ->
                    val exception = throwable as? ChatException
                    updateState {
                        it.copy(
                            isLoading = false,
                            errorCode = exception?.code ?: ChatErrorCode.UNKNOWN,
                            errorMessage = RequestErrorMessages.mapChat(
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
