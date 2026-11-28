package com.thehouse.app.data.catalog

import com.thehouse.app.data.catalog.repository.UniversityRepositoryImpl
import com.thehouse.app.data.remote.api.UniversityApi
import com.thehouse.app.data.remote.dto.catalog.NearbyUniversityResponseDto
import com.thehouse.app.data.remote.dto.catalog.UniversityDto
import com.thehouse.app.data.remote.dto.catalog.UniversityListResponseDto
import com.thehouse.app.data.serialization.JsonProvider
import com.thehouse.app.domain.error.DomainError
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
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/** D21 hotfix: catálogo sin red nunca lanza y nunca limpia estado de sesión. */
class D21CatalogOfflineRegressionTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: UniversityRepositoryImpl

    private class ThrowingApi(private val error: Exception) : UniversityApi {
        override suspend fun getUniversities(page: Int, pageSize: Int): Response<UniversityListResponseDto> = throw error
        override suspend fun getUniversityById(id: String): Response<UniversityDto> = throw error
        override suspend fun searchNearbyUniversities(lat: Double, lng: Double): Response<NearbyUniversityResponseDto> = throw error
    }

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
    fun `socket timeout on universities returns typed timeout failure`() = runTest {
        val repo = UniversityRepositoryImpl(api = ThrowingApi(SocketTimeoutException("timeout")))
        val result = repo.getUniversities()
        assertTrue(result.isFailure)
        assertEquals(DomainError.Timeout, (result.exceptionOrNull() as DomainException).error)
    }

    @Test
    fun `unknown host on nearby search returns typed network failure`() = runTest {
        val repo = UniversityRepositoryImpl(api = ThrowingApi(UnknownHostException("DNS")))
        val result = repo.searchNearbyUniversities(19.9185, -99.3645)
        assertTrue(result.isFailure)
        assertEquals(DomainError.Network, (result.exceptionOrNull() as DomainException).error)
    }

    @Test
    fun `http 503 returns failure and keeps coordinates usable for retry`() = runTest {
        server.enqueue(MockResponse.Builder().code(503).body("""{"code":"service_unavailable"}""").build())
        val result = repository.searchNearbyUniversities(19.9185, -99.3645)
        assertTrue(result.isFailure)
        // Sin fallback inventado: no hay datos que el ViewModel pueda mostrar como reales.
        assertTrue(result.getOrNull() == null)
    }

    @Test
    fun `successful retry after failure works without duplicated state`() = runTest {
        server.enqueue(MockResponse.Builder().code(503).body("""{"code":"service_unavailable"}""").build())
        assertTrue(repository.searchNearbyUniversities(19.9185, -99.3645).isFailure)
        server.enqueue(
            MockResponse.Builder()
                .code(200)
                .body("""{"items":[{"id":"u1","code":"UAEH","name":"UAEH","city":"Pachuca","distanceKm":0.4}]}""")
                .build(),
        )
        val retry = repository.searchNearbyUniversities(19.9185, -99.3645)
        assertTrue(retry.isSuccess)
        assertEquals("u1", retry.getOrThrow().single().id)
        assertEquals(2, server.requestCount)
    }
}
