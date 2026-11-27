package com.thehouse.app.data.serialization

import com.thehouse.app.data.remote.dto.preferences.PreferencesDto
import com.thehouse.app.data.remote.dto.preferences.toDomain
import com.thehouse.app.data.remote.dto.profile.ProfileDto
import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * D19.1 — regresiones de deserialización contra el contrato REAL del Backend.
 *
 * `PreferencesResponse` NO tiene campo `currency` a nivel raíz (solo dentro de
 * `budget`). `ProfileResponse.UpdatedAt` es `DateTimeOffset?`, es decir, puede
 * ser `null`. Los tests D14/D19 anteriores incluían un `currency` inventado y
 * un `updatedAt` string no nullable, enmascarando ambos fallos en runtime.
 */
class D191ContractDeserializationTest {

    private val json = JsonProvider.create()

    /** Respuesta 200 exacta de GET /api/v1/users/me/preferences (sin currency raíz). */
    private val realPreferencesJson = """
        {
          "budget": {"min": 2000, "max": 4500, "currency": "MXN"},
          "maxDistanceKm": 3.5,
          "essentialServices": ["wifi", "water"],
          "livingWith": "women",
          "placeType": "room",
          "pets": true,
          "transport": "walk",
          "propertyTypes": ["house"],
          "quickSetupCompleted": true,
          "version": 2,
          "updatedAt": "2026-11-26T12:00:00Z"
        }
    """.trimIndent()

    /** Respuesta 200 exacta de GET /api/v1/users/me/profile con updatedAt null. */
    private val realProfileJson = """
        {
          "name": "Ana",
          "university": {"id": "u1", "code": "UNAM", "name": "UNAM", "city": "CDMX"},
          "quickSetupCompleted": true,
          "updatedAt": null
        }
    """.trimIndent()

    @Test
    fun `preferences 200 real sin currency raiz deserializa y mantiene MXN del budget`() {
        val dto = json.decodeFromString<PreferencesDto>(realPreferencesJson)
        assertEquals(4500, dto.budget?.max)
        assertEquals("MXN", dto.budget?.currency)
        // El currency efectivo debe venir del budget, no de un campo raíz inexistente.
        assertEquals("MXN", dto.toDomain().currency)
    }

    @Test
    fun `preferences 200 real sin listas ni valores opcionales no lanza y aplica defaults`() {
        val minimal = """
            {
              "budget": {"min": null, "max": null, "currency": "MXN"},
              "maxDistanceKm": null,
              "essentialServices": [],
              "livingWith": null,
              "placeType": null,
              "pets": null,
              "transport": null,
              "propertyTypes": [],
              "quickSetupCompleted": false,
              "version": 1,
              "updatedAt": null
            }
        """.trimIndent()
        val dto = json.decodeFromString<PreferencesDto>(minimal)
        assertNull(dto.pets)
        assertTrue(dto.essentialServices.orEmpty().isEmpty())
        assertEquals("MXN", dto.toDomain().currency)
    }

    @Test
    fun `profile 200 con updatedAt null deserializa y conserva nombre y universidad`() {
        val dto = json.decodeFromString<ProfileDto>(realProfileJson)
        assertEquals("Ana", dto.name)
        assertEquals("UNAM", dto.university?.name)
        assertNull(dto.updatedAt)
    }

    @Test
    fun `profile 200 con updatedAt ausente deserializa sin MissingFieldException`() {
        val withoutUpdatedAt = """
            {"name": null, "university": null, "quickSetupCompleted": false}
        """.trimIndent()
        val dto = json.decodeFromString<ProfileDto>(withoutUpdatedAt)
        assertNull(dto.name)
        assertNull(dto.university)
        assertNull(dto.updatedAt)
    }

    @Test
    fun `los DTOs del contrato real no lanzan SerializationException`() {
        try {
            json.decodeFromString<PreferencesDto>(realPreferencesJson)
            json.decodeFromString<ProfileDto>(realProfileJson)
        } catch (e: SerializationException) {
            throw AssertionError("El contrato real del Backend debe deserializar sin excepción: ${e.message}")
        }
    }
}
