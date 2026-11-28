package com.thehouse.app.data.remote.api

import com.thehouse.app.data.remote.dto.device.PushDeviceListResponseDto
import com.thehouse.app.data.remote.dto.device.PushDeviceRequestDto
import com.thehouse.app.data.remote.dto.device.PushDeviceResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * Contrato real verificado en Backend `TheHouse.Http/PushDevicesEndpoint.cs`.
 *
 * IMPORTANTE: todas las funciones son declaraciones abstractas SIN cuerpo
 * Kotlin default. Retrofit trata los métodos con cuerpo por defecto como
 * "default methods" y NO emite HTTP (fallo D20 corregido en RoomApi).
 */
interface DeviceApi {

    /** 201 = creado, 200 = actualizado; 409 installation_conflict. */
    @PUT("api/v1/users/me/devices/{installationId}")
    suspend fun registerDevice(
        @Path("installationId") installationId: String,
        @Body body: PushDeviceRequestDto,
    ): Response<PushDeviceResponseDto>

    /** 204 = eliminado, 404 = no existe (o pertenece a otra cuenta). */
    @DELETE("api/v1/users/me/devices/{installationId}")
    suspend fun unregisterDevice(
        @Path("installationId") installationId: String,
    ): Response<Unit>

    @GET("api/v1/users/me/devices")
    suspend fun listDevices(): Response<PushDeviceListResponseDto>
}
