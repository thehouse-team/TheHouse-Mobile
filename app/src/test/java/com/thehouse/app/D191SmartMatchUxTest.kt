package com.thehouse.app

import com.thehouse.app.data.serialization.JsonProvider
import com.thehouse.app.data.remote.dto.smartmatch.SmartMatchListResponse
import com.thehouse.app.domain.smartmatch.SmartMatchFactor
import com.thehouse.app.domain.smartmatch.SmartMatchResult
import com.thehouse.app.domain.smartmatch.SmartMatchSource
import com.thehouse.app.presentation.navigation.RoomDetailRoute
import com.thehouse.app.presentation.smartmatch.factorProgress
import com.thehouse.app.presentation.smartmatch.formatMatchScore
import com.thehouse.app.presentation.smartmatch.scoreVisualRange
import com.thehouse.app.presentation.smartmatch.ScoreVisualRange
import java.io.File
import kotlinx.serialization.decodeFromString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * D19.1 UX de Smart Match y regresión del crash de navegación a RoomDetail.
 * Los rangos visuales NO son reglas del modelo: solo colorean el indicador.
 */
class D191SmartMatchUxTest {

    @Test
    fun `room detail route is exactly cuarto slash room id`() {
        val id = "6aef802d406d73585006cfc1"
        val route = RoomDetailRoute(id).route
        assertEquals("cuarto/$id", route)
        assertFalse(route.contains("{"))
        assertFalse(route.contains("propiedad"))
        assertEquals("cuarto/{roomId}", RoomDetailRoute.PATTERN)
    }

    @Test
    fun `decoded room id from route pattern maps to real room`() {
        val id = "room-93"
        val route = RoomDetailRoute(id).route
        val decoded = route.removePrefix("cuarto/")
        assertEquals(id, decoded)
        assertTrue(RoomDetailRoute.isValid(decoded))
    }

    @Test
    fun `circular score color ranges are visual only`() {
        assertEquals(ScoreVisualRange.Neutral, scoreVisualRange(0))
        assertEquals(ScoreVisualRange.Neutral, scoreVisualRange(39))
        assertEquals(ScoreVisualRange.Medium, scoreVisualRange(40))
        assertEquals(ScoreVisualRange.Medium, scoreVisualRange(59))
        assertEquals(ScoreVisualRange.Good, scoreVisualRange(60))
        assertEquals(ScoreVisualRange.Good, scoreVisualRange(79))
        assertEquals(ScoreVisualRange.High, scoreVisualRange(80))
        assertEquals(ScoreVisualRange.High, scoreVisualRange(93))
        assertEquals(ScoreVisualRange.High, scoreVisualRange(100))
        assertEquals(ScoreVisualRange.High, scoreVisualRange(150))
        assertEquals(ScoreVisualRange.Unavailable, scoreVisualRange(0, unavailable = true))
    }

    @Test
    fun `unavailable score is never rendered as zero percent`() {
        val source = sourceOf(
            "src/main/java/com/thehouse/app/presentation/smartmatch/SmartMatchRecommendationCard.kt",
        )
        assertTrue(source.contains("val label = if (unavailable) \"Sin porcentaje\" else \"\$score%\""))
        assertTrue(source.contains("if (!unavailable) {"))
        assertTrue(source.contains("scoreUnavailable"))
    }

    @Test
    fun `factor progress handles decimals zero max and NaN`() {
        assertEquals(0.96, factorProgress(9.6, 10.0), 0.0001)
        assertEquals(0.726, factorProgress(72.6, 100.0), 0.0001)
        assertEquals(0.067, factorProgress(6.7, 100.0), 0.0001)
        assertEquals(0.049, factorProgress(4.9, 100.0), 0.0001)
        assertEquals(0.017, factorProgress(1.7, 100.0), 0.0001)
        assertEquals(0.045, factorProgress(4.5, 100.0), 0.0001)
        assertEquals(0.0, factorProgress(5.0, 0.0), 0.0)
        assertEquals(0.0, factorProgress(Double.NaN, 10.0), 0.0)
        assertEquals(0.0, factorProgress(5.0, Double.NaN), 0.0)
        assertEquals(1.0, factorProgress(999.0, 10.0), 0.0)
        assertEquals(0.0, factorProgress(-5.0, 10.0), 0.0)
    }

    @Test
    fun `factor labels and max points come from backend not hardcoded`() {
        val factors = listOf(
            SmartMatchFactor("budget", "Presupuesto", 9.6, 10.0),
            SmartMatchFactor("distance", "Distancia", 72.6, 100.0),
        )
        assertEquals("Presupuesto", factors[0].label)
        assertEquals(10.0, factors[0].maxPoints, 0.0)
        assertEquals(100.0, factors[1].maxPoints, 0.0)
        val source = sourceOf(
            "src/main/java/com/thehouse/app/presentation/smartmatch/SmartMatchRecommendationCard.kt",
        )
        assertFalse(source.contains("72.6"))
        assertFalse(source.contains("9.6"))
    }

    @Test
    fun `recommendation payload deserializes real room fields`() {
        val json = """
            {
              "items": [{
                "room": {
                  "id": "6aef802d406d73585006cfc1",
                  "buildingId": "b-1",
                  "title": "Cuarto en casa compartida en Tepeji — Cuarto 101",
                  "price": 1900.0,
                  "currency": "MXN",
                  "roomType": "single",
                  "bathroom": "shared",
                  "buildingType": "rooms_building",
                  "allowedTenants": "any",
                  "services": ["wifi", "parking"],
                  "distanceKm": 1.2,
                  "walkingMinutes": 15,
                  "thumbnailUrl": null,
                  "demo": false
                },
                "matchScore": 93,
                "breakdown": [{"factor":"budget","label":"Presupuesto","points":9.6,"maxPoints":10.0}],
                "dismissed": false,
                "modelVersion": "logreg-1.0.0",
                "featureSchemaVersion": "2.0.0"
              }],
              "page": 1,
              "pageSize": 20,
              "totalItems": 1,
              "totalPages": 1
            }
        """.trimIndent()
        val response: SmartMatchListResponse = JsonProvider.create().decodeFromString(json)
        assertEquals(1, response.items.size)
        val item = response.items.first()
        assertEquals("6aef802d406d73585006cfc1", item.room.id)
        assertEquals(1900.0, item.room.price, 0.0)
        assertEquals(93, item.matchScore)
        assertEquals(1.2, item.room.distanceKm, 0.0)
        assertEquals(15, item.room.walkingMinutes)
        assertEquals(listOf("wifi", "parking"), item.room.services)
        assertEquals("93%", formatMatchScore(93))
    }

    @Test
    fun `empty list from backend stays empty without local fallback`() {
        val response: SmartMatchListResponse = JsonProvider.create().decodeFromString(
            """{"items":[],"page":1,"pageSize":20,"totalItems":0,"totalPages":0}""",
        )
        assertTrue(response.items.isEmpty())
        val source = sourceOf(
            "src/main/java/com/thehouse/app/presentation/smartmatch/SmartMatchViewModel.kt",
        )
        // El fallback existe SOLO para network/503; 200 vacío no lo activa.
        assertTrue(source.contains("UiError.Network, UiError.Timeout ->"))
        assertTrue(source.contains("UiError.ServiceUnavailable ->"))
        assertTrue(source.contains("loadAuthenticated"))
    }

    @Test
    fun `mapper keeps only real room fields and no invented zeros`() {
        val source = sourceOf(
            "src/main/java/com/thehouse/app/presentation/smartmatch/SmartMatchViewModel.kt",
        )
        assertTrue(source.contains("distanceKm = room.distanceKm"))
        assertTrue(source.contains("walkingMinutes = room.walkingMinutes"))
        assertTrue(source.contains("services = room.services"))
        assertTrue(source.contains("thumbnailUrl = room.thumbnailUrl"))
        val card = sourceOf(
            "src/main/java/com/thehouse/app/presentation/smartmatch/SmartMatchRecommendationCard.kt",
        )
        assertFalse(card.contains("0 habitaciones"))
        assertFalse(card.contains("0 baños"))
        assertFalse(card.contains("0 m²"))
    }

    @Test
    fun `card uses stable room id key and single percentage`() {
        val source = sourceOf(
            "src/main/java/com/thehouse/app/presentation/smartmatch/SmartMatchScreen.kt",
        )
        assertTrue(source.contains("items(s.results, key = { it.roomId })"))
        val card = sourceOf(
            "src/main/java/com/thehouse/app/presentation/smartmatch/SmartMatchRecommendationCard.kt",
        )
        assertEquals(1, card.split("val label = if (unavailable)").size - 1)
        assertTrue(card.contains("¿Por qué es compatible contigo?"))
        assertTrue(card.contains("Ver alojamiento"))
    }

    @Test
    fun `domain model carries real room info with safe defaults`() {
        val result = SmartMatchResult(
            roomId = "room-1",
            property = com.thehouse.app.domain.model.Property(
                id = "room-1",
                title = "Real",
                price = 1900.0,
                currency = "MXN",
                city = "",
                universityId = null,
                bedrooms = 0,
                bathrooms = 0,
                areaM2 = 0.0,
                createdAt = "",
                description = null,
                amenities = emptyList(),
                thumbnailUrl = null,
                photos = emptyList(),
            ),
            probability = 0.93,
            matchScore = 93,
            factors = emptyList(),
            source = SmartMatchSource.BACKEND,
        )
        assertEquals(null, result.distanceKm)
        assertEquals(emptyList<String>(), result.services)
        assertFalse(result.scoreUnavailable)
    }

    private fun sourceOf(path: String): String {
        val candidates = listOf(File(path), File("app", path))
        return candidates.firstOrNull { it.exists() }?.readText()
            ?: throw AssertionError("No se encontró $path")
    }
}
