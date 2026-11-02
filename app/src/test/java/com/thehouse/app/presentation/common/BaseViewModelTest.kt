package com.thehouse.app.presentation.common

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

private data class TestState(val count: Int = 0)

private sealed interface TestEvent : UiEvent {
    data object Increment : TestEvent
    data object EmitEffect : TestEvent
}

private sealed interface TestEffect : UiEffect {
    data class Message(val value: String) : TestEffect
}

private class TestViewModel : BaseViewModel<TestState, TestEvent, TestEffect>(TestState()) {
    override fun onEvent(event: TestEvent) {
        when (event) {
            TestEvent.Increment -> updateState { state ->
                state.copy(count = state.count + 1)
            }

            TestEvent.EmitEffect -> sendEffect(TestEffect.Message("ok"))
        }
    }
}

class BaseViewModelTest {

    @Test
    fun `updates state through explicit events`() {
        val viewModel = TestViewModel()

        viewModel.onEvent(TestEvent.Increment)
        viewModel.onEvent(TestEvent.Increment)

        assertEquals(2, viewModel.uiState.value.count)
    }

    @Test
    fun `emits one shot effects`() = runBlocking {
        val viewModel = TestViewModel()

        viewModel.onEvent(TestEvent.EmitEffect)

        assertEquals(TestEffect.Message("ok"), viewModel.effects.first())
    }
}
