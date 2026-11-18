package com.thehouse.app.data.catalog.repository

import com.thehouse.app.data.cache.OfflineCacheStore
import com.thehouse.app.data.catalog.mapper.CatalogMappers.toDomain
import com.thehouse.app.data.error.DataError
import com.thehouse.app.data.error.toDomainError
import com.thehouse.app.data.preferences.PreferencesStore
import com.thehouse.app.data.remote.api.PropertyApi
import com.thehouse.app.data.remote.api.UniversityApi
import com.thehouse.app.data.remote.dto.catalog.PropertyDetailDto
import com.thehouse.app.data.remote.dto.catalog.PropertyListResponseDto
import com.thehouse.app.data.remote.dto.catalog.UniversityListResponseDto
import com.thehouse.app.domain.error.DomainError
import com.thehouse.app.domain.error.DomainException
import com.thehouse.app.domain.model.PagedResult
import com.thehouse.app.domain.model.Property
import com.thehouse.app.domain.model.University
import com.thehouse.app.domain.repository.PropertyRepository
import com.thehouse.app.domain.repository.SelectedUniversityRepository
import com.thehouse.app.domain.repository.UniversityRepository
import java.io.IOException
import java.net.SocketTimeoutException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

class UniversityRepositoryImpl(
    private val api: UniversityApi,
    private val cache: OfflineCacheStore? = null,
) : UniversityRepository {

    override suspend fun getUniversities(
        page: Int,
        pageSize: Int,
    ): Result<PagedResult<University>> {
        val cacheKey = if (page == CatalogPagination.DEFAULT_PAGE) OfflineCacheStore.Keys.UNIVERSITIES_PAGE_1 else null
        return try {
            val response = api.getUniversities(page, pageSize)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                if (cacheKey != null) {
                    cache?.put(cacheKey, body, UniversityListResponseDto.serializer())
                }
                Result.success(body.toDomainPaged())
            } else {
                val httpError = DomainException(DataError.Http(response.code()).toDomainError())
                cacheKey?.let { readCachedUniversities(it) }?.let { Result.success(it) } ?: Result.failure(httpError)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (timeout: SocketTimeoutException) {
            cacheKey?.let { readCachedUniversities(it) }?.let { Result.success(it) }
                ?: Result.failure(DomainException(DomainError.Timeout))
        } catch (network: IOException) {
            cacheKey?.let { readCachedUniversities(it) }?.let { Result.success(it) }
                ?: Result.failure(DomainException(DomainError.Network))
        } catch (unexpected: Exception) {
            cacheKey?.let { readCachedUniversities(it) }?.let { Result.success(it) }
                ?: Result.failure(DomainException(DomainError.Unknown))
        }
    }

    override suspend fun getUniversityById(id: String): Result<University> {
        return try {
            val response = api.getUniversityById(id)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.success(body.toDomain())
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

    private suspend fun readCachedUniversities(key: String): PagedResult<University>? {
        val cached = cache?.get(key, UniversityListResponseDto.serializer()) ?: return null
        return cached.payload.toDomainPaged()
    }
}

private fun UniversityListResponseDto.toDomainPaged(): PagedResult<University> = PagedResult(
    items = items.map { it.toDomain() },
    page = page,
    pageSize = pageSize,
    totalItems = totalItems,
    totalPages = totalPages,
)

class PropertyRepositoryImpl(
    private val api: PropertyApi,
    private val cache: OfflineCacheStore? = null,
) : PropertyRepository {

    override suspend fun getProperties(
        page: Int,
        pageSize: Int,
    ): Result<PagedResult<Property>> {
        val cacheKey = if (page == CatalogPagination.DEFAULT_PAGE) OfflineCacheStore.Keys.PROPERTIES_PAGE_1 else null
        return try {
            val response = api.getProperties(page, pageSize)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                if (cacheKey != null) {
                    cache?.put(cacheKey, body, PropertyListResponseDto.serializer())
                }
                Result.success(body.toDomainPaged())
            } else {
                val httpError = DomainException(DataError.Http(response.code()).toDomainError())
                cacheKey?.let { readCachedProperties(it) }?.let { Result.success(it) } ?: Result.failure(httpError)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (timeout: SocketTimeoutException) {
            cacheKey?.let { readCachedProperties(it) }?.let { Result.success(it) }
                ?: Result.failure(DomainException(DomainError.Timeout))
        } catch (network: IOException) {
            cacheKey?.let { readCachedProperties(it) }?.let { Result.success(it) }
                ?: Result.failure(DomainException(DomainError.Network))
        } catch (unexpected: Exception) {
            cacheKey?.let { readCachedProperties(it) }?.let { Result.success(it) }
                ?: Result.failure(DomainException(DomainError.Unknown))
        }
    }

    override suspend fun getPropertyById(id: String): Result<Property> {
        val cacheKey = OfflineCacheStore.Keys.propertyDetail(id)
        return try {
            val response = api.getPropertyById(id)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                cache?.put(cacheKey, body, PropertyDetailDto.serializer())
                Result.success(body.toDomain())
            } else {
                val httpError = DomainException(DataError.Http(response.code()).toDomainError())
                readCachedProperty(cacheKey)?.let { Result.success(it) } ?: Result.failure(httpError)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (timeout: SocketTimeoutException) {
            readCachedProperty(cacheKey)?.let { Result.success(it) }
                ?: Result.failure(DomainException(DomainError.Timeout))
        } catch (network: IOException) {
            readCachedProperty(cacheKey)?.let { Result.success(it) }
                ?: Result.failure(DomainException(DomainError.Network))
        } catch (unexpected: Exception) {
            readCachedProperty(cacheKey)?.let { Result.success(it) }
                ?: Result.failure(DomainException(DomainError.Unknown))
        }
    }

    private suspend fun readCachedProperties(key: String): PagedResult<Property>? {
        val cached = cache?.get(key, PropertyListResponseDto.serializer()) ?: return null
        return cached.payload.toDomainPaged()
    }

    private suspend fun readCachedProperty(key: String): Property? {
        val cached = cache?.get(key, PropertyDetailDto.serializer()) ?: return null
        return cached.payload.toDomain()
    }
}

private fun PropertyListResponseDto.toDomainPaged(): PagedResult<Property> = PagedResult(
    items = items.map { it.toDomain() },
    page = page,
    pageSize = pageSize,
    totalItems = totalItems,
    totalPages = totalPages,
)

object CatalogPagination {
    const val DEFAULT_PAGE = 1
    const val DEFAULT_PAGE_SIZE = 20
    const val MAX_PAGE_SIZE = 50
}

class SelectedUniversityRepositoryImpl(
    private val preferences: PreferencesStore,
) : SelectedUniversityRepository {

    private val _selectedId = MutableStateFlow<String?>(null)
    override val selectedUniversityId: StateFlow<String?> = _selectedId.asStateFlow()

    override suspend fun loadSelected() {
        _selectedId.value = preferences.observeString(KEY).first()
    }

    override suspend fun select(universityId: String) {
        preferences.putString(KEY, universityId)
        _selectedId.value = universityId
    }

    override suspend fun clear() {
        preferences.remove(KEY)
        _selectedId.value = null
    }

    private companion object {
        const val KEY = "selected_university_id"
    }
}