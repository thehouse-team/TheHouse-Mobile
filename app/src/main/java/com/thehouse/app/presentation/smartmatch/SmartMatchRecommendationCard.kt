package com.thehouse.app.presentation.smartmatch

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import com.thehouse.app.domain.room.RoomSearchCatalog
import com.thehouse.app.domain.smartmatch.SmartMatchFactor
import com.thehouse.app.domain.smartmatch.SmartMatchResult
import com.thehouse.app.domain.smartmatch.SmartMatchSource
import com.thehouse.app.presentation.components.TheHouseImage
import com.thehouse.app.presentation.components.TheHouseCard
import com.thehouse.app.ui.theme.IconSizes
import com.thehouse.app.ui.theme.Spacing

/** Rangos VISUALES del porcentaje; NO son reglas nuevas del modelo. */
internal enum class ScoreVisualRange { High, Good, Medium, Neutral, Unavailable }

internal fun scoreVisualRange(score: Int, unavailable: Boolean = false): ScoreVisualRange =
    if (unavailable) ScoreVisualRange.Unavailable
    else when (val clamped = score.coerceIn(0, 100)) {
        in 80..100 -> ScoreVisualRange.High
        in 60..79 -> ScoreVisualRange.Good
        in 40..59 -> ScoreVisualRange.Medium
        else -> ScoreVisualRange.Neutral
    }

/**
 * Proporción de una barra del breakdown. Nunca NaN, nunca división por cero
 * y siempre dentro de 0..1. Los máximos vienen del Backend, no hardcodeados.
 */
internal fun factorProgress(points: Double, maxPoints: Double): Double {
    if (maxPoints.isNaN() || maxPoints <= 0.0 || points.isNaN() || points.isInfinite()) return 0.0
    val raw = points / maxPoints
    if (raw.isNaN() || raw.isInfinite()) return 0.0
    return raw.coerceIn(0.0, 1.0)
}

/** Tarjeta integrada de recomendación (D19.1 UX). Un solo porcentaje visible. */
@Composable
internal fun SmartMatchRecommendationCard(
    result: SmartMatchResult,
    onRoomClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onContactClick: () -> Unit,
    onLoadExplanation: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val unavailable = result.scoreUnavailable
    val score = result.matchScore.coerceIn(0, 100)

    TheHouseCard(
        modifier = modifier.testTag("smart_match_recommendation"),
        contentPadding = PaddingValues(0.dp),
    ) {
        RecommendationImage(result)
        Column(
            modifier = Modifier.padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = result.property.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val price = result.property.price
                    if (price > 0.0) {
                        Text(
                            text = "${formatPrice(price)} ${result.property.currency}".trim(),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                CircularScore(
                    score = score,
                    unavailable = unavailable,
                )
            }

            RealInfoRow(result)

            if (result.source == SmartMatchSource.LOCAL_ESTIMATE) {
                Text(
                    text = "Estimación local",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.semantics { contentDescription = "smart.match.local.fallback" },
                )
            }

            Button(
                onClick = onRoomClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(IconSizes.md),
                )
                Spacer(modifier = Modifier.size(Spacing.xs))
                Text(text = "Ver alojamiento", style = MaterialTheme.typography.labelLarge)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                // D20: acciones reales conectadas al estado compartido y al
                // formulario de solicitudes; sin "próximamente".
                OutlinedButton(
                    onClick = onFavoriteClick,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = if (result.isFavorite) Icons.Rounded.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = null,
                        modifier = Modifier.size(IconSizes.sm),
                    )
                    Spacer(modifier = Modifier.size(Spacing.xs))
                    Text(
                        if (result.isFavorite) "Quitar favorito" else "Guardar favorito",
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                OutlinedButton(
                    onClick = onContactClick,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Contactar", style = MaterialTheme.typography.labelMedium)
                }
            }

            TextButton(
                onClick = {
                    val next = !expanded
                    expanded = next
                    // D21: cargar explicación textual bajo demanda, sin recalcular score.
                    if (next && result.explanation == null && !result.explanationLoading) onLoadExplanation()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = if (expanded) "Ocultar detalles" else "¿Por qué es compatible contigo?",
                    style = MaterialTheme.typography.labelLarge,
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.testTag("smart_match_breakdown"),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    if (result.factors.isEmpty()) {
                        Text(
                            text = "El Backend no entregó factores para esta recomendación.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        result.factors.forEach { factor ->
                            FactorRow(factor = factor)
                        }
                    }

                    // D21: explicación textual del Backend, separada de los
                    // factores numéricos. Etiqueta honesta de su origen.
                    HorizontalDivider()
                    when {
                        result.explanationLoading -> Text(
                            text = "Cargando explicación…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        result.explanationFailed -> Text(
                            text = "No se pudo cargar la explicación. Intenta de nuevo.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                        result.explanation != null -> {
                            val explanation = result.explanation!!
                            Text(
                                text = if (explanation.generatedBy == "genai")
                                    "Explicación generada con IA"
                                else
                                    "Explicación basada en tus preferencias",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.semantics {
                                    contentDescription = "smart.match.explanation.source.${explanation.generatedBy}"
                                },
                            )
                            Text(explanation.summary, style = MaterialTheme.typography.bodySmall)
                            if (explanation.pros.isNotEmpty()) {
                                Text("A favor", style = MaterialTheme.typography.labelSmall)
                                explanation.pros.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                            }
                            if (explanation.cons.isNotEmpty()) {
                                Text("En contra", style = MaterialTheme.typography.labelSmall)
                                explanation.cons.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecommendationImage(result: SmartMatchResult) {
    TheHouseImage(
        url = result.thumbnailUrl,
        title = result.property.title,
        perfOperation = "image.load.smart_match",
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
    )
}

@Composable
private fun RealInfoRow(result: SmartMatchResult) {
    val parts = mutableListOf<String>()
    result.distanceKm?.let { parts += "${formatDecimal(it)} km" }
    result.walkingMinutes?.let { parts += "$it min caminando" }
    result.roomType?.takeIf { it.isNotBlank() }
        ?.let { parts += RoomSearchCatalog.label(RoomSearchCatalog.Kind.ROOM_TYPE, it) }
    result.buildingType?.takeIf { it.isNotBlank() }
        ?.let { parts += RoomSearchCatalog.label(RoomSearchCatalog.Kind.BUILDING_TYPE, it) }
    result.allowedTenants?.takeIf { it.isNotBlank() }
        ?.let { parts += RoomSearchCatalog.label(RoomSearchCatalog.Kind.ALLOWED_TENANTS, it) }
    if (parts.isNotEmpty()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Icon(
                imageVector = Icons.Outlined.LocationOn,
                contentDescription = null,
                modifier = Modifier.size(IconSizes.sm),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = parts.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
    if (result.services.isNotEmpty()) {
        Text(
            text = "Servicios: ${result.services.joinToString(", ") { readableCode(it) }}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun readableCode(code: String): String = code
    .replace('_', ' ')
    .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

private fun formatPrice(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else String.format(java.util.Locale.US, "%.2f", value)

private fun formatDecimal(value: Double): String = String.format(java.util.Locale.US, "%.1f", value)

/** Indicador circular sin librerías externas; clamp solo para dibujar. */
@Composable
private fun CircularScore(
    score: Int,
    unavailable: Boolean,
    modifier: Modifier = Modifier,
) {
    val range = scoreVisualRange(score, unavailable)
    val color = when (range) {
        ScoreVisualRange.High -> Color(0xFF2E7D32)
        ScoreVisualRange.Good -> MaterialTheme.colorScheme.primary
        ScoreVisualRange.Medium -> Color(0xFFB26A00)
        ScoreVisualRange.Neutral, ScoreVisualRange.Unavailable -> MaterialTheme.colorScheme.outline
    }
    val fraction = if (unavailable) 0f else score.coerceIn(0, 100) / 100f
    val label = if (unavailable) "Sin porcentaje" else "$score%"
    Box(
        modifier = modifier
            .size(64.dp)
            .semantics { contentDescription = "Compatibilidad: $label" },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(64.dp)) {
            val stroke = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
            drawArc(
                color = Color(0x1F000000),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = stroke,
            )
            if (!unavailable) {
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = 360f * fraction,
                    useCenter = false,
                    style = stroke,
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = if (unavailable) MaterialTheme.colorScheme.onSurfaceVariant else color,
            maxLines = 1,
        )
    }
}

@Composable
private fun FactorRow(factor: SmartMatchFactor) {
    val progress = factorProgress(factor.points, factor.maxPoints)
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = factor.label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${formatFactorPoints(factor.points)} / ${formatFactorPoints(factor.maxPoints)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(if (progress <= 0.0) 0f else (progress.toFloat()).coerceAtLeast(0.01f))
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .semantics { contentDescription = "smart.match.factor.${factor.factor}" },
            )
        }
    }
}

private fun formatFactorPoints(value: Double): String = when {
    value.isNaN() -> "—"
    value.isInfinite() -> "—"
    value % 1.0 == 0.0 -> value.toLong().toString()
    else -> String.format(java.util.Locale.US, "%.1f", value)
}
