package com.thehouse.app.data.smartmatch

import android.content.Context
import com.thehouse.app.domain.smartmatch.FeatureVector
import com.thehouse.app.domain.smartmatch.SmartMatchModel
import kotlinx.serialization.json.Json
import java.security.MessageDigest
import kotlin.math.abs
import kotlin.math.exp

/**
 * Carga el artefacto de modelo (coeficientes aprendidos en entrenamiento offline)
 * y ejecuta inferencia logística: z = bias + sum(w_i * x_i_norm), p = sigmoid(z).
 * No contiene pesos hardcodeados de negocio; los parámetros provienen del artefacto.
 */
class LogisticRegressionSmartMatchModel(
    private val artifact: SmartMatchModelArtifact,
    override val artifactHash: String,
) : SmartMatchModel {

    override val modelVersion: String = artifact.modelVersion
    override val featureVersion: String = artifact.featureVersion

    private val coefficients: DoubleArray = artifact.coefficients.toDoubleArray()
    private val bias: Double = artifact.bias
    private val mean: DoubleArray = artifact.scaler.mean.toDoubleArray()
    private val std: DoubleArray = artifact.scaler.std.toDoubleArray()

    override fun predictProbability(vector: FeatureVector): Double {
        val x = vector.values
        require(x.size == coefficients.size) {
            "Feature vector size ${x.size} != coefficients size ${coefficients.size}"
        }
        var z = bias
        for (i in coefficients.indices) {
            val s = if (i < std.size && abs(std[i]) > 1e-12) std[i] else 1.0
            val m = if (i < mean.size) mean[i] else 0.0
            val xn = (x[i] - m) / s
            z += coefficients[i] * xn
        }
        return sigmoid(z)
    }

    private fun sigmoid(x: Double): Double = if (x >= 0.0) {
        val z = exp(-x)
        1.0 / (1.0 + z)
    } else {
        val z = exp(x)
        z / (1.0 + z)
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun loadFromAssets(
            context: Context,
            assetPath: String = "smartmatch/model_v1.json",
        ): LogisticRegressionSmartMatchModel {
            val bytes = context.assets.open(assetPath).use { it.readBytes() }
            val raw = bytes.toString(Charsets.UTF_8)
            return loadFromJson(raw)
        }

        fun loadFromJson(raw: String): LogisticRegressionSmartMatchModel {
            val artifact = json.decodeFromString(SmartMatchModelArtifact.serializer(), raw)
            require(artifact.algorithm == "logistic_regression") {
                "Unsupported algorithm: ${artifact.algorithm}"
            }
            require(artifact.features.isNotEmpty()) { "Model artifact has no features" }
            require(artifact.coefficients.size == artifact.features.size) {
                "Coefficients/features size mismatch"
            }
            require(artifact.scaler.mean.size == artifact.features.size) {
                "Scaler mean/features size mismatch"
            }
            require(artifact.scaler.std.size == artifact.features.size) {
                "Scaler std/features size mismatch"
            }
            val declared = artifact.sha256
            val hash = if (!declared.isNullOrBlank()) {
                val without = removeSha256Field(raw)
                val computed = sha256(without)
                require(computed == declared) {
                    "Model artifact hash mismatch: expected $declared got $computed"
                }
                declared
            } else {
                sha256(raw)
            }
            return LogisticRegressionSmartMatchModel(artifact, hash)
        }

        internal fun removeSha256Field(raw: String): String {
            // Controlled format: sha256 is a top-level string field added at end by training script.
            // Remove the line/property `"sha256": "..."` and any trailing comma before it.
            val withoutLine = raw.replace(Regex("\\n\\s*\"sha256\"\\s*:\\s*\"[a-fA-F0-9]*\"\\s*"), "\n")
            return withoutLine.replace(Regex(",\\s*\\n(\\s*\\})"), "\n$1")
        }

        internal fun sha256(text: String): String {
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(text.toByteArray(Charsets.UTF_8))
            return digest.joinToString("") { "%02x".format(it) }
        }
    }
}
