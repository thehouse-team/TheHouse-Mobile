package com.thehouse.app.data.catalog.repository

import com.thehouse.app.data.catalog.mapper.CatalogMappers.toDomain
import com.thehouse.app.data.error.DataError
import com.thehouse.app.data.error.toDomainError
import com.thehouse.app.data.remote.api.PropertyApi
import com.thehouse.app.data.remote.api.UniversityApi
import com.thehouse.app.domain.error.DomainError
import com.thehouse.app.domain.error.DomainException
import com.thehouse.app.domain.model.PagedResult
import com.thehouse.app.domain.model.Property
import com.thehouse.app.domain.model.University
import com.thehouse.app.domain.repository.PropertyRepository
import com.thehouse.app.domain.repository.SelectedUniversityRepository
import com.thehouse.app.domain.repository.UniversityRepository
import com.thehouse.app.data.preferences.PreferencesStore
import java.io.IOException
import java.net.SocketTimeoutException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

/**
 * Implementación Data de universidades (NV-06).
 * Reutiliza el Retrofit/HttpClient existente (incl. D3 Bearer/authenticator);
 * no crea segundo cliente. La API es AllowAnonymous: no se limpian credenciales.
 */
class UniversityRepositoryImpl(
    private val api: UniversityApi,
) : UniversityRepository {

    override suspend fun getUniversities(
        page: Int,
        pageSize: Int,
    ): Result<PagedResult<University>> {
        return try {
            val response = api.getUniversities(page, pageSize)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.success(
                    PagedResult(
                        items = body.items.map { it.toDomain() },
                        page = body.page,
                        pageSize = body.pageSize,
                        totalItems = body.totalItems,
                        totalPages = body.totalPages,
                    ),
                )
            } else {
                Result.failure(
                    DomainException(DataError.Http(response.code()).toDomainError()),
                )
            }
        } catch (timeout: SocketTimeoutException) {
            Result.failure(DomainException(DomainError.Timeout))
        } catch (network: IOException) {
            Result.failure(DomainException(DomainError.Network))
        } catch (unexpected: Exception) {
            Result.failure(DomainException(DomainError.Unknown))
        }
    }

    override suspend fun getUniversityById(id: String): Result<University> {
        return try {
            val response = api.getUniversityById(id)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.success(body.toDomain())
            } else {
                Result.failure(
                    DomainException(DataError.Http(response.code()).toDomainError()),
                )
            }
        } catch (timeout: SocketTimeoutException) {
            Result.failure(DomainException(DomainError.Timeout))
        } catch (network: IOException) {
            Result.failure(DomainException(DomainError.Network))
        } catch (unexpected: Exception) {
            Result.failure(DomainException(DomainError.Unknown))
        }
    }
}

/**
 * Implementación Data de propiedades (NV-07).
 */
class PropertyRepositoryImpl(
    private val api: PropertyApi,
) : PropertyRepository {

    override suspend fun getProperties(
        page: Int,
        pageSize: Int,
    ): Result<PagedResult<Property>> {
        return try {
            val response = api.getProperties(page, pageSize)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.success(
                    PagedResult(
                        items = body.items.map { it.toDomain() },
                        page = body.page,
                        pageSize = body.pageSize,
                        totalItems = body.totalItems,
                        totalPages = body.totalPages,
                    ),
                )
            } else {
                Result.failure(
                    DomainException(DataError.Http(response.code()).toDomainError()),
                )
            }
        } catch (timeout: SocketTimeoutException) {
            Result.failure(DomainException(DomainError.Timeout))
        } catch (network: IOException) {
            Result.failure(DomainException(DomainError.Network))
        } catch (unexpected: Exception) {
            Result.failure(DomainException(DomainError.Unknown))
        }
    }

    override suspend fun getPropertyById(id: String): Result<Property> {
        return try {
            val response = api.getPropertyById(id)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.success(body.toDomain())
            } else {
                Result.failure(
                    DomainException(DataError.Http(response.code()).toDomainError()),
                )
            }
        } catch (timeout: SocketTimeoutException) {
            Result.failure(DomainException(DomainError.Timeout))
        } catch (network: IOException) {
            Result.failure(DomainException(DomainError.Network))
        } catch (unexpected: Exception) {
            Result.failure(DomainException(DomainError.Unknown))
        }
    }
}

/**
 * Totales reales de paginación (paginación solo si contrato la define).
 */
object CatalogPagination {
    const val DEFAULT_PAGE = 1
    const val DEFAULT_PAGE_SIZE = 20
    const val MAX_PAGE_SIZE = 50
}

/**
 * Selección persistente (dato no sensible en DataStore existente).
 * No es secreto: clave fija "selected_university_id".
 * Si el backend no devuelve universidades, la selección sigue nula
 * (sin UTTT hardcodeada como dato productivo).
 */
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
