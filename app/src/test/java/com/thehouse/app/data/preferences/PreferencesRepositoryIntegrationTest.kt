package com.thehouse.app.data.preferences

import com.thehouse.app.data.remote.api.PreferencesApi
import com.thehouse.app.data.repository.PreferenceRepositoryImpl
import com.thehouse.app.data.serialization.JsonProvider
import com.thehouse.app.domain.error.DomainError
import com.thehouse.app.domain.error.DomainException
import com.thehouse.app.domain.model.Budget
import com.thehouse.app.domain.model.Preference
import com.thehouse.app.preferences.flow.PreferencesFlowLogger
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * D14 — contrato real de GET/PUT /api/v1/users/me/preferences y errores tipados.
 */
class PreferencesRepositoryIntegrationTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: PreferenceRepositoryImpl

    private val emptyPreferencesJson = """
        {
          "budget": {"min": null, "max": null, "currency": "MXN"},
          "maxDistanceKm": null,
          "essentialServices": [],
          "livingWith": null,
          "placeType": null,
          "pets": null,
          "transport": null,
          "propertyTypes": [],
          "currency": "MXN",
          "quickSetupCompleted": false,
          "version": 1,
          "updatedAt": null
        }
    """.trimIndent()

    private val fullPreferencesJson = """
        {
          "budget": {"min": 2000, "max": 4500, "currency": "MXN"},
          "maxDistanceKm": 3.5,
          "essentialServices": ["wifi", "water"],
          "livingWith": "women",
          "placeType": "room",
          "pets": true,
          "transport": "walk",
          "propertyTypes": ["house"],
          "currency": "MXN",
          "quickSetupCompleted": true,
          "version": 2,
          "updatedAt": "2026-11-26T12:00:00Z"
        }
    """.trimIndent()

    @Before
    fun setup() {
        PreferencesFlowLogger.enabled = false
        server = MockWebServer()
        server.start()
        val json = JsonProvider.create()
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        repository = PreferenceRepositoryImpl(api = retrofit.create(PreferencesApi::class.java))
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun jsonResponse(code: Int, raw: String = emptyPreferencesJson): MockResponse =
        MockResponse.Builder()
            .code(code)
            .addHeader("Content-Type", "application/json")
            .body(raw)
            .build()

    @Test
    fun `GET 200 with defaults returns editable empty preference`() = runTest {
        server.enqueue(jsonResponse(200))

        repository.loadPreferences()

        val pref = repository.preferenceState.first()
        assertTrue(pref != null)
        assertEquals(null, pref?.budget?.max)
        assertEquals("MXN", pref?.budget?.currency)
        assertTrue(pref?.essentialServices.isNullOrEmpty())
    }

    @Test
    fun `GET 200 decodes backend codes to human labels`() = runTest {
        server.enqueue(jsonResponse(200, fullPreferencesJson))

        repository.loadPreferences()

        val pref = repository.preferenceState.first()
        assertEquals(2000.0, pref?.budget?.min)
        assertEquals(4500.0, pref?.budget?.max)
        assertEquals(listOf("Internet", "Agua"), pref?.essentialServices)
        assertEquals("Mujeres", pref?.livingWith)
        assertEquals("Cuarto", pref?.placeType)
        assertEquals("Sí", pref?.pets)
        assertEquals("Caminar", pref?.transport)
        assertEquals(listOf("Casa"), pref?.propertyTypes)
    }

    @Test
    fun `GET 401 maps to Unauthorized`() = runTest {
        server.enqueue(jsonResponse(401, "{}"))

        try {
            repository.loadPreferences()
            fail("Se esperaba DomainException")
        } catch (e: DomainException) {
            assertEquals(DomainError.Unauthorized, e.error)
        }
    }

    @Test
    fun `GET 429 maps to RateLimited`() = runTest {
        server.enqueue(jsonResponse(429, "{}"))

        try {
            repository.loadPreferences()
            fail("Se esperaba DomainException")
        } catch (e: DomainException) {
            assertEquals(DomainError.RateLimited, e.error)
        }
    }

    @Test
    /** D16: 503 es ServiceUnavailable (único 5xx elegible a fallback etiquetado). */
    fun `GET 503 maps to ServiceUnavailable`() = runTest {
        server.enqueue(jsonResponse(503, "{}"))

        try {
            repository.loadPreferences()
            fail("Se esperaba DomainException")
        } catch (e: DomainException) {
            assertEquals(DomainError.ServiceUnavailable, e.error)
        }
    }

    @Test
    fun `GET 404 maps to NotFound`() = runTest {
        server.enqueue(jsonResponse(404, "{}"))

        try {
            repository.loadPreferences()
            fail("Se esperaba DomainException")
        } catch (e: DomainException) {
            assertEquals(DomainError.NotFound, e.error)
        }
    }

    @Test
    fun `PUT sends exact backend types and codes`() = runTest {
        server.enqueue(jsonResponse(200, fullPreferencesJson))
        val preference = Preference(
            budget = Budget(min = 2000.0, max = 4500.0, currency = "MXN"),
            maxDistanceKm = 3.5,
            essentialServices = listOf("Internet", "Agua"),
            livingWith = "Mujeres",
            placeType = "Cuarto",
            pets = "Sí",
            transport = "Caminar",
            propertyTypes = listOf("Casa"),
            currency = "MXN",
        )

        repository.savePreferences(preference)

        val request = server.takeRequest()
        assertEquals("/api/v1/users/me/preferences", request.url.encodedPath)
        assertEquals("PUT", request.method)
        val body = request.body!!.utf8()
        assertTrue(body.contains("\"min\":2000"))
        assertTrue(body.contains("\"max\":4500"))
        assertTrue(body.contains("\"pets\":true"))
        assertTrue(body.contains("\"wifi\""))
        assertTrue(body.contains("\"water\""))
        assertTrue(body.contains("\"women\""))
        assertTrue(body.contains("\"room\""))
        assertTrue(body.contains("\"walk\""))
        assertTrue(body.contains("\"house\""))
        assertFalse(body.contains("Internet"))
        assertFalse(body.contains("Agua"))
    }

    @Test
    fun `PUT 200 updates state from response without trailing GET`() = runTest {
        server.enqueue(jsonResponse(200, fullPreferencesJson))
        val preference = Preference(
            budget = Budget(min = 1000.0, max = 2000.0, currency = "MXN"),
            maxDistanceKm = null,
            essentialServices = null,
            livingWith = "Mujeres",
            placeType = "Cuarto",
            pets = "Sí",
            transport = null,
            propertyTypes = null,
            currency = "MXN",
        )

        repository.savePreferences(preference)

        assertEquals(4500.0, repository.preferenceState.first()?.budget?.max)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `PUT 400 maps to Conflict typed error`() = runTest {
        server.enqueue(jsonResponse(400, "{}"))
        val preference = Preference(null, null, null, null, null, null, null, null, "MXN")

        try {
            repository.savePreferences(preference)
            fail("Se esperaba DomainException")
        } catch (e: DomainException) {
            assertEquals(DomainError.Conflict, e.error)
        }
    }
}
