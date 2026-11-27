package com.thehouse.app.presentation.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.thehouse.app.domain.model.Favorite
import com.thehouse.app.presentation.common.UiError
import com.thehouse.app.presentation.common.UiErrorMessages
import com.thehouse.app.presentation.components.EmptyStateView
import com.thehouse.app.presentation.components.ErrorStateView
import com.thehouse.app.presentation.components.InfoChip
import com.thehouse.app.presentation.components.LoadingView
import com.thehouse.app.presentation.components.OfflineStatusBanner
import com.thehouse.app.presentation.components.StatusChip
import com.thehouse.app.presentation.components.StatusTone
import com.thehouse.app.presentation.components.TheHouseCard
import com.thehouse.app.presentation.components.TheHouseHeader
import com.thehouse.app.ui.theme.IconSizes
import com.thehouse.app.ui.theme.Spacing

/** Favoritos reales D20: habitaciones guardadas, identidad = roomId. */
@Composable
fun FavoritesScreen(
    isAuthenticated: Boolean,
    isLoading: Boolean,
    items: List<Favorite>,
    error: UiError?,
    removingId: String?,
    onLoginClick: () -> Unit,
    onExploreClick: () -> Unit,
    onRemove: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    isOnline: Boolean = true,
    onRoomClick: (String) -> Unit = {},
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Spacing.xl, vertical = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            TheHouseHeader(
                title = "Favoritos",
                subtitle = "Las habitaciones que guardaste para comparar.",
                modifier = Modifier.semantics { contentDescription = "Título Favoritos" },
            )
            if (!isOnline) {
                OfflineStatusBanner(
                    text = if (isAuthenticated && items.isNotEmpty() && error == null) {
                        "Sin conexión · Mostrando información guardada"
                    } else "Sin conexión",
                )
            }
            when {
                !isAuthenticated -> EmptyStateView(
                    title = "Inicia sesión",
                    message = "Necesitas iniciar sesión para ver tus favoritos.",
                    modifier = Modifier.weight(1f),
                    ctaLabel = "Iniciar sesión",
                    onCtaClick = onLoginClick,
                    icon = Icons.Outlined.FavoriteBorder,
                )
                isLoading -> LoadingView(message = "Cargando tus favoritos…", modifier = Modifier.weight(1f))
                error != null -> ErrorStateView(
                    title = "Algo salió mal",
                    message = UiErrorMessages.map(error),
                    modifier = Modifier.weight(1f),
                    onRetry = onRetry,
                )
                items.isEmpty() -> EmptyStateView(
                    title = "Aún no tienes favoritos",
                    message = "Explora alojamientos y guarda los que te interesen.",
                    modifier = Modifier.weight(1f),
                    ctaLabel = "Explorar alojamientos",
                    onCtaClick = onExploreClick,
                    icon = Icons.Outlined.FavoriteBorder,
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    items(items, key = { it.roomId }) { favorite ->
                        FavoriteCard(
                            favorite = favorite,
                            removing = removingId == favorite.roomId,
                            onRemove = { onRemove(favorite.roomId) },
                            onClick = { onRoomClick(favorite.roomId) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoriteCard(
    favorite: Favorite,
    removing: Boolean,
    onRemove: () -> Unit,
    onClick: () -> Unit,
) {
    TheHouseCard(onClick = onClick) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Text(
                        text = favorite.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                    )
                    favorite.city?.takeIf { it.isNotBlank() }?.let { city ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(IconSizes.sm),
                            )
                            Text(
                                text = city,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    favorite.price?.let { price ->
                        Text(
                            text = "$$price ${favorite.currency.orEmpty()}".trim(),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    if (favorite.demo) {
                        StatusChip(label = "Demo", tone = StatusTone.Neutral)
                    }
                }
            }
            val details = buildString {
                favorite.roomType?.let { append(it) }
                favorite.bathroom?.let { if (isNotEmpty()) append(" · "); append(it) }
            }
            if (details.isNotBlank()) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    InfoChip(label = details)
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            TextButton(onClick = onRemove, enabled = !removing, modifier = Modifier.align(Alignment.End)) {
                if (removing) {
                    CircularProgressIndicator(modifier = Modifier.size(IconSizes.sm), strokeWidth = 2.dp)
                } else {
                    Text(text = "Quitar de favoritos", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
