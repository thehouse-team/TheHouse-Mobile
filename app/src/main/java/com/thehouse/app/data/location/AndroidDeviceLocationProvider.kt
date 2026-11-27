package com.thehouse.app.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import com.thehouse.app.domain.location.DeviceLocation
import com.thehouse.app.domain.location.DeviceLocationProvider
import com.thehouse.app.domain.location.LocationResult
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Implementación con APIs del sistema (sin Google Maps ni dependencias nuevas).
 * Una sola lectura puntual; se cancela el listener al resolver, fallar o cancelar.
 * Nunca registra coordenadas en logs.
 */
@Suppress("MissingPermission") // Permisos comprobados explícitamente arriba y en la UI (contextual).
class AndroidDeviceLocationProvider(
    private val context: Context,
) : DeviceLocationProvider {

    override suspend fun currentLocation(
        fineGranted: Boolean,
        coarseGranted: Boolean,
        timeoutMillis: Long,
    ): LocationResult {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return LocationResult.Unavailable

        val hasFine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED || fineGranted
        val hasCoarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED || coarseGranted

        if (!hasFine && !hasCoarse) return LocationResult.PermissionDenied

        val providers = mutableListOf<String>()
        if (hasFine && manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            providers.add(LocationManager.GPS_PROVIDER)
        }
        if (manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            providers.add(LocationManager.NETWORK_PROVIDER)
        }
        if (providers.isEmpty()) return LocationResult.ServicesDisabled

        return suspendCancellableCoroutine { continuation ->
            var listener: LocationListener? = null
            val cancel = {
                listener?.let { runCatching { manager.removeUpdates(it) } }
                listener = null
            }

            val lastKnown = providers.mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }
                .filter { it.isUsable() }
                .maxByOrNull { it.accuracy }
            if (lastKnown != null) {
                continuation.resume(LocationResult.Success(lastKnown.toDomain()))
                return@suspendCancellableCoroutine
            }

            val callback = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    if (!location.isUsable()) return
                    cancel()
                    if (continuation.isActive) {
                        continuation.resume(LocationResult.Success(location.toDomain()))
                    }
                }

                @Deprecated("Compatibilidad con API 26.")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
                override fun onProviderEnabled(provider: String) = Unit
                override fun onProviderDisabled(provider: String) = Unit
            }
            listener = callback

            val handler = android.os.Handler(Looper.getMainLooper())
            val timeout = Runnable {
                if (continuation.isActive) {
                    cancel()
                    continuation.resume(LocationResult.Timeout)
                }
            }
            handler.postDelayed(timeout, timeoutMillis)

            continuation.invokeOnCancellation {
                handler.removeCallbacks(timeout)
                cancel()
            }

            providers.forEach { provider ->
                val criteriaFine = hasFine && provider == LocationManager.GPS_PROVIDER
                val minTime = if (criteriaFine) 0L else 1_000L
                val minDistance = if (criteriaFine) 0f else 100f
                runCatching {
                    manager.requestLocationUpdates(provider, minTime, minDistance, callback, Looper.getMainLooper())
                }
            }
        }
    }

    private fun Location.isUsable(): Boolean =
        latitude in -90.0..90.0 && longitude >= -180.0 && longitude <= 180.0 &&
            hasAccuracy() && accuracy >= 0f

    private fun Location.toDomain() = DeviceLocation(
        lat = latitude,
        lng = longitude,
        accuracyMeters = if (hasAccuracy()) accuracy else null,
    )
}
