package com.thehouse.app.domain.repository

import com.thehouse.app.domain.model.PagedResult
import com.thehouse.app.domain.model.Property
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

    /** Universidades cercanas reales del Backend (distancia y orden calculados allí). */
    suspend fun searchNearbyUniversities(lat: Double, lng: Double): Result<List<NearbyUniversity>>
}

/** Item del contrato GET /universities/search?lat=&lng= (distanceKm en km, 1 decimal). */
data class NearbyUniversity(
    val id: String,
    val code: String,
    val name: String,
    val city: String,
    val distanceKm: Double?,
)

/**
 * Catálogo de propiedades (NV-07 Explore, contrato real AllowAnonymous).
 * Solo page/pageSize confirmados; sin filtros/search del backend.
 */
interface PropertyRepository {
    suspend fun getProperties(
        page: Int = 1,
        pageSize: Int = 20,
    ): Result<PagedResult<Property>>

    suspend fun getPropertyById(id: String): Result<Property>
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
