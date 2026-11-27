package com.thehouse.app.data.remote.dto.device

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Espejo exacto de `PushDeviceRequest` del Backend:
 * `string? FcmToken, string? Platform, string? AppVersion, string? Locale`.
 *
 * El Backend rechaza campos desconocidos (JsonUnmappedMemberHandling.Disallow),
 * por lo que NO se envía ningún campo adicional.
 */
@Serializable
data class PushDeviceRequestDto(
    @SerialName("fcmToken") val fcmToken: String,
    @SerialName("platform") val platform: String,
    @SerialName("appVersion") val appVersion: String? = null,
    @SerialName("locale") val locale: String? = null,
)

@Serializable
data class PushDeviceResponseDto(
    @SerialName("installationId") val installationId: String,
    @SerialName("platform") val platform: String,
    @SerialName("appVersion") val appVersion: String? = null,
    @SerialName("registeredAt") val registeredAt: String,
    @SerialName("updatedAt") val updatedAt: String,
)

@Serializable
data class PushDeviceListResponseDto(
    @SerialName("items") val items: List<PushDeviceResponseDto>,
)
