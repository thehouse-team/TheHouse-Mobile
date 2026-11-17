package com.thehouse.app.data.config

import com.thehouse.app.BuildConfig
import com.thehouse.app.domain.privacy.PrivacyPolicyUrl

/**
 * Proveedor de configuración no secreta desde BuildConfig.
 *
 * Los valores reales de entornos futuros deberán inyectarse sin versionar secretos.
 */
object AppConfigProvider {
    fun provide(): AppConfig = AppConfig(
        apiBaseUrl = BuildConfig.API_BASE_URL,
        httpLoggingEnabled = BuildConfig.ENABLE_HTTP_LOGGING,
        // Vacío por defecto: BLOCKED-PRIVACY-URL. Solo se enlaza una URL
        // HTTPS real cuando se configure centralizadamente.
        privacyPolicyUrl = PrivacyPolicyUrl.normalize(BuildConfig.PRIVACY_POLICY_URL),
    )
}
