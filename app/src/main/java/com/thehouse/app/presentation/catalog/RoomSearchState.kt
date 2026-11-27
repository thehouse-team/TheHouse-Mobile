package com.thehouse.app.presentation.catalog

import com.thehouse.app.domain.error.DomainError
import com.thehouse.app.domain.error.DomainException
import com.thehouse.app.domain.room.RoomSearchItem
import com.thehouse.app.presentation.common.UiError
import com.thehouse.app.presentation.common.toUiError

/**
 * Estados de la búsqueda real de habitaciones (D20, UDF).
 * Cubre Loading / Content / Empty / Error / Refreshing / Pagination.
 * `Refreshing` y `Pagination` conservan el contenido previo.
 */
sealed interface RoomSearchState {
    data object Loading : RoomSearchState
    data class Content(
        val items: List<RoomSearchItem>,
        val page: Int,
        val totalPages: Int,
        val totalItems: Long,
        val isRefreshing: Boolean = false,
        val isPaginating: Boolean = false,
        val paginationError: UiError? = null,
    ) : RoomSearchState
    data object Empty : RoomSearchState
    data class Error(val error: UiError) : RoomSearchState
}

fun Result<*>.toRoomSearchStateError(): RoomSearchState.Error {
    val error = (exceptionOrNull() as? DomainException)?.error ?: DomainError.Unknown
    return RoomSearchState.Error(error.toUiError())
}

fun RoomSearchState.errorMessage(): String? = when (this) {
    is RoomSearchState.Error -> when (error) {
        UiError.Network -> "Sin conexión. Inténtalo de nuevo."
        UiError.Timeout -> "Tiempo agotado. Inténtalo de nuevo."
        UiError.Unauthorized -> "Sesión inválida. Inicia sesión."
        UiError.NotFound -> "No encontramos alojamientos."
        UiError.Server -> "Error del servidor. Inténtalo de nuevo."
        else -> "Algo salió mal. Inténtalo de nuevo."
    }
    else -> null
}
