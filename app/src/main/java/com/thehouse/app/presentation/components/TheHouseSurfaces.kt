package com.thehouse.app.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.thehouse.app.ui.theme.Elevations
import com.thehouse.app.ui.theme.Heights
import com.thehouse.app.ui.theme.IconSizes
import com.thehouse.app.ui.theme.Radii
import com.thehouse.app.ui.theme.Spacing
import com.thehouse.app.ui.theme.theHouseColors

/**
 * Superficie base de tarjetas TheHouse: radio 16, borde suave y sombra mínima.
 * Evita duplicar CardDefaults en cada pantalla.
 */
@Composable
fun TheHouseCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(Spacing.lg),
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLowest,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = MaterialTheme.shapes.medium
    val border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            color = containerColor,
            border = border,
            shadowElevation = Elevations.card,
        ) {
            Column(modifier = Modifier.padding(contentPadding), content = content)
        }
    } else {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            color = containerColor,
            border = border,
            shadowElevation = Elevations.card,
        ) {
            Column(modifier = Modifier.padding(contentPadding), content = content)
        }
    }
}

/** Encabezado de sección con acción opcional. */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction, modifier = Modifier.height(Heights.button)) {
                Text(actionLabel, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

enum class StatusTone { Neutral, Primary, Success, Warning, Error }

/** Chip de estado unificado (badges de propiedad, visitas, solicitudes, acuerdos). */
@Composable
fun StatusChip(
    label: String,
    modifier: Modifier = Modifier,
    tone: StatusTone = StatusTone.Neutral,
) {
    val colors = MaterialTheme.colorScheme
    val extra = MaterialTheme.theHouseColors
    val container = when (tone) {
        StatusTone.Neutral -> colors.surfaceContainerHigh
        StatusTone.Primary -> colors.primaryContainer
        StatusTone.Success -> extra.successContainer
        StatusTone.Warning -> extra.warningContainer
        StatusTone.Error -> colors.errorContainer
    }
    val content = when (tone) {
        StatusTone.Neutral -> colors.onSurfaceVariant
        StatusTone.Primary -> colors.onPrimaryContainer
        StatusTone.Success -> extra.onSuccessContainer
        StatusTone.Warning -> extra.onWarningContainer
        StatusTone.Error -> colors.onErrorContainer
    }
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraSmall,
        color = container,
        contentColor = content,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
        )
    }
}

/** Chip informativo con icono (amenidades, filtros, metadatos). */
@Composable
fun InfoChip(
    label: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraSmall,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(IconSizes.sm))
            }
            Text(text = label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

/** Mensaje en línea (errores, avisos, confirmaciones) dentro de una pantalla. */
@Composable
fun InlineMessage(
    text: String,
    modifier: Modifier = Modifier,
    tone: StatusTone = StatusTone.Error,
) {
    val colors = MaterialTheme.colorScheme
    val extra = MaterialTheme.theHouseColors
    val container = when (tone) {
        StatusTone.Neutral -> colors.surfaceContainerHigh
        StatusTone.Primary -> colors.primaryContainer
        StatusTone.Success -> extra.successContainer
        StatusTone.Warning -> extra.warningContainer
        StatusTone.Error -> colors.errorContainer
    }
    val content = when (tone) {
        StatusTone.Neutral -> colors.onSurfaceVariant
        StatusTone.Primary -> colors.onPrimaryContainer
        StatusTone.Success -> extra.onSuccessContainer
        StatusTone.Warning -> extra.onWarningContainer
        StatusTone.Error -> colors.onErrorContainer
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = container,
        contentColor = content,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
        )
    }
}

/** Avatar con iniciales; no usa imágenes externas para evitar URLs inventadas. */
@Composable
fun Avatar(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = Heights.avatar,
) {
    val initials = name.trim()
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifBlank { "TH" }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

/** Fila de dato con etiqueta y valor; usada en detalles y resúmenes. */
@Composable
fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor,
            modifier = Modifier.weight(1.4f),
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
        )
    }
}

/** Tarjeta de estadística para dashboards (sin cálculos propios). */
@Composable
fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    icon: ImageVector? = null,
) {
    TheHouseCard(
        modifier = modifier,
        contentPadding = PaddingValues(Spacing.md),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(IconSizes.md),
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!supporting.isNullOrBlank()) {
                    Text(
                        text = supporting,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** Calificación simple y accesible. */
@Composable
fun RatingView(
    average: Double,
    count: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.Star,
            contentDescription = null,
            tint = MaterialTheme.theHouseColors.rating,
            modifier = Modifier.size(IconSizes.sm),
        )
        Text(
            text = String.format(java.util.Locale.US, "%.1f", average),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "($count)",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Contenedor de acciones secundarias alineadas. */
@Composable
fun ActionRow(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/** Radio visual usado por tarjetas destacadas. */
val HeroShape = androidx.compose.foundation.shape.RoundedCornerShape(Radii.xxl)
