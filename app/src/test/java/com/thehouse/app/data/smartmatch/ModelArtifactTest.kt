package com.thehouse.app.data.smartmatch

import com.thehouse.app.domain.smartmatch.FeatureVector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ModelArtifactTest {

    @Test(expected = IllegalArgumentException::class)
    fun `malformed artifact missing coefficients fails`() {
        val bad = """
            {"modelVersion":"1","featureVersion":"1","algorithm":"logistic_regression","features":["a"],"coefficients":[],"bias":0.0,"scaler":{"mean":[0],"std":[1]}}
        """.trimIndent()
        LogisticRegressionSmartMatchModel.loadFromJson(bad)
    }

    @Test(expected = Exception::class)
    fun `malformed artifact wrong algorithm fails`() {
        val bad = """
            {"modelVersion":"1","featureVersion":"1","algorithm":"decision_tree","features":["a"],"coefficients":[1.0],"bias":0.0,"scaler":{"mean":[0.0],"std":[1.0]}}
        """.trimIndent()
        LogisticRegressionSmartMatchModel.loadFromJson(bad)
    }

    @Test
    fun `normalization applied via scaler`() {
        val json = """
            {
              "modelVersion":"t","featureVersion":"t","algorithm":"logistic_regression",
              "features":["x"],
              "coefficients":[2.0],
              "bias":0.0,
              "scaler":{"mean":[10.0],"std":[2.0]}
            }
        """.trimIndent()
        val model = LogisticRegressionSmartMatchModel.loadFromJson(json)
        // x=10 -> normalized 0 -> z=0 -> p=0.5
        val p = model.predictProbability(FeatureVector(doubleArrayOf(10.0)))
        assertEquals(0.5, p, 1e-9)
        val pHigh = model.predictProbability(FeatureVector(doubleArrayOf(12.0))) // (12-10)/2=1 -> z=2
        assertTrue(pHigh > 0.8)
    }

    @Test
    fun `artifact hash matches declared`() {
        val raw = File("app/src/main/assets/smartmatch/model_v1.json").readText()
        val model = LogisticRegressionSmartMatchModel.loadFromJson(raw)
        // recompute same way as loader
        val without = LogisticRegressionSmartMatchModel.removeSha256Field(raw)
        val computed = LogisticRegressionSmartMatchModel.sha256(without)
        assertEquals(computed, model.artifactHash)
    }
}
