package com.thehouse.app.domain.repository

import com.thehouse.app.domain.model.Favorite
import com.thehouse.app.domain.model.PagedResult

interface FavoritesRepository {
    suspend fun getFavorites(page: Int = 1, pageSize: Int = 20): Result<PagedResult<Favorite>>
    suspend fun getFavoriteIds(): Result<Set<String>>
    /** roomId real (nunca propertyId legacy para habitaciones). */
    suspend fun addFavorite(roomId: String): Result<Unit>
    suspend fun removeFavorite(roomId: String): Result<Unit>
}
