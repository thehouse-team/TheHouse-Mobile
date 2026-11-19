package com.thehouse.app.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Bathtub
import androidx.compose.material.icons.outlined.Bed
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.SquareFoot
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Roofing
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.thehouse.app.domain.model.Property
import com.thehouse.app.presentation.demo.PrototypeProperties
import com.thehouse.app.presentation.demo.PrototypeProperty
import com.thehouse.app.ui.theme.Elevations
import com.thehouse.app.ui.theme.IconSizes
import com.thehouse.app.ui.theme.Spacing

/**
 * Tarjeta de renta REAL (D5, NV-06/NV-07).
 * - Lenguaje visual TheHouse: imagen 168dp, radio 16, precio destacado.
 * - Muestra SOLO campos del contrato (título, precio+moneda, ciudad,
 *   recámaras/baños/área, foto real si existe).
 * - NUNCA Match, verificación, distancia ni filtros inventados.
 */
@Composable
fun PropertyCard(
    property: Property,
    onFavoriteClick: () -> Unit,
    onContactClick: () -> Unit,
    modifier: Modifier = Modifier,
    isFavorite: Boolean = false,
    onPropertyClick: () -> Unit,
) {
    Surface(
        onClick = onPropertyClick,
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Renta ${property.title}" },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
        shadowElevation = Elevations.card,
    ) {
        Column {
            Box {
                val thumb = property.thumbnailUrl
                if (!thumb.isNullOrBlank()) {
                    AsyncImage(
                        model = thumb,
                        contentDescription = "Foto de ${property.title}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(168.dp),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(168.dp)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        MaterialTheme.colorScheme.secondaryContainer,
                                    ),
                                ),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Roofing,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                            modifier = Modifier.size(44.dp),
                        )
                    }
                }
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.94f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(Spacing.sm),
                ) {
                    IconButton(
                        onClick = onFavoriteClick,
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = if (isFavorite) "Quitar favorito" else "Guardar favorito",
                            tint = if (isFavorite) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(IconSizes.md),
                        )
                    }
                }
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.94f),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(Spacing.sm),
                ) {
                    Text(
                        text = "$" + formatPrice(property.price) + " " + property.currency,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs),
                    )
                }
            }
            Column(
                modifier = Modifier.padding(Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Text(
                    text = property.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(IconSizes.sm),
                    )
                    Text(
                        text = property.city,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    InfoChip("${property.bedrooms} rec.", icon = Icons.Outlined.Bed)
                    InfoChip("${property.bathrooms} baños", icon = Icons.Outlined.Bathtub)
                    InfoChip("${formatArea(property.areaM2)} m²", icon = Icons.Outlined.SquareFoot)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "$" + formatPrice(property.price) + " " + property.currency,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = "por mes",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    OutlinedButton(
                        onClick = onContactClick,
                        shape = MaterialTheme.shapes.small,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        contentPadding = PaddingValues(horizontal = Spacing.md),
                        modifier = Modifier.height(40.dp),
                    ) {
                        Text("Contactar", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

/**
 * Tarjeta de renta del prototipo (D4, NV-05).
 * - Datos de prototipo etiquetados como demo, nunca como backend.
 * - Acciones de contacto/favorito son eventos de UI (sin tocar backend).
 */
@Composable
fun PropertyCard(
    property: PrototypeProperty,
    onFavoriteClick: () -> Unit,
    onContactClick: () -> Unit,
    modifier: Modifier = Modifier,
    isFavorite: Boolean = false,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Renta ${property.title}" },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
        shadowElevation = Elevations.card,
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.secondaryContainer,
                            ),
                        ),
                    ),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Roofing,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(44.dp),
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(Spacing.sm),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    MatchBadge(matchPercent = property.matchPercent)
                    if (property.verified) VerifiedBadge()
                }
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.94f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(Spacing.sm),
                ) {
                    IconButton(onClick = onFavoriteClick, modifier = Modifier.size(40.dp)) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = if (isFavorite) "Quitar favorito" else "Guardar favorito",
                            tint = if (isFavorite) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(IconSizes.md),
                        )
                    }
                }
            }
            Column(
                modifier = Modifier.padding(Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Text(
                    text = property.title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = distanceText(property),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "$" + formatPrice(property.priceMxn.toDouble()) + " MXN/mes",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = property.services.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (property.isDemo) {
                    StatusChip(label = "Demo del prototipo", tone = StatusTone.Neutral)
                }
                TheHouseSecondaryButton(
                    text = "Contactar",
                    onClick = onContactClick,
                )
            }
        }
    }
}

private fun formatPrice(value: Double): String {
    val longValue = value.toLong()
    return if (value == longValue.toDouble()) {
        java.text.NumberFormat.getIntegerInstance(java.util.Locale.US).format(longValue)
    } else {
        String.format(java.util.Locale.US, "%,.2f", value)
    }
}

private fun formatArea(value: Double): String {
    val longValue = value.toLong()
    return if (value == longValue.toDouble()) longValue.toString() else String.format(java.util.Locale.US, "%.1f", value)
}

private fun distanceText(property: PrototypeProperty): String {
    val meters = property.distanceMeters
    return if (meters != null) {
        "A ${property.distanceMinutes} min de tu universidad ($meters m)"
    } else {
        "A ${property.distanceMinutes} min de tu universidad"
    }
}

@Preview(showBackground = true)
@Composable
private fun PropertyCardPreview() {
    MaterialTheme {
        PropertyCard(
            property = PrototypeProperties.featuredProperty,
            onFavoriteClick = {},
            onContactClick = {},
        )
    }
}
