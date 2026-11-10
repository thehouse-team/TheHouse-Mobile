package com.thehouse.app.presentation.catalog

import com.thehouse.app.domain.error.DomainError
import com.thehouse.app.domain.error.DomainException
import com.thehouse.app.presentation.common.UiError
import com.thehouse.app.presentation.common.toUiError

/**
 * Estados de carga del catálogo (D5, UDF).
 * Solo loading/success/empty/error + retry (sin detalle técnico en UI).
 */
sealed interface CatalogLoadState<out T> {
    data object Loading : CatalogLoadState<Nothing>
    data class Success<T>(val items: List<T>) : CatalogLoadState<T>
    data object Empty : CatalogLoadState<Nothing>
    data class Error(val error: UiError) : CatalogLoadState<Nothing>
}

@Suppress("UNCHECKED_CAST")
fun <T> Result<List<T>>.toCatalogState(): CatalogLoadState<T> {
    return if (isSuccess) {
        val items = getOrNull().orEmpty()
        if (items.isEmpty()) CatalogLoadState.Empty else CatalogLoadState.Success(items)
    } else {
        val error = (exceptionOrNull() as? DomainException)?.error ?: DomainError.Unknown
        CatalogLoadState.Error(error.toUiError())
    }
}

/** Mensaje seguro para errores de catálogo (sin detalle técnico). */
fun CatalogLoadState<*>.errorMessage(): String? = when (this) {
    is CatalogLoadState.Error -> when (error) {
        UiError.Network -> "Sin conexión. Inténtalo de nuevo."
        UiError.Timeout -> "Tiempo agotado. Inténtalo de nuevo."
        UiError.BadRequest -> "Los filtros no son válidos. Ajusta la búsqueda."
        UiError.Unauthorized -> "Sesión inválida. Inicia sesión."
        UiError.NotFound -> "No encontrado."
        UiError.Server -> "Error del servidor. Inténtalo de nuevo."
        else -> "Algo salió mal. Inténtalo de nuevo."
    }
    else -> null
}
