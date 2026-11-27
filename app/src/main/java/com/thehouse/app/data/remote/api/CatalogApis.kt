package com.thehouse.app.data.remote.api

import com.thehouse.app.data.remote.dto.catalog.NearbyUniversityResponseDto
import com.thehouse.app.data.remote.dto.catalog.PropertyDetailDto
import com.thehouse.app.data.remote.dto.catalog.PropertyListResponseDto
import com.thehouse.app.data.remote.dto.catalog.UniversityDto
import com.thehouse.app.data.remote.dto.catalog.UniversityListResponseDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Universidades (NV-06, contrato real AllowAnonymous).
 * Solo page/pageSize confirmados. No se agregan query params inventados.
 * La caché HTTP de OkHttp aplica (datos públicos, sin tokens).
 */
interface UniversityApi {

    @GET("api/v1/universities")
    suspend fun getUniversities(
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
    ): Response<UniversityListResponseDto>

    @GET("api/v1/universities/{id}")
    suspend fun getUniversityById(
        @Path("id") id: String,
    ): Response<UniversityDto>

    /**
     * Búsqueda pública por cercanía. Confirmada en Backend:
     * UniversitySearchEndpoint.Path = /api/v1/universities/search.
     * Solo lat/lng (ambos obligatorios); sin page/pageSize en el contrato.
     * Declaración abstracta real: nunca body default (D20/M-MOB-070).
     */
    @GET("api/v1/universities/search")
    suspend fun searchNearbyUniversities(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
    ): Response<NearbyUniversityResponseDto>
}

/**
 * Propiedades (NV-07, contrato real AllowAnonymous).
 * Solo page/pageSize confirmados. No se agregan query params inventados.
 */
interface PropertyApi {

    @GET("api/v1/properties")
    suspend fun getProperties(
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
    ): Response<PropertyListResponseDto>

    @GET("api/v1/properties/{id}")
    suspend fun getPropertyById(
        @Path("id") id: String,
    ): Response<PropertyDetailDto>
}
