package com.thehouse.app.data.cache

import com.thehouse.app.data.account.repository.LocalUserDataPurgerImpl
import com.thehouse.app.data.auth.repository.OnboardingPreferencesRepository
import com.thehouse.app.data.catalog.repository.PropertyRepositoryImpl
import com.thehouse.app.data.catalog.repository.UniversityRepositoryImpl
import com.thehouse.app.data.favorites.repository.FavoritesRepositoryImpl
import com.thehouse.app.data.favorites.state.FavoriteIdsStore
import com.thehouse.app.data.notifications.push.PendingNotificationNavigation
import com.thehouse.app.data.preferences.PreferencesStore
import com.thehouse.app.data.repository.PreferenceRepositoryImpl
import com.thehouse.app.data.repository.ProfileRepositoryImpl
import com.thehouse.app.data.serialization.JsonProvider
import com.thehouse.app.domain.auth.LocalSessionCleaner
import com.thehouse.app.domain.common.DispatcherProvider
import com.thehouse.app.domain.error.DomainError
import com.thehouse.app.domain.error.DomainException
import com.thehouse.app.domain.push.LocalPushNotifier
import com.thehouse.app.domain.push.PushPayload
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class FakePreferencesStore : PreferencesStore {
    private val strings = mutableMapOf<String, MutableStateFlow<String?>>()
    private val booleans = mutableMapOf<String, MutableStateFlow<Boolean>>()

    override fun observeString(key: String): Flow<String?> = strings.getOrPut(key) { MutableStateFlow(null) }
    override fun observeBoolean(key: String, defaultValue: Boolean): Flow<Boolean> =
        booleans.getOrPut(key) { MutableStateFlow(defaultValue) }

    override suspend fun putString(key: String, value: String) { strings.getOrPut(key) { MutableStateFlow(null) }.value = value }
    override suspend fun putBoolean(key: String, value: Boolean) { booleans.getOrPut(key) { MutableStateFlow(value) }.value = value }
    override suspend fun remove(key: String) { strings[key]?.value = null; booleans.remove(key) }
    override suspend fun clear() { strings.values.forEach { it.value = null }; booleans.clear() }

    fun dumpKeys(): Set<String> = strings.filterValues { it.value != null }.keys + booleans.keys
}

class OfflineCacheRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var retrofit: Retrofit
    private lateinit var prefs: FakePreferencesStore
    private lateinit var cache: OfflineCacheStore

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()
        val json = JsonProvider.create()
        val ok = OkHttpClient.Builder().build()
        retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(ok)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        prefs = FakePreferencesStore()
        cache = OfflineCacheStore(prefs, json)
    }

    @After fun tearDown() { server.close() }

    private fun jsonBody(raw: String, code: Int = 200): MockResponse =
        MockResponse.Builder().code(code).addHeader("Content-Type", "application/json").body(raw).build()

    private fun disconnect(): MockResponse = MockResponse.Builder().code(503).body("").build()

    @Test
    fun `property list network success updates cache and server error uses cache`() = runTest {
        val repo = PropertyRepositoryImpl(retrofit.create(com.thehouse.app.data.remote.api.PropertyApi::class.java), cache)
        server.enqueue(jsonBody("""{"items":[{"id":"p1","title":"Casa","price":1000.0,"currency":"MXN","city":"Tul","universityId":null,"bedrooms":1,"bathrooms":1,"areaM2":20.0,"createdAt":"2026"}],"page":1,"pageSize":20,"totalItems":1,"totalPages":1}"""))
        val ok = repo.getProperties(1, 20)
        assertTrue(ok.isSuccess)
        server.enqueue(MockResponse.Builder().code(500).body("").build())
        val fallback = repo.getProperties(1, 20)
        assertTrue(fallback.isSuccess)
        assertEquals("p1", fallback.getOrNull()?.items?.first()?.id)
    }

    @Test
    fun `property list cache miss http failure returns error`() = runTest {
        val repo = PropertyRepositoryImpl(retrofit.create(com.thehouse.app.data.remote.api.PropertyApi::class.java), cache)
        server.enqueue(MockResponse.Builder().code(500).body("").build())
        val res = repo.getProperties(1, 20)
        assertTrue(res.isFailure)
        assertEquals(DomainError.Server, (res.exceptionOrNull() as DomainException).error)
    }

    @Test
    fun `property detail refresh replaces stale cache`() = runTest {
        val repo = PropertyRepositoryImpl(retrofit.create(com.thehouse.app.data.remote.api.PropertyApi::class.java), cache)
        server.enqueue(jsonBody("""{"id":"p1","title":"Viejo","description":"d","price":1.0,"currency":"MXN","city":"C","universityId":null,"bedrooms":1,"bathrooms":1,"areaM2":1.0,"amenities":[],"createdAt":"2026"}"""))
        assertEquals("Viejo", repo.getPropertyById("p1").getOrNull()?.title)
        server.enqueue(jsonBody("""{"id":"p1","title":"Nuevo","description":"d2","price":2.0,"currency":"MXN","city":"C","universityId":null,"bedrooms":1,"bathrooms":1,"areaM2":1.0,"amenities":[],"createdAt":"2026"}"""))
        assertEquals("Nuevo", repo.getPropertyById("p1").getOrNull()?.title)
        server.enqueue(MockResponse.Builder().code(500).body("").build())
        assertEquals("Nuevo", repo.getPropertyById("p1").getOrNull()?.title)
    }

    @Test
    fun `universities fallback uses cache`() = runTest {
        val repo = UniversityRepositoryImpl(retrofit.create(com.thehouse.app.data.remote.api.UniversityApi::class.java), cache)
        server.enqueue(jsonBody("""{"items":[{"id":"u1","code":"UTTT","name":"Uni","city":"Tul","country":"MX","createdAt":"2026"}],"page":1,"pageSize":20,"totalItems":1,"totalPages":1}"""))
        assertTrue(repo.getUniversities(1, 20).isSuccess)
        server.enqueue(MockResponse.Builder().code(500).body("").build())
        val offline = repo.getUniversities(1, 20)
        assertTrue(offline.isSuccess)
        assertEquals("UTTT", offline.getOrNull()?.items?.first()?.code)
    }

    @Test
    fun `favorites cached read is user scoped`() = runTest {
        val favApi = retrofit.create(com.thehouse.app.data.remote.api.FavoritesApi::class.java)
        var user: String? = "A"
        val repo = FavoritesRepositoryImpl(favApi, cache) { user }
        server.enqueue(jsonBody("""{"items":[{"propertyId":"p1","title":"Fav"}],"page":1,"pageSize":20,"totalItems":1,"totalPages":1}"""))
        assertTrue(repo.getFavorites(1, 20).isSuccess)
        server.enqueue(MockResponse.Builder().code(500).body("").build())
        assertEquals(1, repo.getFavorites(1, 20).getOrNull()?.items?.size)
        OfflineCacheStore.Keys.privateKeysFor("A").forEach { cache.remove(it) }
        user = "B"
        server.enqueue(MockResponse.Builder().code(500).body("").build())
        assertTrue(repo.getFavorites(1, 20).isFailure)
    }

    @Test
    fun `profile and preferences cache keys are user isolated`() = runTest {
        cache.put(OfflineCacheStore.Keys.profile("A"), ProfileSnapshot(name = "Ana", university = null, quickSetupCompleted = false), ProfileSnapshot.serializer(), userScope = "A")
        cache.put(OfflineCacheStore.Keys.preferences("A"), PreferencesSnapshot(placeType = "room"), PreferencesSnapshot.serializer(), userScope = "A")
        cache.put(OfflineCacheStore.Keys.profile("B"), ProfileSnapshot(name = "Beto", university = null, quickSetupCompleted = true), ProfileSnapshot.serializer(), userScope = "B")

        assertEquals("Ana", cache.get(OfflineCacheStore.Keys.profile("A"), ProfileSnapshot.serializer())?.payload?.name)
        assertEquals("room", cache.get(OfflineCacheStore.Keys.preferences("A"), PreferencesSnapshot.serializer())?.payload?.placeType)
        assertEquals("Beto", cache.get(OfflineCacheStore.Keys.profile("B"), ProfileSnapshot.serializer())?.payload?.name)

        OfflineCacheStore.Keys.privateKeysFor("A").forEach { cache.remove(it) }
        assertNull(cache.get(OfflineCacheStore.Keys.profile("A"), ProfileSnapshot.serializer()))
        assertNull(cache.get(OfflineCacheStore.Keys.preferences("A"), PreferencesSnapshot.serializer()))
        assertEquals("Beto", cache.get(OfflineCacheStore.Keys.profile("B"), ProfileSnapshot.serializer())?.payload?.name)
    }

    @Test
    fun `purger clears private user cache and no sensitive keys`() = runTest {
        val userId = "U1"
        cache.put(OfflineCacheStore.Keys.profile(userId), ProfileSnapshot(name = "N", university = null, quickSetupCompleted = false), ProfileSnapshot.serializer(), userScope = userId)
        assertNotNull(cache.get(OfflineCacheStore.Keys.profile(userId), ProfileSnapshot.serializer()))

        val purger = LocalUserDataPurgerImpl(
            onboardingRepository = OnboardingPreferencesRepository(prefs),
            favoritesStore = FavoriteIdsStore(),
            pendingNotificationNavigation = PendingNotificationNavigation(),
            localPushNotifier = object : LocalPushNotifier {
                override fun show(payload: PushPayload) {}
                override fun cancelAll() {}
            },
            httpCache = null,
            localSessionCleaner = object : LocalSessionCleaner { override suspend fun clearLocalSession() {} },
            dispatchers = object : DispatcherProvider {
                override val io: CoroutineDispatcher = Dispatchers.Unconfined
                override val default: CoroutineDispatcher = Dispatchers.Unconfined
            },
            offlineCacheStore = cache,
        )
        purger.purge(userId)
        assertNull(cache.get(OfflineCacheStore.Keys.profile(userId), ProfileSnapshot.serializer()))

        val all = prefs.dumpKeys().joinToString("|").lowercase()
        assertTrue(!all.contains("jwt"))
        assertTrue(!all.contains("fcm"))
        assertTrue(!all.contains("qrtoken"))
        assertTrue(!all.contains("agreement"))
    }
}