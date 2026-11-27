package com.thehouse.app.data.catalog

import com.thehouse.app.data.catalog.repository.UniversityRepositoryImpl
import com.thehouse.app.data.remote.api.UniversityApi
import com.thehouse.app.data.serialization.JsonProvider
import com.thehouse.app.domain.error.DomainException
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/** Contrato PÚBLICO real verificado: GET /api/v1/universities/search?lat=&lng=. */
class UniversitySearchHttpIntegrationTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: UniversityRepositoryImpl

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(JsonProvider.create().asConverterFactory("application/json".toMediaType()))
            .build()
        repository = UniversityRepositoryImpl(api = retrofit.create(UniversityApi::class.java))
    }

    @After
    fun tearDown() = server.close()

    @Test
    fun `nearby search sends real GET with lat and lng query`() = runTest {
        server.enqueue(
            MockResponse.Builder()
                .code(200)
                .body("""{"items":[{"id":"u1","code":"UAEH","name":"UAEH Este","city":"Pachuca","distanceKm":0.4},{"id":"u2","code":"UTTT","name":"UTTT","city":"Tula","distanceKm":12.3}]}""")
                .build(),
        )

        val result = repository.searchNearbyUniversities(19.9185, -99.3645)
        assertTrue(result.isSuccess)
        val items = result.getOrThrow()
        assertEquals(listOf("u1", "u2"), items.map { it.id })
        assertEquals(0.4, items.first().distanceKm!!, 0.0)

        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/api/v1/universities/search", request.url.encodedPath)
        assertEquals("19.9185", request.url.queryParameter("lat"))
        assertEquals("-99.3645", request.url.queryParameter("lng"))
    }

    @Test
    fun `empty response is preserved without inventing universities`() = runTest {
        server.enqueue(MockResponse.Builder().code(200).body("""{"items":[]}""").build())
        val result = repository.searchNearbyUniversities(19.9185, -99.3645)
        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().isEmpty())
    }

    @Test
    fun `http 400 maps to validation error without local fallback`() = runTest {
        server.enqueue(
            MockResponse.Builder()
                .code(400)
                .body("""{"errors":{"request":["Entrada inválida."]}}""")
                .build(),
        )
        val result = repository.searchNearbyUniversities(99.0, 0.0)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is DomainException)
    }
}
