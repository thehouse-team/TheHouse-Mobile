package com.thehouse.app.data.image

import com.thehouse.app.data.remote.dto.room.RoomDetailResponseDto
import com.thehouse.app.data.remote.dto.room.RoomSearchResponseDto
import com.thehouse.app.data.room.toDomain
import com.thehouse.app.data.serialization.JsonProvider
import com.thehouse.app.domain.room.RoomSearchCatalog
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** D21 hotfix: URLs relativas, mappers y ausencia de Bearer en imágenes. */
class D21ImagePipelineTest {

    private val json: Json = JsonProvider.create()
    private val base = "https://ca-thehouse-u2.grayisland-cb722b5f.eastus2.azurecontainerapps.io/"

    @Test
    fun `URL relativa se resuelve contra el origen del backend`() {
        val resolved = ImageUrlResolver.resolve("/api/v1/media/abc?size=thumb", base)
        assertTrue(resolved!!.startsWith("https://"))
        assertTrue(resolved.contains("/api/v1/media/abc?size=thumb"))
    }

    @Test
    fun `URL absoluta no se modifica`() {
        val absolute = "https://cdn.ejemplo.com/foto.webp?sig=abc"
        assertEquals(absolute, ImageUrlResolver.resolve(absolute, base))
    }

    @Test
    fun `null y blank no producen imagen`() {
        assertNull(ImageUrlResolver.resolve(null, base))
        assertNull(ImageUrlResolver.resolve("   ", base))
        assertNull(ImageUrlResolver.resolve("foto.webp", base))
    }

    @Test
    fun `thumbnailUrl se deserializa y el mapper lo conserva`() {
        val payload = """
            {"items":[{"id":"r1","buildingId":"b1","title":"[PRUEBA Mobile] Cuarto",
              "price":3200,"currency":"MXN","roomType":"single","bathroom":"private",
              "buildingType":"house_rooms","allowedTenants":"women","services":["wifi"],
              "distanceKm":1.2,"walkingMinutes":15,"approxLocation":{"lat":19.5,"lng":-99.3},
              "thumbnailUrl":"/api/v1/media/m1?size=thumb","demo":false}],
             "page":1,"pageSize":20,"totalItems":1,"totalPages":1}
        """.trimIndent()
        val dto = json.decodeFromString<RoomSearchResponseDto>(payload)
        val item = dto.toDomain().items.single()
        assertEquals("/api/v1/media/m1?size=thumb", item.thumbnailUrl)
        assertFalse(item.demo)
        assertEquals("Casa con habitaciones", RoomSearchCatalog.label(RoomSearchCatalog.Kind.BUILDING_TYPE, item.buildingType))
        assertEquals("Solo mujeres", RoomSearchCatalog.label(RoomSearchCatalog.Kind.ALLOWED_TENANTS, item.allowedTenants))
    }

    @Test
    fun `respuesta con thumbnail null conserva placeholder`() {
        val payload = """
            {"items":[{"id":"r2","buildingId":"b1","title":"Demo",
              "price":3200,"currency":"MXN","buildingType":"rooms_building",
              "allowedTenants":"any","distanceKm":1.2,"walkingMinutes":15,
              "approxLocation":{"lat":19.5,"lng":-99.3},"thumbnailUrl":null,"demo":true}],
             "page":1,"pageSize":20,"totalItems":1,"totalPages":1}
        """.trimIndent()
        val item = json.decodeFromString<RoomSearchResponseDto>(payload).toDomain().items.single()
        assertNull(item.thumbnailUrl)
        assertTrue(item.demo)
    }

    @Test
    fun `mapper de detalle conserva photos y thumbnail`() {
        val payload = """
            {"id":"r3","buildingId":"b1","title":"Cuarto","price":3200,"currency":"MXN",
             "roomType":"double","bathroom":"shared","distanceKm":1.2,"walkingMinutes":15,
             "approxLocation":{"lat":19.5,"lng":-99.3},
             "building":{"id":"b1","name":"Casa","type":"house_rooms","allowedTenants":"any","services":[]},
             "thumbnailUrl":"/api/v1/media/m1?size=thumb","demo":false,
             "photos":[{"id":"p1","url":"/api/v1/media/p1","thumbUrl":"/api/v1/media/p1?size=thumb",
                        "width":800,"height":600,"bytes":1234,"createdAt":"2026-01-01T00:00:00Z"}]}
        """.trimIndent()
        val detail = json.decodeFromString<RoomDetailResponseDto>(payload).toDomain()
        assertEquals("/api/v1/media/m1?size=thumb", detail.thumbnailUrl)
        assertEquals(1, detail.photos.size)
        assertEquals("/api/v1/media/p1", detail.photos.first().url)
        assertEquals("Doble", RoomSearchCatalog.label(RoomSearchCatalog.Kind.ROOM_TYPE, detail.roomType!!))
    }

    @Test
    fun `cliente de imagenes no envia Bearer a dominio externo`() {
        val server = MockWebServer()
        server.start()
        server.enqueue(
            MockResponse.Builder()
                .code(200)
                .addHeader("Content-Type", "image/webp")
                .body("RIFFfake-webp")
                .build(),
        )
        val url = server.url("/foto.webp")
        val client = ImageHttpClientFactory.create()
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        response.use {
            assertEquals(200, it.code)
            assertEquals("image/webp", it.header("Content-Type"))
        }
        val recorded = server.takeRequest()
        assertNull(recorded.headers["Authorization"])
        assertTrue(recorded.target.contains("foto.webp"))
        server.close()
    }

    @Test
    fun `catalogo usa etiquetas en espanol solicitadas`() {
        assertEquals("Individual", RoomSearchCatalog.label(RoomSearchCatalog.Kind.ROOM_TYPE, "single"))
        assertEquals("Doble", RoomSearchCatalog.label(RoomSearchCatalog.Kind.ROOM_TYPE, "double"))
        assertEquals("Compartido o Compartida", RoomSearchCatalog.label(RoomSearchCatalog.Kind.ROOM_TYPE, "shared"))
        assertEquals("Casa con habitaciones", RoomSearchCatalog.label(RoomSearchCatalog.Kind.BUILDING_TYPE, "house_rooms"))
        assertEquals("Cuartos en edificio", RoomSearchCatalog.label(RoomSearchCatalog.Kind.BUILDING_TYPE, "rooms_building"))
        assertEquals("Departamento completo", RoomSearchCatalog.label(RoomSearchCatalog.Kind.BUILDING_TYPE, "full_apartment"))
        assertEquals("Solo hombres", RoomSearchCatalog.label(RoomSearchCatalog.Kind.ALLOWED_TENANTS, "men"))
        assertEquals("Cualquiera", RoomSearchCatalog.label(RoomSearchCatalog.Kind.ALLOWED_TENANTS, "any"))
        assertEquals("Privado", RoomSearchCatalog.label(RoomSearchCatalog.Kind.BATHROOM, "private"))
    }
}
