package com.thehouse.app.data.config

/**
 * Configuración no sensible de la aplicación.
 *
 * No debe contener tokens, API keys, passwords ni credenciales.
 */
data class AppConfig(
    val apiBaseUrl: String,
    val httpLoggingEnabled: Boolean,
) {
    init {
        require(apiBaseUrl.startsWith("https://")) {
            "apiBaseUrl must use HTTPS"
        }
        require(apiBaseUrl.endsWith("/")) {
            "apiBaseUrl must end with '/'"
        }
    }
}
