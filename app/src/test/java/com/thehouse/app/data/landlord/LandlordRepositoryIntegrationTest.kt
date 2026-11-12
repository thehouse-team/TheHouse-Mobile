package com.thehouse.app.data.landlord

import com.thehouse.app.data.landlord.repository.LandlordRepositoryImpl
import com.thehouse.app.data.remote.api.LandlordApi
import com.thehouse.app.data.serialization.JsonProvider
import com.thehouse.app.domain.error.DomainError
import com.thehouse.app.domain.error.LandlordErrorCode
import com.thehouse.app.domain.error.LandlordException
import com.thehouse.app.domain.model.AllowedTenants
import com.thehouse.app.domain.model.BuildingAddress
import com.thehouse.app.domain.model.BuildingLocation
import com.thehouse.app.domain.model.BuildingPatch
import com.thehouse.app.domain.model.BuildingStatus
import com.thehouse.app.domain.model.BuildingType
import com.thehouse.app.domain.model.BathroomType
import com.thehouse.app.domain.model.MinContract
import com.thehouse.app.domain.model.RentalMode
import com.thehouse.app.domain.model.RoomPatch
import com.thehouse.app.domain.model.RoomStatus
import com.thehouse.app.domain.model.RoomType
import com.thehouse.app.domain.model.Service
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Pruebas de integración del contrato REAL de NV-12 (Backend §6.1).
 * MockWebServer; sin endpoints inventados.
 */
class LandlordRepositoryIntegrationTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: LandlordRepositoryImpl

    private val buildingJson = """
        {
          "id": "b1",
          "type": "rooms_building",
          "name": "Edificio Centro",
          "address": {"street":"Av Juárez","number":"12","neighborhood":"Centro","city":"Tulancingo","state":"Hidalgo","postalCode":"43600"},
          "location": {"lat":20.1,"lng":-98.2},
          "nearestUniversity": {"id":"u1","code":"UH","name":"Universidad"},
          "distanceToUniversityKm": 1.4,
          "walkingMinutes": 17,
          "allowedTenants": "any",
          "services": ["wifi","water"],
          "rules": "Silencio después de las 11",
          "status": "draft",
          "roomsTotal": 2,
          "roomsAvailable": 2,
          "version": 3,
          "createdAt": "2026-11-10T12:00:00+00:00",
          "updatedAt": "2026-11-11T12:00:00+00:00"
        }
    """.trimIndent()

    private val roomJson = """
        {
          "id": "r1",
          "buildingId": "b1",
          "number": "101",
          "floor": 1,
          "price": 3500,
          "currency": "MXN",
          "sizeM2": 12,
          "type": "single",
          "bathroom": "shared",
          "furniture": ["bed","desk"],
          "availableFrom": "2026-12-01",
          "minContract": "six_months",
          "status": "ready",
          "version": 2,
          "createdAt": "2026-11-10T12:00:00+00:00",
          "updatedAt": "2026-11-11T12:00:00+00:00"
        }
    """.trimIndent()

    private fun problem(code: String, status: Int) =
        """{"type":"about:blank","title":"Conflict","status":$status,"code":"$code"}"""

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
        repository = LandlordRepositoryImpl(
            api = retrofit.create(LandlordApi::class.java),
            json = json,
        )
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun jsonResponse(code: Int, raw: String): MockResponse =
        MockResponse.Builder()
            .code(code)
            .addHeader("Content-Type", "application/json")
            .body(raw)
            .build()

    // ===== LANDLORD =====

    @Test
    fun `profile create 201 maps rental mode`() = runTest {
        server.enqueue(
            jsonResponse(
                201,
                """{"fullName":"Ana","phone":"+527711234567","rentalMode":"agency",
                   "rfc":"ABC123456XY1","acceptedTermsVersion":"2026-11",
                   "createdAt":"2026-11-10T12:00:00+00:00"}""",
            ),
        )
        val profile = repository.createProfile(
            "Ana",
            "+527711234567",
            RentalMode.AGENCY,
            "ABC123456XY1",
            "2026-11",
        ).getOrThrow()
        assertEquals(RentalMode.AGENCY, profile.rentalMode)
        val recorded = server.takeRequest()
        assertEquals("/api/v1/landlord/profile", recorded.url.encodedPath)
        assertEquals("2026-11", Json.parseToJsonElement(recorded.body?.utf8() ?: "")
            .jsonObject["acceptedTermsVersion"]?.let { (it as JsonPrimitive).content })
    }

    @Test
    fun `profile 404 returns null`() = runTest {
        server.enqueue(jsonResponse(404, problem("not_found", 404)))
        val profile = repository.getProfile().getOrThrow()
        assertNull(profile)
    }

    @Test
    fun `landlord_required 403 on buildings list`() = runTest {
        server.enqueue(jsonResponse(403, problem("landlord_required", 403)))
        val failure = repository.getBuildings().exceptionOrNull() as LandlordException
        assertEquals(DomainError.Unauthorized, failure.domainError)
        assertEquals(LandlordErrorCode.LANDLORD_REQUIRED, failure.code)
    }

    // ===== BUILDINGS =====

    @Test
    fun `buildings list with summary`() = runTest {
        server.enqueue(
            jsonResponse(
                200,
                """{"items":[$buildingJson],"page":1,"pageSize":20,"totalItems":1,"totalPages":1,
                   "summary":{"buildings":1,"rooms":2,"available":2,"occupancyPercent":0.0,
                   "monthlyIncome":0.0}}""",
            ),
        )
        val page = repository.getBuildings().getOrThrow()
        assertEquals(1, page.items.size)
        assertEquals(BuildingStatus.DRAFT, page.items.first().status)
        assertEquals(3, page.items.first().version)
        assertEquals(2, page.summary?.rooms)
    }

    @Test
    fun `building create sends exact fields`() = runTest {
        server.enqueue(jsonResponse(201, buildingJson))
        val building = repository.createBuilding(
            type = BuildingType.ROOMS_BUILDING,
            name = "Edificio Centro",
            address = BuildingAddress("Av Juárez", "12", "Centro", "Tulancingo", "Hidalgo", "43600"),
            location = BuildingLocation(20.1, -98.2),
            nearestUniversityId = "u1",
            allowedTenants = AllowedTenants.ANY,
            services = listOf(Service.WIFI, Service.WATER),
            rules = "Silencio",
        ).getOrThrow()
        assertEquals("b1", building.id)
        val body = Json.parseToJsonElement(server.takeRequest().body?.utf8() ?: "").jsonObject
        assertTrue(body.containsKey("type") && body.containsKey("nearestUniversityId"))
        assertTrue(!body.containsKey("status") && !body.containsKey("version"))
    }

    @Test
    fun `building update sends version in body`() = runTest {
        server.enqueue(jsonResponse(200, buildingJson))
        val building = repository.updateBuilding(
            id = "b1",
            version = 2,
            patch = BuildingPatch(name = "Nuevo nombre"),
        ).getOrThrow()
        assertEquals("b1", building.id)
        val recorded = server.takeRequest()
        assertEquals("PATCH", recorded.method)
        assertEquals("/api/v1/landlord/buildings/b1", recorded.url.encodedPath)
        val body = Json.parseToJsonElement(recorded.body?.utf8() ?: "").jsonObject
        assertEquals(2, (body["version"] as JsonPrimitive).content.toInt())
        assertEquals("Nuevo nombre", (body["name"] as JsonPrimitive).content)
    }

    @Test
    fun `building update 200 refreshes version`() = runTest {
        server.enqueue(jsonResponse(200, buildingJson))
        val building = repository.updateBuilding(
            "b1",
            2,
            BuildingPatch(name = "x"),
        ).getOrThrow()
        assertEquals(3, building.version)
    }

    @Test
    fun `building delete sends version in query`() = runTest {
        server.enqueue(MockResponse.Builder().code(204).build())
        val result = repository.deleteBuilding("b1", 3)
        assertTrue(result.isSuccess)
        val recorded = server.takeRequest()
        assertEquals("DELETE", recorded.method)
        assertEquals("3", recorded.url.queryParameter("version"))
    }

    @Test
    fun `version_conflict 409 maps correctly`() = runTest {
        server.enqueue(jsonResponse(409, problem("version_conflict", 409)))
        val failure = repository.updateBuilding("b1", 2, BuildingPatch(name = "x"))
            .exceptionOrNull() as LandlordException
        assertEquals(DomainError.Conflict, failure.domainError)
        assertEquals(LandlordErrorCode.VERSION_CONFLICT, failure.code)
    }

    @Test
    fun `publish 202 returns in_review`() = runTest {
        server.enqueue(jsonResponse(202, buildingJson.replace("\"draft\"", "\"in_review\"")))
        val building = repository.publishBuilding("b1").getOrThrow()
        assertEquals(BuildingStatus.IN_REVIEW, building.status)
        assertEquals("POST", server.takeRequest().method)
    }

    @Test
    fun `publication steps map`() = runTest {
        server.enqueue(
            jsonResponse(
                200,
                """{"canPublish":true,"steps":[{"id":"building_data","done":true},
                   {"id":"room_ready","done":true}]}""",
            ),
        )
        val publication = repository.getPublication("b1").getOrThrow()
        assertTrue(publication.canPublish)
        assertEquals(2, publication.steps.size)
        assertTrue(publication.steps.first().done)
    }

    @Test
    fun `publication_incomplete 409 maps code`() = runTest {
        server.enqueue(jsonResponse(409, problem("publication_incomplete", 409)))
        val failure = repository.publishBuilding("b1").exceptionOrNull() as LandlordException
        assertEquals(LandlordErrorCode.PUBLICATION_INCOMPLETE, failure.code)
    }

    @Test
    fun `buildings_limit 409 maps code`() = runTest {
        server.enqueue(jsonResponse(409, problem("buildings_limit", 409)))
        val failure = repository.createBuilding(
            BuildingType.ROOMS_BUILDING, "x",
            BuildingAddress("s", "1", "n", "c", "e", "12345"),
            BuildingLocation(0.0, 0.0), "u1", AllowedTenants.ANY, emptyList(), null,
        ).exceptionOrNull() as LandlordException
        assertEquals(LandlordErrorCode.BUILDINGS_LIMIT, failure.code)
    }

    // ===== ROOMS =====

    @Test
    fun `rooms list ordered payload maps`() = runTest {
        server.enqueue(
            jsonResponse(
                200,
                """{"items":[$roomJson],"page":1,"pageSize":50,"totalItems":1,"totalPages":1}""",
            ),
        )
        val page = repository.getRooms("b1").getOrThrow()
        assertEquals(RoomStatus.READY, page.items.first().status)
        assertEquals(2, page.items.first().version)
        assertEquals("/api/v1/landlord/buildings/b1/rooms", server.takeRequest().url.encodedPath)
    }

    @Test
    fun `generate rooms sends floors and base`() = runTest {
        server.enqueue(
            jsonResponse(
                201,
                """{"items":[$roomJson],"page":1,"pageSize":50,"totalItems":1,"totalPages":1}""",
            ),
        )
        val page = repository.generateRooms(
            buildingId = "b1",
            floors = 2,
            roomsPerFloor = listOf(3, 3),
            base = com.thehouse.app.domain.model.RoomBase(
                price = 3500,
                sizeM2 = 12,
                type = RoomType.SINGLE,
                bathroom = BathroomType.SHARED,
                furniture = listOf(com.thehouse.app.domain.model.Furniture.BED),
                availableFrom = "2026-12-01",
                minContract = MinContract.SIX_MONTHS,
            ),
        ).getOrThrow()
        assertEquals(1, page.items.size)
        val body = Json.parseToJsonElement(server.takeRequest().body?.utf8() ?: "").jsonObject
        assertEquals("floor_prefix", (body["numbering"] as JsonPrimitive).content)
        assertTrue(body.containsKey("roomsPerFloor") && body.containsKey("base"))
    }

    @Test
    fun `room update sends version in body`() = runTest {
        server.enqueue(jsonResponse(200, roomJson))
        val room = repository.updateRoom(
            id = "r1",
            version = 2,
            patch = RoomPatch(price = 4000),
        ).getOrThrow()
        assertEquals(3500, room.price)
        val body = Json.parseToJsonElement(server.takeRequest().body?.utf8() ?: "").jsonObject
        assertEquals(2, (body["version"] as JsonPrimitive).content.toInt())
        assertEquals(4000, (body["price"] as JsonPrimitive).content.toInt())
    }

    @Test
    fun `room delete sends version in query`() = runTest {
        server.enqueue(MockResponse.Builder().code(204).build())
        assertTrue(repository.deleteRoom("r1", 2).isSuccess)
        assertEquals("2", server.takeRequest().url.queryParameter("version"))
    }

    @Test
    fun `pause and resume return room`() = runTest {
        server.enqueue(jsonResponse(200, roomJson.replace("\"ready\"", "\"paused\"")))
        val paused = repository.pauseRoom("r1").getOrThrow()
        assertEquals(RoomStatus.PAUSED, paused.status)
        server.enqueue(jsonResponse(200, roomJson))
        val resumed = repository.resumeRoom("r1").getOrThrow()
        assertEquals(RoomStatus.READY, resumed.status)
    }

    @Test
    fun `room_state 409 maps code`() = runTest {
        server.enqueue(jsonResponse(409, problem("room_state", 409)))
        val failure = repository.pauseRoom("r1").exceptionOrNull() as LandlordException
        assertEquals(LandlordErrorCode.ROOM_STATE, failure.code)
    }

    @Test
    fun `room_number_taken 409 maps code`() = runTest {
        server.enqueue(jsonResponse(409, problem("room_number_taken", 409)))
        val failure = repository.updateRoom("r1", 2, RoomPatch(number = "101"))
            .exceptionOrNull() as LandlordException
        assertEquals(LandlordErrorCode.ROOM_NUMBER_TAKEN, failure.code)
    }

    // ===== ERRORES =====

    @Test
    fun `400 maps validation`() = runTest {
        server.enqueue(jsonResponse(400, problem("validation_failed", 400)))
        val failure = repository.getBuildings().exceptionOrNull() as LandlordException
        assertEquals(LandlordErrorCode.VALIDATION, failure.code)
    }

    @Test
    fun `401 maps unauthorized`() = runTest {
        server.enqueue(jsonResponse(401, problem("unauthorized", 401)))
        val failure = repository.getBuildings().exceptionOrNull() as LandlordException
        assertEquals(DomainError.Unauthorized, failure.domainError)
    }

    @Test
    fun `404 maps not found`() = runTest {
        server.enqueue(jsonResponse(404, problem("not_found", 404)))
        val failure = repository.getBuilding("b1").exceptionOrNull() as LandlordException
        assertEquals(DomainError.NotFound, failure.domainError)
    }

    @Test
    fun `429 maps rate limited with retry after`() = runTest {
        server.enqueue(
            MockResponse.Builder()
                .code(429)
                .addHeader("Retry-After", "30")
                .body("")
                .build(),
        )
        val failure = repository.getBuildings().exceptionOrNull() as LandlordException
        assertEquals(DomainError.RateLimited, failure.domainError)
        assertEquals(30L, failure.retryAfterSeconds)
    }

    @Test
    fun `503 maps server`() = runTest {
        server.enqueue(jsonResponse(503, problem("service_unavailable", 503)))
        val failure = repository.getBuildings().exceptionOrNull() as LandlordException
        assertEquals(DomainError.Server, failure.domainError)
    }
}
