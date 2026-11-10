package com.thehouse.app.data.catalog.repository

import com.thehouse.app.data.catalog.mapper.CatalogMappers.toDomain
import com.thehouse.app.data.error.DataError
import com.thehouse.app.data.error.toDomainError
import com.thehouse.app.data.remote.api.PropertyApi
import com.thehouse.app.data.remote.api.SearchApi
import com.thehouse.app.data.remote.api.UniversityApi
import com.thehouse.app.data.remote.dto.search.RoomMapResponseDto
import com.thehouse.app.data.remote.dto.search.RoomSearchResponseDto
import com.thehouse.app.data.search.mapper.SearchMappers.toDomain
import com.thehouse.app.domain.error.DomainError
import com.thehouse.app.domain.error.DomainException
import com.thehouse.app.domain.model.MapBounds
import com.thehouse.app.domain.model.PagedResult
import com.thehouse.app.domain.model.PlaceSuggestion
import com.thehouse.app.domain.model.Property
import com.thehouse.app.domain.model.RoomDetail
import com.thehouse.app.domain.model.RoomMarker
import com.thehouse.app.domain.model.RoomSearchFilters
import com.thehouse.app.domain.model.RoomSummary
import com.thehouse.app.domain.model.University
import com.thehouse.app.domain.repository.PropertyRepository
import com.thehouse.app.domain.repository.SelectedUniversityRepository
import com.thehouse.app.domain.repository.UniversityRepository
import com.thehouse.app.data.preferences.PreferencesStore
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import retrofit2.Response

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

/**
 * Implementación Data de propiedades (NV-07).
 */
class PropertyRepositoryImpl(
    private val api: PropertyApi,
    private val searchApi: SearchApi,
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

    // ===== NV-07 D10: búsqueda real de cuartos y colonias =====

    override suspend fun searchRooms(
        universityId: String,
        filters: RoomSearchFilters,
        page: Int,
        pageSize: Int,
    ): Result<PagedResult<RoomSummary>> = safeCall {
        searchApi.searchRooms(
            universityId = universityId,
            minPrice = filters.minPrice,
            maxPrice = filters.maxPrice,
            roomType = filters.roomType,
            bathroom = filters.bathroom,
            buildingType = filters.buildingType,
            allowedTenants = filters.allowedTenants,
            services = filters.services.takeIf { it.isNotEmpty() }?.sorted()?.joinToString(","),
            maxDistanceKm = filters.maxDistanceKm,
            sort = filters.sort.apiValue,
            page = page,
            pageSize = pageSize,
        )
    }.map { body: RoomSearchResponseDto ->
        PagedResult(
            items = body.items.map { it.toDomain() },
            page = body.page,
            pageSize = body.pageSize,
            totalItems = body.totalItems,
            totalPages = body.totalPages,
        )
    }

    override suspend fun getRoomMapMarkers(
        universityId: String,
        filters: RoomSearchFilters,
        bounds: MapBounds,
    ): Result<List<RoomMarker>> = safeCall {
        searchApi.getRoomMap(
            universityId = universityId,
            minPrice = filters.minPrice,
            maxPrice = filters.maxPrice,
            roomType = filters.roomType,
            bathroom = filters.bathroom,
            buildingType = filters.buildingType,
            allowedTenants = filters.allowedTenants,
            services = filters.services.takeIf { it.isNotEmpty() }?.sorted()?.joinToString(","),
            maxDistanceKm = filters.maxDistanceKm,
            bbox = String.format(
                Locale.US,
                "%.6f,%.6f,%.6f,%.6f",
                bounds.minLongitude,
                bounds.minLatitude,
                bounds.maxLongitude,
                bounds.maxLatitude,
            ),
        )
    }.map { body: RoomMapResponseDto -> body.items.map { it.toDomain() } }

    override suspend fun searchPlaces(
        query: String,
        universityId: String,
    ): Result<List<PlaceSuggestion>> = safeCall {
        searchApi.searchPlaces(query, universityId)
    }.map { body -> body.items.map { it.toDomain() } }

    override suspend fun getRoomById(id: String): Result<RoomDetail> = safeCall {
        searchApi.getRoomById(id)
    }.map { it.toDomain() }

    private suspend fun <T> safeCall(block: suspend () -> Response<T>): Result<T> {
        return try {
            val response = block()
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.success(body)
            } else {
                Result.failure(
                    DomainException(DataError.Http(response.code()).toDomainError()),
                )
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
