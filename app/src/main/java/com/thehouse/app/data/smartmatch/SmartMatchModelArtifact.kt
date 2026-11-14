package com.thehouse.app.data.smartmatch

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SmartMatchModelArtifact(
    val modelVersion: String,
    val featureVersion: String,
    val algorithm: String,
    val features: List<String>,
    val coefficients: List<Double>,
    val bias: Double,
    val scaler: ScalerDto,
    val training: TrainingDto? = null,
    val sha256: String? = null,
)

@Serializable
data class ScalerDto(
    val mean: List<Double>,
    val std: List<Double>,
)

@Serializable
data class TrainingDto(
    val type: String? = null,
    val seed: Int? = null,
    val examples: Int? = null,
    val epochs: Int? = null,
    val learningRate: Double? = null,
    val l2: Double? = null,
    val note: String? = null,
)
