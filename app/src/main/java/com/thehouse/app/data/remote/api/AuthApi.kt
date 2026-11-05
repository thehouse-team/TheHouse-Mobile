package com.thehouse.app.data.remote.api

import com.thehouse.app.data.remote.dto.auth.AuthResponseDto
import com.thehouse.app.data.remote.dto.auth.LoginRequestDto
import com.thehouse.app.data.remote.dto.auth.RegisterRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

/**
 * Endpoints de autenticación confirmados por el backend TheHouse.
 *
 * - `Cache-Control: no-store` impide que cualquier respuesta de auth (y con
 *   ello tokens) quede en la caché HTTP de OkHttp.
 * - NO se declara `/auth/refresh` ni `/users/me`: NO están confirmados y su
 *   implementación queda BLOCKED-BACKEND hasta evidencia del contrato real.
 */
interface AuthApi {

    @Headers("Cache-Control: no-store, no-cache")
    @POST("api/v1/auth/register")
    suspend fun register(@Body body: RegisterRequestDto): Response<AuthResponseDto>

    @Headers("Cache-Control: no-store, no-cache")
    @POST("api/v1/auth/login")
    suspend fun login(@Body body: LoginRequestDto): Response<AuthResponseDto>

    @Headers("Cache-Control: no-store, no-cache")
    @POST("api/v1/auth/logout")
    suspend fun logout(): Response<Unit>
}
