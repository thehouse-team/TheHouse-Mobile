package com.thehouse.app.data.remote.api

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
