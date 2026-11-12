package com.thehouse.app.data.remote.api

import com.thehouse.app.data.remote.dto.profile.ProfileDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.PUT

/**
 * Contrato D11:
 * GET   /api/v1/users/me/profile
 * PATCH /api/v1/users/me/profile
 * PUT   /api/v1/users/me/university
 */
interface ProfileApi {

    @GET("api/v1/users/me/profile")
    suspend fun getProfile(): ProfileDto

    @PATCH("api/v1/users/me/profile")
    suspend fun patchProfile(@Body body: PatchProfileRequest)

    @PUT("api/v1/users/me/university")
    suspend fun putUniversity(@Body body: PutUniversityRequest)
}

/** Cuerpo mínimo del PATCH: solo el campo editable (nombre). */
data class PatchProfileRequest(val name: String?)

/** Cuerpo mínimo del PUT de universidad. */
data class PutUniversityRequest(val universityId: String?)
