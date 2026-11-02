package com.thehouse.app.presentation.common

/**
 * Estado base de una pantalla.
 *
 * [Loading], [Empty] y [Error] no transportan datos de negocio.
 * [Content] es el único estado que expone datos a la UI.
 */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data object Empty : UiState<Nothing>
    data class Content<T>(val data: T) : UiState<T>
    data class Error(val error: UiError) : UiState<Nothing>
}
