package com.thehouse.app.domain.repository

import com.thehouse.app.domain.model.MapBounds
import com.thehouse.app.domain.model.PagedResult
import com.thehouse.app.domain.model.PlaceSuggestion
import com.thehouse.app.domain.model.Property
import com.thehouse.app.domain.model.RoomDetail
import com.thehouse.app.domain.model.RoomMarker
import com.thehouse.app.domain.model.RoomSearchFilters
import com.thehouse.app.domain.model.RoomSummary
import com.thehouse.app.domain.model.University
import kotlinx.coroutines.flow.StateFlow

/**
 * Catálogo de universidades (NV-06, contrato real AllowAnonymous).
 * Solo page/pageSize confirmados; sin filtros/search del backend.
 */
interface UniversityRepository {
    suspend fun getUniversities(
        page: Int = 1,
        pageSize: Int = 20,
    ): Result<PagedResult<University>>

    suspend fun getUniversityById(id: String): Result<University>
}

/**
 * Catálogo de propiedades y búsqueda real de cuartos (NV-07 D10).
 *
 * Extiende el repositorio existente en lugar de crear un segundo cliente:
 * las operaciones de búsqueda usan el mismo Retrofit/OkHttp de D3.
 * Contrato: /rooms/search, /rooms/map, /rooms/{id}, /places/search.
 */
interface PropertyRepository {
    suspend fun getProperties(
        page: Int = 1,
        pageSize: Int = 20,
    ): Result<PagedResult<Property>>

    suspend fun getPropertyById(id: String): Result<Property>

    suspend fun searchRooms(
        universityId: String,
        filters: RoomSearchFilters,
        page: Int = 1,
        pageSize: Int = 20,
    ): Result<PagedResult<RoomSummary>>

    suspend fun getRoomMapMarkers(
        universityId: String,
        filters: RoomSearchFilters,
        bounds: MapBounds,
    ): Result<List<RoomMarker>>

    suspend fun searchPlaces(
        query: String,
        universityId: String,
    ): Result<List<PlaceSuggestion>>

    suspend fun getRoomById(id: String): Result<RoomDetail>
}

/**
 * Universidad seleccionada (dato no sensible en DataStore).
 * Nunca UTTT hardcodeada como dato productivo.
 */
interface SelectedUniversityRepository {
    val selectedUniversityId: StateFlow<String?>
    /** Lee la selección persistida (llamar una vez al arrancar). */
    suspend fun loadSelected()
    suspend fun select(universityId: String)
    suspend fun clear()
}
