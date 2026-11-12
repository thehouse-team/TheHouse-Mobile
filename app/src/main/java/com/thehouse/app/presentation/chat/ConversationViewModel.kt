package com.thehouse.app.presentation.chat

import com.thehouse.app.domain.error.ChatErrorCode
import com.thehouse.app.domain.error.ChatException
import com.thehouse.app.domain.error.DomainError
import com.thehouse.app.domain.model.ChatMessage
import com.thehouse.app.domain.model.MessagesFetch
import com.thehouse.app.domain.usecase.GetMessagesUseCase
import com.thehouse.app.domain.usecase.MarkConversationReadUseCase
import com.thehouse.app.domain.usecase.SendMessageUseCase
import com.thehouse.app.presentation.common.BaseViewModel
import com.thehouse.app.presentation.common.UiEffect
import com.thehouse.app.presentation.common.UiEvent
import com.thehouse.app.presentation.requests.RequestErrorMessages
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class ConversationUiState(
    val isLoadingInitial: Boolean = false,
    val messages: List<ChatMessage> = emptyList(),
    val hasMoreHistory: Boolean = false,
    val composerText: String = "",
    val isSending: Boolean = false,
    /** UUID del envío en curso o pendiente de reintentar: el retry conserva el mismo UUID. */
    val pendingClientMessageId: String? = null,
    val isClosed: Boolean = false,
    val errorCode: ChatErrorCode? = null,
    val errorMessage: String? = null,
)

sealed interface ConversationEvent : UiEvent {
    /** Carga inicial + inicio de polling (al entrar a la pantalla). */
    data object Load : ConversationEvent
    /** Detiene el polling cuando la pantalla ya no está activa. */
    data object StopPolling : ConversationEvent
    data class ComposerChanged(val value: String) : ConversationEvent
    data object Send : ConversationEvent
}

sealed interface ConversationEffect : UiEffect {
    data class ShowMessage(val text: String) : ConversationEffect
}

/**
 * ViewModel de conversación con polling incremental eficiente:
 *
 * - Un único fetch inicial (sin `after`), luego polls con `after=<último mensaje>`
 *   e `If-None-Match=<último ETag>`.
 * - Respeta `pollAfterSeconds` del backend (default 10 s).
 * - 304 → no modifica la lista.
 * - El polling vive en un Job del ViewModel y se cancela con StopPolling/onCleared.
 */
class ConversationViewModel(
    private val conversationId: String,
    private val getMessages: GetMessagesUseCase,
    private val sendMessage: SendMessageUseCase,
    private val markRead: MarkConversationReadUseCase,
    private val pollIntervalOverrideMs: Long? = null,
) : BaseViewModel<ConversationUiState, ConversationEvent, ConversationEffect>(
    ConversationUiState(),
) {

    private var pollingJob: Job? = null
    private var lastMessageId: String? = null
    private var etag: String? = null
    /** Intervalo confirmado por el backend (pollAfterSeconds); default 10 s. */
    private var pollAfterMs: Long = 10_000L

    override fun onCleared() {
        pollingJob?.cancel()
        super.onCleared()
    }

    override fun onEvent(event: ConversationEvent) {
        when (event) {
            ConversationEvent.Load -> {
                if (pollingJob?.isActive != true) startPolling()
            }
            ConversationEvent.StopPolling -> {
                pollingJob?.cancel()
                pollingJob = null
            }
            is ConversationEvent.ComposerChanged -> updateState {
                // Contrato: text 1–1000.
                it.copy(composerText = event.value.take(1000), errorMessage = null)
            }
            ConversationEvent.Send -> send()
        }
    }

    private fun startPolling() {
        updateState { it.copy(isLoadingInitial = it.messages.isEmpty(), errorCode = null, errorMessage = null) }
        pollingJob?.cancel()
        pollingJob = launch {
            try {
                var intervalMs = 0L // primer fetch inmediato
                while (true) {
                    if (intervalMs > 0) delay(intervalMs)
                    intervalMs = pollOnce()
                }
            } catch (cancelled: CancellationException) {
                // Regla de contrato: CancellationException → rethrow.
                throw cancelled
            } catch (unexpected: Exception) {
                updateState {
                    it.copy(
                        isLoadingInitial = false,
                        errorCode = ChatErrorCode.UNKNOWN,
                        errorMessage = RequestErrorMessages.mapChat(null, DomainError.Unknown),
                    )
                }
            }
        }
    }

    /** Devuelve el intervalo para el siguiente poll. Nunca lanza (no rompe el ciclo). */
    private suspend fun pollOnce(): Long {
        val result = getMessages(conversationId, after = lastMessageId, etag = etag)
        result.fold(
            onSuccess = { fetch ->
                when (fetch) {
                    is MessagesFetch.NotModified -> Unit // 304: no tocar la lista
                    is MessagesFetch.Modified -> applyMessages(fetch)
                }
                updateState { it.copy(isLoadingInitial = false, errorCode = null, errorMessage = null) }
                return nextIntervalMs()            },
            onFailure = { throwable ->
                if (throwable is ChatException &&
                    throwable.code == ChatErrorCode.CONVERSATION_CLOSED
                ) {
                    updateState {
                        it.copy(
                            isLoadingInitial = false,
                            isClosed = true,
                            errorCode = ChatErrorCode.CONVERSATION_CLOSED,
                            errorMessage = RequestErrorMessages.mapChat(throwable.code),
                        )
                    }
                    // Conversación cerrada: dejar de hacer polling.
                    pollingJob?.cancel()
                    pollingJob = null
                    return 0
                }
                val exception = throwable as? ChatException
                updateState {
                    it.copy(
                        isLoadingInitial = false,
                        errorCode = exception?.code ?: ChatErrorCode.UNKNOWN,
                        errorMessage = RequestErrorMessages.mapChat(
                            exception?.code,
                            exception?.domainError,
                        ),
                    )
                }
                return nextIntervalMs()
            },
        )
    }

    private fun applyMessages(fetch: MessagesFetch.Modified) {
        etag = fetch.etag
        pollAfterMs = fetch.page.pollAfterSeconds * 1000
        updateState { state ->
            val existing = state.messages.associateBy { it.id }
            val merged = (state.messages + fetch.page.items.filter { it.id !in existing })
                .sortedBy { it.createdAt?.toEpochMilli() ?: Long.MAX_VALUE }
            val lastId = merged.lastOrNull()?.id ?: lastMessageId
            lastMessageId = lastId
            state.copy(
                messages = merged,
                hasMoreHistory = fetch.page.hasMore,
                isClosed = state.isClosed,
            )
        }
        // Marcar leído tras éxito; el unread local se refresca al re-entrar a la lista.
        val last = lastMessageId
        if (last != null) {
            launch { markRead(conversationId, last) }
        }
    }

    private fun nextIntervalMs(): Long {
        pollIntervalOverrideMs?.let { return it }
        // Respeta pollAfterSeconds del backend (default 10 s).
        return pollAfterMs
    }

    private fun send() {
        val state = uiState.value
        if (state.isSending || state.isClosed) return
        val text = state.composerText.trim()
        if (text.isEmpty()) return
        // Idempotencia: mismo mensaje en retry/doble toque → mismo UUID;
        // una acción NUEVA (draft nuevo tras éxito) → UUID nuevo.
        val clientMessageId = state.pendingClientMessageId ?: UUID.randomUUID().toString()
        updateState { it.copy(isSending = true, pendingClientMessageId = clientMessageId, errorMessage = null) }
        launch {
            sendMessage(conversationId, text, clientMessageId).fold(
                onSuccess = { sent ->
                    updateState { current ->
                        val withoutDuplicate =
                            current.messages.filter { it.id != sent.message.id } + sent.message
                        current.copy(
                            isSending = false,
                            pendingClientMessageId = null,
                            composerText = "",
                            messages = withoutDuplicate.sortedBy {
                                it.createdAt?.toEpochMilli() ?: Long.MAX_VALUE
                            },
                        )
                    }
                    lastMessageId = sent.message.id
                    launch { markRead(conversationId, sent.message.id) }
                },
                onFailure = { throwable ->
                    val exception = throwable as? ChatException
                    val closed = exception?.code == ChatErrorCode.CONVERSATION_CLOSED
                    // pendingClientMessageId se conserva: el retry reutiliza el UUID.
                    updateState {
                        it.copy(
                            isSending = false,
                            isClosed = it.isClosed || closed,
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
