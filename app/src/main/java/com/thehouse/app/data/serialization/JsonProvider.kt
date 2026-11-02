package com.thehouse.app.data.serialization

import kotlinx.serialization.json.Json

/**
 * Configuración JSON compartida por la capa Data.
 *
 * No contiene DTOs ni modelos de negocio.
 */
object JsonProvider {
    fun create(): Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = false
    }
}
