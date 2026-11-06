package com.thehouse.app.data.remote.api

import com.thehouse.app.data.remote.dto.auth.AuthResponseDto
import com.thehouse.app.data.remote.dto.auth.CurrentUserDto
import com.thehouse.app.data.remote.dto.auth.LoginRequestDto
import com.thehouse.app.data.remote.dto.auth.RefreshRequestDto
import com.thehouse.app.data.remote.dto.auth.RegisterRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

/**
 * Endpoints de autenticación confirmados por el backend TheHouse (D3).
 *
 * - `Cache-Control: no-store` impide que cualquier respuesta de auth (y con
 *   ello tokens) quede en la caché HTTP de OkHttp.
 * - `POST /api/v1/auth/refresh` y `GET /api/v1/users/me` confirmados en D3.
 *   Respuesta de refresh = mismo shape que login; el cliente SIEMPRE
 *   reemplaza el refreshToken anterior.
 */
interface AuthApi {

    @Headers("Cache-Control: no-store, no-cache")
    @POST("api/v1/auth/register")
    suspend fun register(@Body body: RegisterRequestDto): Response<AuthResponseDto>

    @Headers("Cache-Control: no-store, no-cache")
    @POST("api/v1/auth/login")
    suspend fun login(@Body body: LoginRequestDto): Response<AuthResponseDto>

    @Headers("Cache-Control: no-store, no-cache")
    @POST("api/v1/auth/refresh")
    suspend fun refresh(@Body body: RefreshRequestDto): Response<AuthResponseDto>

    @Headers("Cache-Control: no-store, no-cache")
    @GET("api/v1/users/me")
    suspend fun getCurrentUser(
        @Header("Authorization") authHeader: String,
    ): Response<CurrentUserDto>

    @Headers("Cache-Control: no-store, no-cache")
    @POST("api/v1/auth/logout")
    suspend fun logout(
        @Header("Authorization") authHeader: String? = null,
    ): Response<Unit>
}
