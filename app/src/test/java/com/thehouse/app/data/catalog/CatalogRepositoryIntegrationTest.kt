package com.thehouse.app.data.catalog

import com.thehouse.app.data.catalog.repository.PropertyRepositoryImpl
import com.thehouse.app.data.catalog.repository.SelectedUniversityRepositoryImpl
import com.thehouse.app.data.catalog.repository.UniversityRepositoryImpl
import com.thehouse.app.data.preferences.PreferencesStore
import com.thehouse.app.data.serialization.JsonProvider
import com.thehouse.app.domain.error.DomainError
import com.thehouse.app.domain.error.DomainException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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

/** Fake de PreferencesStore no sensible para selección persistida. */
class FakePreferencesStore : PreferencesStore {
    private val strings = mutableMapOf<String, String>()
    private val booleans = mutableMapOf<String, Boolean>()

    override fun observeString(key: String): Flow<String?> =
        MutableStateFlow(strings[key])

    override fun observeBoolean(key: String, defaultValue: Boolean): Flow<Boolean> =
        MutableStateFlow(booleans[key] ?: defaultValue)

    override suspend fun putString(key: String, value: String) {
        strings[key] = value
    }

    override suspend fun putBoolean(key: String, value: Boolean) {
        booleans[key] = value
    }

    override suspend fun remove(key: String) {
        strings.remove(key)
        booleans.remove(key)
    }

    override suspend fun clear() {
        strings.clear()
        booleans.clear()
    }
}

/**
 * Integración D5 NV-06/NV-07 contra MockWebServer:
 * universidades/propiedades reales (AllowAnonymous, paginación, mapeo).
 */
class CatalogRepositoryIntegrationTest {

    private lateinit var server: MockWebServer
    private lateinit var universities: UniversityRepositoryImpl
    private lateinit var properties: PropertyRepositoryImpl

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()
        val ok = OkHttpClient.Builder().build()
        val json = JsonProvider.create()
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(ok)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        universities = UniversityRepositoryImpl(
            api = retrofit.create(com.thehouse.app.data.remote.api.UniversityApi::class.java),
        )
        properties = PropertyRepositoryImpl(
            api = retrofit.create(com.thehouse.app.data.remote.api.PropertyApi::class.java),
            searchApi = retrofit.create(com.thehouse.app.data.remote.api.SearchApi::class.java),
        )
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun body(raw: String): MockResponse =
        MockResponse.Builder()
            .code(200)
            .addHeader("Content-Type", "application/json")
            .body(raw)
            .build()

    @Test
    fun `universities 200 mapea paginacion`() = runTest {
        server.enqueue(
            body(
                """{"items":[{"id":"6578a8b8c29c000000000001","code":"UTTT","name":"Universidad Tecnologica de Tulancingo","city":"Tulancingo","country":"MX","createdAt":"2026-11-05T00:00:00Z"}],"page":1,"pageSize":20,"totalItems":1,"totalPages":1}""",
            ),
        )
        val result = universities.getUniversities()
        assertTrue(result.isSuccess)
        val page = result.getOrNull()
        assertEquals(1, page?.items?.size)
        assertEquals("UTTT", page?.items?.firstOrNull()?.code)
        assertEquals("Tulancingo", page?.items?.firstOrNull()?.city)
        assertEquals(1L, page?.totalItems)
        assertEquals(1, page?.totalPages)
        val req = server.takeRequest()
        assertTrue(req.url.encodedPath.endsWith("/api/v1/universities"))
        assertEquals("1", req.url.queryParameter("page"))
        assertEquals("20", req.url.queryParameter("pageSize"))
    }

    @Test
    fun `universities empty`() = runTest {
        server.enqueue(
            body("""{"items":[],"page":1,"pageSize":20,"totalItems":0,"totalPages":0}"""),
        )
        val result = universities.getUniversities()
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.items?.isEmpty() == true)
    }

    @Test
    fun `universities error 500 mapea a Server`() = runTest {
        server.enqueue(MockResponse.Builder().code(500).body("").build())
        val result = universities.getUniversities()
        assertTrue(result.isFailure)
        assertEquals(DomainError.Server, (result.exceptionOrNull() as DomainException).error)
    }

    @Test
    fun `university detail 200`() = runTest {
        server.enqueue(
            body(
                """{"id":"6578a8b8c29c000000000001","code":"UTTT","name":"Universidad X","city":"Tulancingo","country":"MX","createdAt":"2026-11-05T00:00:00Z"}""",
            ),
        )
        val result = universities.getUniversityById("6578a8b8c29c000000000001")
        assertTrue(result.isSuccess)
        assertEquals("Universidad X", result.getOrNull()?.name)
    }

    @Test
    fun `properties 200 mapea solo campos reales`() = runTest {
        server.enqueue(
            body(
                """{"items":[{"id":"6578a8b8c29c000000000002","title":"Estudio Centro","price":3200,"currency":"MXN","city":"Tulancingo","universityId":"6578a8b8c29c000000000001","bedrooms":1,"bathrooms":1,"areaM2":28.5,"createdAt":"2026-11-05T00:00:00Z"}],"page":1,"pageSize":20,"totalItems":1,"totalPages":1}""",
            ),
        )
        val result = properties.getProperties()
        assertTrue(result.isSuccess)
        val item = result.getOrNull()?.items?.firstOrNull()
        assertEquals("Estudio Centro", item?.title)
        assertEquals(3200.0, item?.price)
        assertEquals("MXN", item?.currency)
        assertEquals("Tulancingo", item?.city)
        assertEquals(1, item?.bedrooms)
        assertEquals(28.5, item?.areaM2)
    }

    @Test
    fun `properties empty`() = runTest {
        server.enqueue(
            body("""{"items":[],"page":1,"pageSize":20,"totalItems":0,"totalPages":0}"""),
        )
        val result = properties.getProperties()
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.items?.isEmpty() == true)
    }

    @Test
    fun `properties error 429 mapea a RateLimited`() = runTest {
        server.enqueue(MockResponse.Builder().code(429).body("").build())
        val result = properties.getProperties()
        assertTrue(result.isFailure)
        assertEquals(DomainError.RateLimited, (result.exceptionOrNull() as DomainException).error)
    }

    @Test
    fun `property detail incluye descripcion y amenities`() = runTest {
        server.enqueue(
            body(
                """{"id":"6578a8b8c29c000000000002","title":"Estudio Centro","description":"Cerca del campus","price":3200,"currency":"MXN","city":"Tulancingo","universityId":null,"bedrooms":1,"bathrooms":1,"areaM2":28.5,"amenities":["wifi","amueblado"],"createdAt":"2026-11-05T00:00:00Z"}""",
            ),
        )
        val result = properties.getPropertyById("6578a8b8c29c000000000002")
        assertTrue(result.isSuccess)
        assertEquals("Cerca del campus", result.getOrNull()?.description)
        assertEquals(listOf("wifi", "amueblado"), result.getOrNull()?.amenities)
        assertEquals(null, result.getOrNull()?.universityId)
    }

    @Test
    fun `seleccion persistida se guarda y recarga`() = runTest {
        val prefs = FakePreferencesStore()
        val repo = SelectedUniversityRepositoryImpl(prefs)
        repo.select("uni-123")
        val repo2 = SelectedUniversityRepositoryImpl(prefs)
        repo2.loadSelected()
        assertEquals("uni-123", repo2.selectedUniversityId.value)
        repo2.clear()
        val repo3 = SelectedUniversityRepositoryImpl(prefs)
        repo3.loadSelected()
        assertEquals(null, repo3.selectedUniversityId.value)
    }
}
