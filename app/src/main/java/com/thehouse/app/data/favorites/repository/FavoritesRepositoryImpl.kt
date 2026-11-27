package com.thehouse.app.data.favorites.repository

import com.thehouse.app.data.cache.OfflineCacheStore
import com.thehouse.app.data.error.DataError
import com.thehouse.app.data.error.toDomainError
import com.thehouse.app.data.favorites.mapper.toDomain
import com.thehouse.app.data.remote.api.FavoritesApi
import com.thehouse.app.data.remote.dto.favorites.FavoriteIdsResponseDto
import com.thehouse.app.data.remote.dto.favorites.FavoritesListResponseDto
import com.thehouse.app.domain.error.DomainError
import com.thehouse.app.domain.error.DomainException
import com.thehouse.app.domain.model.Favorite
import com.thehouse.app.domain.model.PagedResult
import com.thehouse.app.domain.repository.FavoritesRepository
import java.io.IOException
import java.net.SocketTimeoutException
import kotlinx.coroutines.CancellationException

class FavoritesRepositoryImpl(
    private val api: FavoritesApi,
    private val cache: OfflineCacheStore? = null,
    private val currentUserId: () -> String? = { null },
) : FavoritesRepository {

    override suspend fun getFavorites(page: Int, pageSize: Int): Result<PagedResult<Favorite>> {
        val cacheKey = if (page == 1) currentUserId()?.let { OfflineCacheStore.Keys.favorites(it) } else null
        return try {
            val response = api.getFavorites(page, pageSize)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                if (cacheKey != null) cache?.put(cacheKey, body, FavoritesListResponseDto.serializer(), userScope = currentUserId())
                Result.success(body.toDomainPaged())
            } else {
                val httpError = DomainException(DataError.Http(response.code()).toDomainError())
                cacheKey?.let { readCachedFavorites(it) }?.let { Result.success(it) } ?: Result.failure(httpError)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (timeout: SocketTimeoutException) {
            cacheKey?.let { readCachedFavorites(it) }?.let { Result.success(it) }
                ?: Result.failure(DomainException(DomainError.Timeout))
        } catch (network: IOException) {
            cacheKey?.let { readCachedFavorites(it) }?.let { Result.success(it) }
                ?: Result.failure(DomainException(DomainError.Network))
        } catch (unexpected: Exception) {
            cacheKey?.let { readCachedFavorites(it) }?.let { Result.success(it) }
                ?: Result.failure(DomainException(DomainError.Unknown))
        }
    }

    override suspend fun getFavoriteIds(): Result<Set<String>> {
        val cacheKey = currentUserId()?.let { OfflineCacheStore.Keys.favoriteIds(it) }
        return try {
            val response = api.getFavoriteIds()
            val body = response.body()
            if (response.isSuccessful && body != null) {
                if (cacheKey != null) cache?.put(cacheKey, body, FavoriteIdsResponseDto.serializer(), userScope = currentUserId())
                Result.success(body.effectiveRoomIds())
            } else {
                val httpError = DomainException(DataError.Http(response.code()).toDomainError())
                cacheKey?.let { readCachedFavoriteIds(it) }?.let { Result.success(it) } ?: Result.failure(httpError)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (timeout: SocketTimeoutException) {
            cacheKey?.let { readCachedFavoriteIds(it) }?.let { Result.success(it) }
                ?: Result.failure(DomainException(DomainError.Timeout))
        } catch (network: IOException) {
            cacheKey?.let { readCachedFavoriteIds(it) }?.let { Result.success(it) }
                ?: Result.failure(DomainException(DomainError.Network))
        } catch (unexpected: Exception) {
            cacheKey?.let { readCachedFavoriteIds(it) }?.let { Result.success(it) }
                ?: Result.failure(DomainException(DomainError.Unknown))
        }
    }

    override suspend fun addFavorite(roomId: String): Result<Unit> = mutate { api.addFavorite(roomId) }

    override suspend fun removeFavorite(roomId: String): Result<Unit> = mutate { api.removeFavorite(roomId) }

    private suspend fun mutate(call: suspend () -> retrofit2.Response<Unit>): Result<Unit> = try {
        val response = call()
        if (response.isSuccessful) {
            Result.success(Unit) // 204 No Content
        } else {
            Result.failure(DomainException(DataError.Http(response.code()).toDomainError()))
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (timeout: SocketTimeoutException) {
        Result.failure(DomainException(DomainError.Timeout))
    } catch (network: IOException) {
        Result.failure(DomainException(DomainError.Network))
    } catch (unexpected: Exception) {
        Result.failure(DomainException(DomainError.Unknown))
    }

    private suspend fun readCachedFavorites(key: String): PagedResult<Favorite>? {
        val cached = cache?.get(key, FavoritesListResponseDto.serializer()) ?: return null
        return cached.payload.toDomainPaged()
    }

    private suspend fun readCachedFavoriteIds(key: String): Set<String>? {
        val cached = cache?.get(key, FavoriteIdsResponseDto.serializer()) ?: return null
        return cached.payload.effectiveRoomIds()
    }
}

private fun FavoritesListResponseDto.toDomainPaged(): PagedResult<Favorite> = PagedResult(
    items = items.map { it.toDomain() },
    page = page,
    pageSize = pageSize,
    totalItems = totalItems,
    totalPages = totalPages,
)
