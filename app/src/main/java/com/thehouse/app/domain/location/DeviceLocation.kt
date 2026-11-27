package com.thehouse.app.domain.location

/** Lectura puntual; nunca seguimiento continuo ni acceso a coordenadas exactas en logs. */
data class DeviceLocation(
    val lat: Double,
    val lng: Double,
    val accuracyMeters: Float? = null,
)

sealed interface LocationResult {
    data class Success(val location: DeviceLocation) : LocationResult
    data object PermissionDenied : LocationResult
    data object PermanentlyDenied : LocationResult
    data object ServicesDisabled : LocationResult
    data object Timeout : LocationResult
    data object Unavailable : LocationResult
    data object Cancelled : LocationResult
}

/** Contrato de dominio: la UI solo solicita una lectura al pulsar el botón. */
interface DeviceLocationProvider {
    /**
     * @param fineGranted si el usuario concedió ACCESS_FINE_LOCATION; si solo concedió
     * ACCESS_COARSE_LOCATION se usa una lectura aproximada y se informa al usuario.
     */
    suspend fun currentLocation(
        fineGranted: Boolean,
        coarseGranted: Boolean,
        timeoutMillis: Long = 10_000L,
    ): LocationResult
}
