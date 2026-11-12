package com.thehouse.app.data.remote.api

import com.thehouse.app.data.remote.dto.preferences.PreferencesDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT

/**
 * Contrato D11:
 * GET /api/v1/users/me/preferences
 * PUT /api/v1/users/me/preferences
 */
interface PreferencesApi {

    @GET("api/v1/users/me/preferences")
    suspend fun getPreferences(): PreferencesDto

    @PUT("api/v1/users/me/preferences")
    suspend fun putPreferences(@Body body: PreferencesPatchBody)
}

data class PreferencesPatchBody(
    val budget: BudgetPatchBody?,
    val maxDistanceKm: Double?,
    val essentialServices: List<String>?,
    val livingWith: String?,
    val placeType: String?,
    val pets: String?,
    val transport: String?,
    val propertyTypes: List<String>?
)

data class BudgetPatchBody(
    val min: Double?,
    val max: Double?,
    val currency: String?
)
