package com.thehouse.app.domain.usecase

import com.thehouse.app.domain.model.Favorite
import com.thehouse.app.domain.model.PagedResult
import com.thehouse.app.domain.repository.FavoritesRepository
import kotlinx.coroutines.flow.StateFlow

class GetFavoritesUseCase(private val repository: FavoritesRepository) {
    suspend operator fun invoke(page: Int = 1, pageSize: Int = 20): Result<PagedResult<Favorite>> =
        repository.getFavorites(page, pageSize)
}

class GetFavoriteIdsUseCase(private val repository: FavoritesRepository) {
    suspend operator fun invoke(): Result<Set<String>> = repository.getFavoriteIds()
}

class AddFavoriteUseCase(private val repository: FavoritesRepository) {
    suspend operator fun invoke(propertyId: String): Result<Unit> = repository.addFavorite(propertyId)
}

class RemoveFavoriteUseCase(private val repository: FavoritesRepository) {
    suspend operator fun invoke(propertyId: String): Result<Unit> = repository.removeFavorite(propertyId)
}