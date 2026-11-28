package com.thehouse.app.data.image

/**
 * D21 hotfix: el contrato de `rooms/search`, `rooms/{id}` y Smart Match
 * devuelve `thumbnailUrl` y `photos[].url` como **URLs relativas** al origen
 * del Backend (p. ej. `/api/v1/media/{id}?size=thumb`).
 *
 * Coil recibe un `String` como modelo: una URL relativa no tiene esquema ni
 * host y no puede resolveverse sola. Este resolvedor es puro y testeable;
 * nunca inventa ni modifica URLs absolutas (incluidas las firmadas).
 */
object ImageUrlResolver {

    /**
     * @param value valor del contrato (puede ser null o blank).
     * @param apiBaseUrl URL base configurada del Backend (siempre HTTPS, con '/').
     * @return URL absoluta lista para Coil, o null cuando no hay imagen.
     */
    fun resolve(value: String?, apiBaseUrl: String): String? {
        val raw = value?.trim().takeUnless { it.isNullOrBlank() } ?: return null
        if (raw.startsWith("https://", ignoreCase = true) ||
            raw.startsWith("http://", ignoreCase = true)
        ) {
            return raw
        }
        if (!raw.startsWith("/")) return null
        val base = runCatching { java.net.URI(apiBaseUrl) }.getOrNull() ?: return null
        val scheme = base.scheme ?: return null
        val authority = base.rawAuthority ?: return null
        return "$scheme://$authority$raw"
    }
}
