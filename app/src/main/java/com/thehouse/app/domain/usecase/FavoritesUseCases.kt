package com.thehouse.app.domain.usecase

import com.thehouse.app.domain.error.DomainError
import com.thehouse.app.domain.error.DomainException
import com.thehouse.app.domain.model.PagedResult
import com.thehouse.app.domain.repository.FavoritesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GetFavoritesUseCase(private val repository: FavoritesRepository) {
    suspend operator fun invoke(page: Int = 1, pageSize: Int = 20): Result<PagedResult<com.thehouse.app.domain.model.Favorite>> =
        repository.getFavorites(page, pageSize)
}

class GetFavoriteIdsUseCase(private val repository: FavoritesRepository) {
    suspend operator fun invoke(): Result<Set<String>> = repository.getFavoriteIds()
}

class AddFavoriteUseCase(private val repository: FavoritesRepository) {
    suspend operator fun invoke(roomId: String): Result<Unit> = repository.addFavorite(roomId)
}

class RemoveFavoriteUseCase(private val repository: FavoritesRepository) {
    suspend operator fun invoke(roomId: String): Result<Unit> = repository.removeFavorite(roomId)
}

/** Resultado de un toggle optimista con posible reversión. */
sealed interface FavoriteToggleResult {
    data object Added : FavoriteToggleResult
    data object Removed : FavoriteToggleResult
    data class Failed(val error: DomainError, val reverted: Boolean) : FavoriteToggleResult
}

/**
 * Estado compartido de favoritos (D20). Fuente de verdad = Backend; el store
 * solo refleja el último estado confirmado. Sin escrituras automáticas.
 */
class FavoriteStateStore {
    private val _roomIds = MutableStateFlow<Set<String>>(emptySet())
    val roomIds: StateFlow<Set<String>> = _roomIds.asStateFlow()

    fun setAll(ids: Set<String>) { _roomIds.value = ids }
    fun add(roomId: String) { _roomIds.value = _roomIds.value + roomId }
    fun remove(roomId: String) { _roomIds.value = _roomIds.value - roomId }
    fun clear() { _roomIds.value = emptySet() }
    fun contains(roomId: String): Boolean = roomId in _roomIds.value
}

/**
 * Alterna favorito con actualización optimista y reversión ante error.
 * Previene pulsaciones duplicadas mediante [inFlight].
 */
class ToggleFavoriteUseCase(
    private val repository: FavoritesRepository,
    private val store: FavoriteStateStore,
    private val isAuthenticated: () -> Boolean,
    private val inFlight: MutableSet<String>,
) {
    suspend operator fun invoke(roomId: String): FavoriteToggleResult {
        if (!isAuthenticated()) return FavoriteToggleResult.Failed(DomainError.Unauthorized, reverted = false)
        if (!inFlight.add(roomId)) return FavoriteToggleResult.Failed(DomainError.Conflict, reverted = false)
        try {
            val wasFavorite = store.contains(roomId)
            if (wasFavorite) store.remove(roomId) else store.add(roomId)
            val result = if (wasFavorite) repository.removeFavorite(roomId) else repository.addFavorite(roomId)
            return result.fold(
                onSuccess = { if (wasFavorite) FavoriteToggleResult.Removed else FavoriteToggleResult.Added },
                onFailure = { throwable ->
                    if (wasFavorite) store.add(roomId) else store.remove(roomId)
                    val error = (throwable as? DomainException)?.error ?: DomainError.Unknown
                    FavoriteToggleResult.Failed(error, reverted = true)
                },
            )
        } finally {
            inFlight.remove(roomId)
        }
    }
}
