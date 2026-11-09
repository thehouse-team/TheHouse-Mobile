package com.thehouse.app.domain.repository

import com.thehouse.app.domain.model.Favorite
import com.thehouse.app.domain.model.PagedResult

interface FavoritesRepository {
    suspend fun getFavorites(page: Int = 1, pageSize: Int = 20): Result<PagedResult<Favorite>>
    suspend fun getFavoriteIds(): Result<Set<String>>
    suspend fun addFavorite(propertyId: String): Result<Unit>
    suspend fun removeFavorite(propertyId: String): Result<Unit>
}