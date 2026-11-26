package com.thehouse.app.perf

import com.thehouse.app.data.catalog.repository.PropertyRepositoryImpl
import com.thehouse.app.data.catalog.repository.UniversityRepositoryImpl
import com.thehouse.app.data.remote.api.PropertyApi
import com.thehouse.app.data.remote.api.UniversityApi
import com.thehouse.app.data.serialization.JsonProvider
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Verifica que los repositorios de catálogo emitan los spans NETWORK y MAPPING
 * con el formato `PERF <operation> durationMs=<n>` cuando la instrumentación
 * está habilitada (DEBUG).
 */
class PerfCatalogSpansTest {

    private lateinit var server: MockWebServer
    private lateinit var universities: UniversityRepositoryImpl
    private lateinit var properties: PropertyRepositoryImpl

    private val lines = mutableListOf<String>()
    private val previousEnabled = Perf.enabled
    private val previousLogger = Perf.logger

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()
        val json = JsonProvider.create()
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        universities = UniversityRepositoryImpl(
            api = retrofit.create(UniversityApi::class.java),
        )
        properties = PropertyRepositoryImpl(
            api = retrofit.create(PropertyApi::class.java),
        )
        lines.clear()
        Perf.enabled = true
        Perf.logger = { message -> lines += message }
    }

    @After
    fun tearDown() {
        Perf.enabled = previousEnabled
        Perf.logger = previousLogger
        server.close()
    }

    private fun json(raw: String): MockResponse =
        MockResponse.Builder()
            .code(200)
            .addHeader("Content-Type", "application/json")
            .body(raw)
            .build()

    @Test
    fun `properties emite span de red y de mapping`() = runTest {
        server.enqueue(
            json(
                """{"items":[{"id":"6578a8b8c29c000000000002","title":"Estudio Centro","price":3200,"currency":"MXN","city":"Tulancingo","universityId":"6578a8b8c29c000000000001","bedrooms":1,"bathrooms":1,"areaM2":28.5,"createdAt":"2026-11-05T00:00:00Z"}],"page":1,"pageSize":20,"totalItems":1,"totalPages":1}""",
            ),
        )

        val result = properties.getProperties()

        assertTrue(result.isSuccess)
        assertTrue(
            "falta span NETWORK: $lines",
            lines.any { it.startsWith("PERF network.properties.get durationMs=") },
        )
        assertTrue(
            "falta span MAPPING: $lines",
            lines.any { it.startsWith("PERF mapping.properties.toDomain durationMs=") },
        )
        lines.forEach { line ->
            assertTrue("formato inesperado: $line", PERF_FORMAT.matches(line))
        }
    }

    @Test
    fun `universities emite span de red y de mapping`() = runTest {
        server.enqueue(
            json(
                """{"items":[{"id":"6578a8b8c29c000000000001","code":"UTTT","name":"Universidad Tecnologica de Tulancingo","city":"Tulancingo","country":"MX","createdAt":"2026-11-05T00:00:00Z"}],"page":1,"pageSize":20,"totalItems":1,"totalPages":1}""",
            ),
        )

        val result = universities.getUniversities()

        assertTrue(result.isSuccess)
        assertTrue(
            "falta span NETWORK: $lines",
            lines.any { it.startsWith("PERF network.universities.get durationMs=") },
        )
        assertTrue(
            "falta span MAPPING: $lines",
            lines.any { it.startsWith("PERF mapping.universities.toDomain durationMs=") },
        )
    }

    private companion object {
        val PERF_FORMAT = Regex("""^PERF \S+ durationMs=\d+$""")
    }
}
