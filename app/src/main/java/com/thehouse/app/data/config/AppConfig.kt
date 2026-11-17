package com.thehouse.app.data.config

/**
 * Configuración no sensible de la aplicación.
 *
 * No debe contener tokens, API keys, passwords ni credenciales.
 */
data class AppConfig(
    val apiBaseUrl: String,
    val httpLoggingEnabled: Boolean,
    /**
     * URL HTTPS del aviso de privacidad, ya normalizada.
     *
     * null significa `BLOCKED-PRIVACY-URL`: no se inventa ni se abre una URL
     * alternativa. La UI muestra el estado no disponible sin crash.
     */
    val privacyPolicyUrl: String? = null,
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
