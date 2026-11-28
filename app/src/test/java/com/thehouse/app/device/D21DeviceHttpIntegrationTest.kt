package com.thehouse.app.device

import com.thehouse.app.data.device.DeviceRepositoryImpl
import com.thehouse.app.data.remote.api.DeviceApi
import com.thehouse.app.data.serialization.JsonProvider
import com.thehouse.app.domain.device.DeviceError
import com.thehouse.app.domain.device.DeviceRegistrationResult
import com.thehouse.app.domain.device.InstallationId
import com.thehouse.app.domain.repository.DeviceException
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * D21: pruebas REALES de transporte HTTP. Usan [DeviceApi] de producción
 * (sin fakes ni métodos default) + Retrofit + MockWebServer.
 */
class D21DeviceHttpIntegrationTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: DeviceRepositoryImpl
    private val installationId = InstallationId("6aef1234-1111-2222-3333-444455556666")

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(JsonProvider.create().asConverterFactory("application/json".toMediaType()))
            .build()
        repository = DeviceRepositoryImpl(retrofit.create(DeviceApi::class.java))
    }

    @After
    fun tearDown() { server.close() }

    private fun enqueue(code: Int, body: String = "") {
        server.enqueue(MockResponse.Builder().code(code).body(body).build())
    }

    @Test
    fun put_device_201_emitsRealHttpWithContractualBody() = runTest {
        enqueue(
            201,
            """{"installationId":"${installationId.value}","platform":"android","appVersion":"1.1.1","registeredAt":"2026-11-27T00:00:00Z","updatedAt":"2026-11-27T00:00:00Z"}""",
        )
        val result = repository.register(
            installationId = installationId,
            fcmToken = "fake-token-for-http-test-0123456789",
            appVersion = "1.1.1",
            locale = "es-MX",
        )
        assertTrue(result.isSuccess)
        assertEquals(DeviceRegistrationResult.Created, result.getOrNull())

        val request = server.takeRequest()
        assertEquals("PUT", request.method)
        assertEquals("/api/v1/users/me/devices/${installationId.value}", request.target)
        val body = request.body?.utf8().orEmpty()
        assertTrue(body.contains("\"fcmToken\""))
        assertTrue(body.contains("\"platform\":\"android\""))
        assertTrue(body.contains("\"appVersion\":\"1.1.1\""))
        assertTrue(body.contains("\"locale\":\"es-MX\""))
        assertFalse(body.contains("userId"))
    }

    private fun assertFalse(value: Boolean) { assertTrue(!value) }

    @Test
    fun put_device_200_isUpdate() = runTest {
        enqueue(
            200,
            """{"installationId":"${installationId.value}","platform":"android","registeredAt":"2026-11-27T00:00:00Z","updatedAt":"2026-11-27T00:01:00Z"}""",
        )
        val result = repository.register(
            installationId, fcmToken = "fake-token-for-http-test-0123456789",
            appVersion = null, locale = "es-MX",
        )
        assertEquals(DeviceRegistrationResult.Updated, result.getOrNull())
    }

    @Test
    fun put_device_409_isFailure_notSuccess() = runTest {
        enqueue(409, """{"status":409,"code":"installation_conflict"}""")
        val result = repository.register(
            installationId, fcmToken = "fake-token-for-http-test-0123456789",
            appVersion = null, locale = "es-MX",
        )
        assertTrue(result.isFailure)
        assertEquals(DeviceError.Conflict, (result.exceptionOrNull() as DeviceException).reason)
    }

    @Test
    fun put_device_404_forInvalidGuid_isFailure() = runTest {
        enqueue(404, """{"status":404,"detail":"El dispositivo no existe."}""")
        val result = repository.register(
            installationId, fcmToken = "fake-token-for-http-test-0123456789",
            appVersion = null, locale = "es-MX",
        )
        assertTrue(result.isFailure)
        assertEquals(DeviceError.NotFound, (result.exceptionOrNull() as DeviceException).reason)
    }

    @Test
    fun delete_device_204_isSuccess() = runTest {
        enqueue(204)
        val result = repository.unregister(installationId)
        assertTrue(result.isSuccess)
        val request = server.takeRequest()
        assertEquals("DELETE", request.method)
        assertEquals("/api/v1/users/me/devices/${installationId.value}", request.target)
    }

    @Test
    fun delete_device_404_isTreatedAsAlreadyAbsent() = runTest {
        enqueue(404, """{"status":404}""")
        assertTrue(repository.unregister(installationId).isSuccess)
    }

    @Test
    fun delete_device_401_isFailure() = runTest {
        enqueue(401, """{"status":401}""")
        val result = repository.unregister(installationId)
        assertTrue(result.isFailure)
        assertEquals(DeviceError.Unauthorized, (result.exceptionOrNull() as DeviceException).reason)
    }

    @Test
    fun get_devices_200_mapsListWithoutTokens() = runTest {
        enqueue(
            200,
            """{"items":[{"installationId":"${installationId.value}","platform":"android","appVersion":"1.1.1","registeredAt":"2026-11-27T00:00:00Z","updatedAt":"2026-11-27T00:00:00Z"}]}""",
        )
        val result = repository.list()
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()!!.size)
        assertEquals("android", result.getOrNull()!![0].platform)
        assertEquals("GET", server.takeRequest().method)
    }

    @Test
    fun put_device_networkFailure_isNotReportedAsSuccess() = runTest {
        // Servidor auxiliar cerrado: la petición falla a nivel de conexión.
        val closed = MockWebServer().apply { start(); close() }
        val retrofit = Retrofit.Builder()
            .baseUrl(closed.url("/"))
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(JsonProvider.create().asConverterFactory("application/json".toMediaType()))
            .build()
        val failingRepository = DeviceRepositoryImpl(retrofit.create(DeviceApi::class.java))
        val result = failingRepository.register(
            installationId, fcmToken = "fake-token-for-http-test-0123456789",
            appVersion = null, locale = "es-MX",
        )
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is DeviceException)
    }
}
