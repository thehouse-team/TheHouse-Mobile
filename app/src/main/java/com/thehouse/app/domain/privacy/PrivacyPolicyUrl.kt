package com.thehouse.app.domain.privacy

import java.net.URI

/**
 * Validación pura de la URL del aviso de privacidad.
 *
 * Reglas NV-15:
 * - Solo HTTPS.
 * - Host obligatorio.
 * - Cualquier valor ausente, vacío, relativo o malformado se normaliza a null
 *   (la UI muestra el bloqueo, nunca inventa ni abre una URL alternativa).
 *
 * No depende de Android: puede probarse en JVM.
 */
object PrivacyPolicyUrl {

    fun normalize(raw: String?): String? {
        val value = raw?.trim().orEmpty()
        if (value.isEmpty()) return null
        val uri = runCatching { URI(value) }.getOrNull() ?: return null
        if (!uri.isAbsolute) return null
        if (!uri.scheme.equals("https", ignoreCase = true)) return null
        if (uri.host.isNullOrBlank()) return null
        return uri.toString()
    }
}
