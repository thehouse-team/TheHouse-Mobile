package com.thehouse.app.presentation.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Base mínima para ViewModels que aplican UDF.
 *
 * - Expone estado inmutable mediante [StateFlow].
 * - Expone efectos one-shot mediante un [Channel].
 * - Recibe eventos de UI mediante [onEvent].
 * - Ejecuta trabajo con [viewModelScope] a través de [launch].
 */
abstract class BaseViewModel<State, Event : UiEvent, Effect : UiEffect>(
    initialState: State,
) : ViewModel() {

    private val _uiState = MutableStateFlow(initialState)
    val uiState: StateFlow<State> = _uiState.asStateFlow()

    private val _effects = Channel<Effect>(Channel.BUFFERED)
    val effects: Flow<Effect> = _effects.receiveAsFlow()

    protected fun setState(state: State) {
        _uiState.value = state
    }

    protected fun updateState(transform: (State) -> State) {
        _uiState.update(transform)
    }

    protected fun sendEffect(effect: Effect) {
        _effects.trySend(effect)
    }

    protected fun launch(block: suspend CoroutineScope.() -> Unit): Job =
        viewModelScope.launch(block = block)

    abstract fun onEvent(event: Event)

    override fun onCleared() {
        _effects.close()
    }
}
