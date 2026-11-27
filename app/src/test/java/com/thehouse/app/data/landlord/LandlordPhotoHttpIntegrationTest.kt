package com.thehouse.app.data.landlord

import com.thehouse.app.data.landlord.repository.LandlordRepositoryImpl
import com.thehouse.app.data.remote.api.LandlordApi
import com.thehouse.app.data.serialization.JsonProvider
import com.thehouse.app.domain.error.DomainError
import com.thehouse.app.domain.error.LandlordErrorCode
import com.thehouse.app.domain.error.LandlordException
import java.io.File
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/** Contrato real verificado de fotos (MediaEndpoint.cs / MediaServices.cs, solo lectura). */
class LandlordPhotoHttpIntegrationTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var server: MockWebServer
    private lateinit var repository: LandlordRepositoryImpl

    private val photoJson = """
        {"id":"p1","url":"/api/v1/media/p1","thumbUrl":"/api/v1/media/p1?size=thumb",
         "width":1200,"height":800,"bytes":12345,"createdAt":"2026-11-27T12:00:00Z"}
    """.trimIndent()

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(JsonProvider.create().asConverterFactory("application/json".toMediaType()))
            .build()
        repository = LandlordRepositoryImpl(api = retrofit.create(LandlordApi::class.java), json = JsonProvider.create())
    }

    @After
    fun tearDown() = server.close()

    private fun imageFile(): File {
        val file = tempFolder.newFile("fixture-${System.nanoTime()}.webp")
        file.writeBytes(byteArrayOf(0x52, 0x49, 0x46, 0x46, 0, 0, 0, 0, 0x57, 0x45, 0x42, 0x50))
        return file
    }

    @Test
    fun `room photo upload is multipart field file on real route`() = runTest {
        server.enqueue(MockResponse.Builder().code(201).body(photoJson).build())
        val result = repository.uploadRoomPhoto("r1", imageFile(), "image/webp")
        assertTrue(result.isSuccess)

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/landlord/rooms/r1/photos", request.url.encodedPath)
        assertTrue(request.headers["Content-Type"]!!.startsWith("multipart/form-data"))
        val body = request.body?.utf8().orEmpty()
        assertTrue(body.contains("name=\"file\""))
        assertTrue(body.contains("filename=\"fixture-"))
    }

    @Test
    fun `building photo upload uses PUT on real route`() = runTest {
        server.enqueue(MockResponse.Builder().code(200).body(photoJson).build())
        val result = repository.uploadBuildingPhoto("b1", imageFile(), "image/jpeg")
        assertTrue(result.isSuccess)
        val request = server.takeRequest()
        assertEquals("PUT", request.method)
        assertEquals("/api/v1/landlord/buildings/b1/photo", request.url.encodedPath)
        assertTrue(request.body?.utf8().orEmpty().contains("name=\"file\""))
    }

    @Test
    fun `room photo delete uses photo id and expects 204`() = runTest {
        server.enqueue(MockResponse.Builder().code(204).build())
        val result = repository.deleteRoomPhoto("r1", "p1")
        assertTrue(result.isSuccess)
        val request = server.takeRequest()
        assertEquals("DELETE", request.method)
        assertEquals("/api/v1/landlord/rooms/r1/photos/p1", request.url.encodedPath)
    }

    @Test
    fun `building photo delete expects 204`() = runTest {
        server.enqueue(MockResponse.Builder().code(204).build())
        assertTrue(repository.deleteBuildingPhoto("b1").isSuccess)
        assertEquals("DELETE", server.takeRequest().method)
    }

    @Test
    fun `content rejected maps to moderation code without exposing internals`() = runTest {
        server.enqueue(
            MockResponse.Builder()
                .code(422)
                .body("""{"code":"content_rejected","field":"file","categories":["hate"]}""")
                .build(),
        )
        val result = repository.uploadRoomPhoto("r1", imageFile(), "image/webp")
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull() as LandlordException
        assertEquals(LandlordErrorCode.CONTENT_REJECTED, exception.code)
        assertEquals(DomainError.ValidationError, exception.domainError)
    }

    @Test
    fun `moderation unavailable is retryable and keeps Retry-After`() = runTest {
        server.enqueue(
            MockResponse.Builder()
                .code(503)
                .addHeader("Retry-After", "30")
                .body("""{"code":"moderation_unavailable"}""")
                .build(),
        )
        val result = repository.uploadRoomPhoto("r1", imageFile(), "image/webp")
        val exception = result.exceptionOrNull() as LandlordException
        assertEquals(LandlordErrorCode.MODERATION_UNAVAILABLE, exception.code)
        assertEquals(DomainError.ServiceUnavailable, exception.domainError)
        assertEquals(30L, exception.retryAfterSeconds)
    }

    @Test
    fun `photos limit and invalid file map to contractual codes`() = runTest {
        server.enqueue(MockResponse.Builder().code(409).body("""{"code":"photos_limit"}""").build())
        val limit = repository.uploadRoomPhoto("r1", imageFile(), "image/webp")
        assertEquals(LandlordErrorCode.PHOTOS_LIMIT, (limit.exceptionOrNull() as LandlordException).code)

        server.enqueue(MockResponse.Builder().code(415).body("""{"code":"unsupported_media"}""").build())
        val format = repository.uploadRoomPhoto("r1", imageFile(), "application/pdf")
        assertEquals(LandlordErrorCode.UNSUPPORTED_MEDIA, (format.exceptionOrNull() as LandlordException).code)

        server.enqueue(MockResponse.Builder().code(413).body("""{"code":"payload_too_large"}""").build())
        val size = repository.uploadRoomPhoto("r1", imageFile(), "image/webp")
        assertEquals(LandlordErrorCode.PAYLOAD_TOO_LARGE, (size.exceptionOrNull() as LandlordException).code)
    }

    @Test
    fun `successful upload response is contract shaped photo`() = runTest {
        server.enqueue(MockResponse.Builder().code(201).body(photoJson).build())
        // El repositorio ignora el body, pero Retrofit debe deserializar el DTO real.
        val response = retrofitApi().uploadRoomPhoto(
            "r1",
            okhttp3.MultipartBody.Part.createFormData(
                "file", "fixture.webp", imageFile().asRequestBody("image/webp".toMediaType()),
            ),
        )
        assertTrue(response.isSuccessful)
        assertEquals("p1", response.body()!!.id)
        assertEquals("/api/v1/media/p1?size=thumb", response.body()!!.thumbUrl)
    }

    private fun retrofitApi(): LandlordApi {
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(JsonProvider.create().asConverterFactory("application/json".toMediaType()))
            .build()
        return retrofit.create(LandlordApi::class.java)
    }
}
