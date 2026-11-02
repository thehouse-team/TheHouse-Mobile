package com.thehouse.app.data.config

import com.thehouse.app.BuildConfig

/**
 * Proveedor de configuración no secreta desde BuildConfig.
 *
 * Los valores reales de entornos futuros deberán inyectarse sin versionar secretos.
 */
object AppConfigProvider {
    fun provide(): AppConfig = AppConfig(
        apiBaseUrl = BuildConfig.API_BASE_URL,
        httpLoggingEnabled = BuildConfig.ENABLE_HTTP_LOGGING,
    )
}
