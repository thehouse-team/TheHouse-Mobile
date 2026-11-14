package com.thehouse.app.domain.smartmatch

import com.thehouse.app.data.smartmatch.LogisticRegressionSmartMatchModel
import com.thehouse.app.domain.model.Budget
import com.thehouse.app.domain.model.Preference
import com.thehouse.app.domain.model.Property
import com.thehouse.app.domain.usecase.RunSmartMatchUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.abs

class SmartMatchMlTest {

    private fun artifactFile(): File {
        val candidates = listOf(
            File("app/src/main/assets/smartmatch/model_v1.json"),
            File("src/main/assets/smartmatch/model_v1.json"),
            File("../app/src/main/assets/smartmatch/model_v1.json"),
        )
        return candidates.firstOrNull { it.exists() }
            ?: error("model_v1.json not found. cwd=${File(".").absolutePath}")
    }

    private fun loadProductionModel(): LogisticRegressionSmartMatchModel {
        val raw = artifactFile().readText()
        return LogisticRegressionSmartMatchModel.loadFromJson(raw)
    }

    private fun pref(
        min: Double? = 2000.0,
        max: Double? = 5000.0,
        currency: String? = "MXN",
        services: List<String>? = listOf("wifi", "water"),
    ) = Preference(
        budget = Budget(min = min, max = max, currency = currency),
        maxDistanceKm = null,
        essentialServices = services,
        livingWith = null,
        placeType = null,
        pets = null,
        transport = null,
        propertyTypes = null,
        currency = currency,
    )

    private fun prop(
        id: String,
        price: Double,
        currency: String = "MXN",
        amenities: List<String> = listOf("wifi", "water"),
    ) = Property(
        id = id,
        title = "Prop $id",
        price = price,
        currency = currency,
        city = "Puebla",
        universityId = "u1",
        bedrooms = 1,
        bathrooms = 1,
        areaM2 = 20.0,
        createdAt = "2026-01-01T00:00:00Z",
        description = null,
        amenities = amenities,
    )

    @Test
    fun `feature extraction budget fit and services overlap`() {
        val p = pref(min = 3000.0, max = 4000.0, services = listOf("wifi", "water", "gas"))
        val inBudget = prop("1", 3500.0, amenities = listOf("wifi", "gas"))
        val v1 = SmartMatchFeatureExtractor.extract(p, inBudget)
        assertEquals(1.0, v1.values[0], 1e-9) // budget_fit
        assertEquals(2.0 / 3.0, v1.values[1], 1e-9) // services overlap

        val outBudget = prop("2", 8000.0, amenities = emptyList())
        val v2 = SmartMatchFeatureExtractor.extract(p, outBudget)
        assertTrue(v2.values[0] < 1.0)
        assertEquals(0.0, v2.values[1], 1e-9)
    }

    @Test
    fun `currency match feature`() {
        val p = pref(currency = "MXN")
        val same = prop("1", 3000.0, currency = "MXN")
        val diff = prop("2", 3000.0, currency = "USD")
        assertEquals(1.0, SmartMatchFeatureExtractor.extract(p, same).values[3], 1e-9)
        assertEquals(0.0, SmartMatchFeatureExtractor.extract(p, diff).values[3], 1e-9)
    }

    @Test
    fun `artifact parsing hash and versions`() {
        val model = loadProductionModel()
        assertEquals("1.0.0", model.modelVersion)
        assertEquals("1.0.0", model.featureVersion)
        assertEquals(64, model.artifactHash.length)
        assertTrue(model.artifactHash.all { it in '0'..'9' || it in 'a'..'f' })
    }

    @Test
    fun `inference uses trained artifact coefficients not hardcoded engine`() {
        // Motor de producción = coeficientes aprendidos del artefacto (cargado desde JSON), no pesos fijos en código.
        val raw = artifactFile().readText()
        assertTrue(raw.contains("\"coefficients\""))
        assertTrue(raw.contains("\"bias\""))
        val model = loadProductionModel()
        // Misma entrada produce salida definida por parámetros del artefacto.
        val v = FeatureVector(doubleArrayOf(1.0, 1.0, 1.0, 1.0))
        val p = model.predictProbability(v)
        assertTrue(p in 0.0..1.0)
        // Si alteramos el "artefacto" (coeficientes/bias) cambia la salida -> prueba que el motor usa el artefacto.
        val mutated = raw.replaceFirst(Regex("\"bias\"\\s*:\\s*[-0-9.eE]+"), "\"bias\": 5.0")
            .replace(Regex(",?\\s*\"sha256\"\\s*:\\s*\"[a-fA-F0-9]*\""), "")
        val mutatedModel = LogisticRegressionSmartMatchModel.loadFromJson(mutated)
        val p2 = mutatedModel.predictProbability(v)
        assertNotEquals(p, p2, 1e-12)
    }

    @Test
    fun `sigmoid inference in range`() {
        val model = loadProductionModel()
        val vectors = listOf(
            FeatureVector(doubleArrayOf(0.0, 0.0, 0.0, 0.0)),
            FeatureVector(doubleArrayOf(1.0, 1.0, 1.0, 1.0)),
            FeatureVector(doubleArrayOf(0.5, 0.2, 0.7, 1.0)),
        )
        vectors.forEach { v ->
            val p = model.predictProbability(v)
            assertTrue("prob $p out of range", p >= 0.0 && p <= 1.0)
        }
    }

    @Test
    fun `score range ranking and determinism`() {
        val model = loadProductionModel()
        val useCase = RunSmartMatchUseCase(model)
        val preference = pref(min = 3000.0, max = 5000.0, services = listOf("wifi", "laundry"))
        val candidates = listOf(
            prop("a", 3500.0, amenities = listOf("wifi", "laundry")),
            prop("b", 9000.0, amenities = emptyList()),
            prop("c", 4200.0, amenities = listOf("wifi")),
        )
        val r1 = useCase.execute(preference, candidates)
        val r2 = useCase.execute(preference, candidates)
        assertEquals(r1.map { it.property.id }, r2.map { it.property.id })
        assertEquals(r1.map { it.score }, r2.map { it.score })
        assertTrue(r1.all { it.score in 0..100 })
        // ranking descendente por probabilidad
        for (i in 0 until r1.size - 1) {
            assertTrue(r1[i].probability + 1e-12 >= r1[i + 1].probability)
        }
        // el candidato claramente compatible no debe quedar por debajo del incompatible
        val scoreA = r1.first { it.property.id == "a" }.score
        val scoreB = r1.first { it.property.id == "b" }.score
        assertTrue("compatible score $scoreA should be > incompatible $scoreB", scoreA > scoreB)
    }

    @Test
    fun `preference change alters results when relevant`() {
        val model = loadProductionModel()
        val useCase = RunSmartMatchUseCase(model)
        val candidates = listOf(
            prop("cheap", 1500.0, amenities = listOf("wifi")),
            prop("mid", 3500.0, amenities = listOf("wifi", "laundry")),
        )
        val lowBudget = pref(min = 1000.0, max = 2000.0, services = listOf("wifi"))
        val highBudget = pref(min = 3000.0, max = 4000.0, services = listOf("wifi", "laundry"))
        val rLow = useCase.execute(lowBudget, candidates)
        val rHigh = useCase.execute(highBudget, candidates)
        assertEquals("cheap", rLow.first().property.id)
        assertEquals("mid", rHigh.first().property.id)
    }

    @Test
    fun `incompatible currency and out of budget not artificially high`() {
        val model = loadProductionModel()
        val useCase = RunSmartMatchUseCase(model)
        val preference = pref(min = 2000.0, max = 3000.0, currency = "MXN", services = listOf("wifi"))
        val bad = prop("bad", 15000.0, currency = "USD", amenities = emptyList())
        val good = prop("good", 2500.0, currency = "MXN", amenities = listOf("wifi"))
        val r = useCase.execute(preference, listOf(bad, good))
        val badScore = r.first { it.property.id == "bad" }.score
        val goodScore = r.first { it.property.id == "good" }.score
        assertTrue("bad=$badScore good=$goodScore", goodScore > badScore)
    }

    @Test
    fun `amenities mapping overlap reflected`() {
        val p = pref(services = listOf("Wifi", "Laundry", "Parking"))
        val propMatch = prop("m", 3000.0, amenities = listOf("wifi", "parking", "pool"))
        val propNone = prop("n", 3000.0, amenities = listOf("gym"))
        val fMatch = SmartMatchFeatureExtractor.extract(p, propMatch).values[1]
        val fNone = SmartMatchFeatureExtractor.extract(p, propNone).values[1]
        assertTrue(abs(fMatch - (2.0 / 3.0)) < 1e-9)
        assertEquals(0.0, fNone, 1e-9)
        assertTrue(fMatch > fNone)
    }

    @Test
    fun `no PII in feature vector`() {
        val p = pref()
        val property = prop("id-123", 3000.0)
        val v = SmartMatchFeatureExtractor.extract(p, property)
        assertEquals(SmartMatchFeatures.ORDERED.size, v.values.size)
        // solo valores numéricos normalizados 0..1 esperables, sin ids/texto
        assertTrue(v.values.all { it.isFinite() })
    }

    @Test
    fun `factors are structural and separated from score`() {
        val p = pref(min = 2000.0, max = 4000.0, services = listOf("wifi"))
        val property = prop("1", 3000.0, amenities = listOf("wifi"))
        val factors = SmartMatchFeatureExtractor.factors(p, property)
        assertTrue(factors.isNotEmpty())
        assertTrue(factors.any { it.contains("presupuesto", true) || it.contains("servicio", true) || it.contains("Moneda", true) })
    }
}
