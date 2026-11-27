package com.thehouse.app.data.remote.api

import com.thehouse.app.data.remote.dto.legal.AcceptLegalResponseDto
import com.thehouse.app.data.remote.dto.legal.LegalResponseDto
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT

/**
 * Contrato Backend confirmado (780b7be, §3 de docs/mobile/contrato-pendiente-mobile.md):
 * - `GET /api/v1/legal` (público) → versiones y URLs (URLs PUEDEN ser null).
 * - `PUT /api/v1/users/me/legal` (auth) → aceptación de versiones vigentes.
 */
interface LegalApi {

    @GET("api/v1/legal")
    suspend fun getLegal(): Response<LegalResponseDto>

    @PUT("api/v1/users/me/legal")
    suspend fun acceptLegal(@Body body: AcceptLegalRequestDto): Response<AcceptLegalResponseDto>
}

/** Cuerpo exacto del PUT: solo las dos versiones vigentes. */
@Serializable
data class AcceptLegalRequestDto(
    val termsVersion: String,
    val privacyVersion: String,
)
