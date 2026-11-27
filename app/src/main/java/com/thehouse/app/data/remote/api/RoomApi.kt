package com.thehouse.app.data.remote.api

import com.thehouse.app.data.remote.dto.room.RoomDetailResponseDto
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Contrato real del Backend (`SearchEndpoint`): `GET /api/v1/rooms/{id}`.
 * Requiere sesión; el token se añade con el interceptor existente.
 */
interface RoomApi {
    @GET("api/v1/rooms/{id}")
    suspend fun getRoom(@Path("id") id: String): RoomDetailResponseDto
}
