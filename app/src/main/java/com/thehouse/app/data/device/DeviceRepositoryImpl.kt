package com.thehouse.app.data.device

import com.thehouse.app.data.remote.api.DeviceApi
import com.thehouse.app.data.remote.dto.device.PushDeviceRequestDto
import com.thehouse.app.domain.device.DeviceError
import com.thehouse.app.domain.device.DeviceRegistrationResult
import com.thehouse.app.domain.device.InstallationId
import com.thehouse.app.domain.device.RegisteredDevice
import com.thehouse.app.domain.repository.DeviceException
import com.thehouse.app.domain.repository.DeviceRepository
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * Implementación real del contrato de devices. Nunca devuelve éxito ante un
 * error HTTP: [DeviceException] transporta el motivo semántico.
 */
class DeviceRepositoryImpl(
    private val api: DeviceApi,
) : DeviceRepository {

    override suspend fun register(
        installationId: InstallationId,
        fcmToken: String,
        platform: String,
        appVersion: String?,
        locale: String?,
    ): Result<DeviceRegistrationResult> = try {
        val response = api.registerDevice(
            installationId = installationId.value,
            body = PushDeviceRequestDto(
                fcmToken = fcmToken,
                platform = platform,
                appVersion = appVersion,
                locale = locale,
            ),
        )
        when (response.code()) {
            201 -> Result.success(DeviceRegistrationResult.Created)
            200 -> Result.success(DeviceRegistrationResult.Updated)
            else -> Result.failure(DeviceException(errorFor(response.code())))
        }
    } catch (timeout: SocketTimeoutException) {
        Result.failure(DeviceException(DeviceError.Timeout))
    } catch (io: IOException) {
        Result.failure(DeviceException(DeviceError.Network))
    } catch (t: Throwable) {
        Result.failure(DeviceException(DeviceError.Unknown))
    }

    override suspend fun unregister(installationId: InstallationId): Result<Unit> = try {
        val response = api.unregisterDevice(installationId.value)
        when (response.code()) {
            204, 404 -> Result.success(Unit)
            else -> Result.failure(DeviceException(errorFor(response.code())))
        }
    } catch (timeout: SocketTimeoutException) {
        Result.failure(DeviceException(DeviceError.Timeout))
    } catch (io: IOException) {
        Result.failure(DeviceException(DeviceError.Network))
    } catch (t: Throwable) {
        Result.failure(DeviceException(DeviceError.Unknown))
    }

    override suspend fun list(): Result<List<RegisteredDevice>> = try {
        val response = api.listDevices()
        if (response.isSuccessful && response.body() != null) {
            Result.success(response.body()!!.items.map { dto ->
                RegisteredDevice(
                    installationId = dto.installationId,
                    platform = dto.platform,
                    appVersion = dto.appVersion,
                    registeredAt = dto.registeredAt,
                    updatedAt = dto.updatedAt,
                )
            })
        } else {
            Result.failure(DeviceException(errorFor(response.code())))
        }
    } catch (timeout: SocketTimeoutException) {
        Result.failure(DeviceException(DeviceError.Timeout))
    } catch (io: IOException) {
        Result.failure(DeviceException(DeviceError.Network))
    } catch (t: Throwable) {
        Result.failure(DeviceException(DeviceError.Unknown))
    }

    private fun errorFor(code: Int): DeviceError = when (code) {
        400, 413, 415 -> DeviceError.Validation
        401 -> DeviceError.Unauthorized
        404 -> DeviceError.NotFound
        409 -> DeviceError.Conflict
        429 -> DeviceError.RateLimited
        503 -> DeviceError.ServiceUnavailable
        in 500..599 -> DeviceError.Server
        else -> DeviceError.Unknown
    }
}
