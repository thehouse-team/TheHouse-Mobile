package com.thehouse.app.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.thehouse.app.domain.room.RoomSearchCatalog
import com.thehouse.app.domain.room.RoomSearchItem
import com.thehouse.app.ui.theme.Elevations
import com.thehouse.app.ui.theme.IconSizes
import com.thehouse.app.ui.theme.Spacing

/**
 * Tarjeta de habitación REAL compartida por Home y Explore (D20).
 * Muestra SOLO campos contractuales de `RoomSearchItem`; nunca muestra 0 hab/baño/m²
 * como sustituto de datos ausentes. `roomId` es la identidad de navegación.
 */
@Composable
fun RoomCard(
    room: RoomSearchItem,
    onRoomClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier,
    isFavorite: Boolean = false,
) {
    Surface(
        onClick = onRoomClick,
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Alojamiento ${room.title}" },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
        shadowElevation = Elevations.card,
    ) {
        Column {
            Box {
                TheHouseImage(
                    url = room.thumbnailUrl,
                    title = room.title,
                    perfOperation = "image.load.room_card",
                    modifier = Modifier.fillMaxWidth().height(ImageHeight),
                )
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.94f),
                    modifier = Modifier.align(Alignment.TopEnd).padding(Spacing.sm),
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
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.94f),
                    modifier = Modifier.align(Alignment.BottomStart).padding(Spacing.sm),
                ) {
                    Text(
                        text = "$${room.price} ${room.currency}",
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
                    text = room.title,
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
                        text = "A ${room.walkingMinutes} min · ${room.distanceKm} km (aprox.)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = buildString {
                        append(RoomSearchCatalog.label(RoomSearchCatalog.Kind.BUILDING_TYPE, room.buildingType))
                        room.roomType?.let { append(" · ").append(RoomSearchCatalog.label(RoomSearchCatalog.Kind.ROOM_TYPE, it)) }
                        room.bathroom?.let { append(" · ").append(RoomSearchCatalog.label(RoomSearchCatalog.Kind.BATHROOM, it)) }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (room.services.isNotEmpty()) {
                    Text(
                        text = room.services.take(3).joinToString(" · "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
