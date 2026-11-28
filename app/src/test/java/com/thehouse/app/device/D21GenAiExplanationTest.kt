package com.thehouse.app.device

import com.thehouse.app.data.remote.api.SmartMatchApi
import com.thehouse.app.data.remote.dto.smartmatch.SmartMatchExplanationQueryRequest
import com.thehouse.app.data.remote.dto.smartmatch.SmartMatchExplanationResponse
import com.thehouse.app.data.serialization.JsonProvider
import com.thehouse.app.data.smartmatch.SmartMatchRepository
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

/** D21: contrato REAL de explicaciones GenAI/template, sin llamar a GenAI directamente. */
class D21GenAiExplanationTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: SmartMatchRepository

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(JsonProvider.create().asConverterFactory("application/json".toMediaType()))
            .build()
        repository = SmartMatchRepository(retrofit.create(SmartMatchApi::class.java))
    }

    @After
    fun tearDown() { server.close() }

    private fun response(generatedBy: String) = SmartMatchExplanationResponse(
        roomId = "room-123",
        score = 78.0,
        summary = "Resumen de prueba seguro",
        pros = listOf("Cerca de la universidad"),
        cons = listOf("Sin lavadora"),
        generatedBy = generatedBy,
        modelVersion = "logreg-1.0.0",
        explanationVersion = "explanation-1.0.0",
    )

    @Test
    fun guest_query_hitsContractualPostEndpoint() = runTest {
        val json = JsonProvider.create()
        server.enqueue(
            MockResponse.Builder()
                .code(200)
                .body(json.encodeToString(SmartMatchExplanationResponse.serializer(), response("genai")))
                .build(),
        )
        val result = repository.queryExplanation(
            SmartMatchExplanationQueryRequest(roomId = "room-123", universityId = null, preferences = null),
        )
        assertEquals("genai", result.generatedBy)
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/smart-match/explanations/query", request.target)
    }

    @Test
    fun template_explanation_isHonestlyLabelled() = runTest {
        val json = JsonProvider.create()
        server.enqueue(
            MockResponse.Builder()
                .code(200)
                .body(json.encodeToString(SmartMatchExplanationResponse.serializer(), response("template")))
                .build(),
        )
        val result = repository.queryExplanation(
            SmartMatchExplanationQueryRequest(roomId = "room-123", universityId = null, preferences = null),
        )
        assertEquals("template", result.generatedBy)
        assertTrue(result.pros.size == 1)
        assertEquals("explanation-1.0.0", result.explanationVersion)
    }
}
