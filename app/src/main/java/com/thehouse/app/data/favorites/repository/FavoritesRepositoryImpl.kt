package com.thehouse.app.data.favorites.repository

import com.thehouse.app.data.error.DataError
import com.thehouse.app.data.error.toDomainError
import com.thehouse.app.data.favorites.mapper.toDomain
import com.thehouse.app.data.remote.api.FavoritesApi
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
) : FavoritesRepository {

    override suspend fun getFavorites(page: Int, pageSize: Int): Result<PagedResult<Favorite>> {
        return try {
            val response = api.getFavorites(page, pageSize)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.success(
                    PagedResult(
                        items = body.items.map { it.toDomain() },
                        page = body.page,
                        pageSize = body.pageSize,
                        totalItems = body.totalItems.toLong(),
                        totalPages = body.totalPages,
                    ),
                )
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
    }

    override suspend fun getFavoriteIds(): Result<Set<String>> {
        return try {
            val response = api.getFavoriteIds()
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.success(body.propertyIds.toSet())
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
    }

    override suspend fun addFavorite(propertyId: String): Result<Unit> {
        return try {
            val response = api.addFavorite(propertyId)
            if (response.isSuccessful) {
                Result.success(Unit)
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
    }

    override suspend fun removeFavorite(propertyId: String): Result<Unit> {
        return try {
            val response = api.removeFavorite(propertyId)
            if (response.isSuccessful) {
                Result.success(Unit)
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
    }
}