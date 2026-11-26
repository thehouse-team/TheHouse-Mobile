package com.thehouse.app.data.remote.api

import com.thehouse.app.data.remote.dto.preferences.PreferencesDto
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT

/**
 * Contrato confirmado (backend productivo):
 * GET /api/v1/users/me/preferences → 200 con PreferencesResponse
 * PUT /api/v1/users/me/preferences → 200 con PreferencesResponse (reemplazo)
 */
interface PreferencesApi {

    @GET("api/v1/users/me/preferences")
    suspend fun getPreferences(): PreferencesDto

    @PUT("api/v1/users/me/preferences")
    suspend fun putPreferences(@Body body: PreferencesPatchBody): PreferencesDto
}

/** Request exacto del backend: Pets bool?, Budget.Min/Max int?, códigos de catálogo cerrados. */
@Serializable
data class PreferencesPatchBody(
    val budget: BudgetPatchBody?,
    val maxDistanceKm: Double?,
    val essentialServices: List<String>?,
    val livingWith: String?,
    val placeType: String?,
    val pets: Boolean?,
    val transport: String?,
    val propertyTypes: List<String>?,
)

@Serializable
data class BudgetPatchBody(
    val min: Int?,
    val max: Int?,
    val currency: String?,
)
