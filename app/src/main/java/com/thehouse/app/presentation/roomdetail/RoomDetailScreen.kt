package com.thehouse.app.presentation.roomdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModelProvider
import com.thehouse.app.domain.room.RoomDetail
import com.thehouse.app.domain.room.RoomSearchCatalog
import com.thehouse.app.presentation.common.UiErrorMessages
import com.thehouse.app.presentation.components.EmptyStateView
import com.thehouse.app.presentation.components.ErrorStateView
import com.thehouse.app.presentation.components.LoadingView
import com.thehouse.app.presentation.components.TheHouseCard
import com.thehouse.app.presentation.components.TheHouseHeader
import com.thehouse.app.presentation.components.TheHousePrimaryButton
import com.thehouse.app.presentation.components.TheHouseImage
import com.thehouse.app.presentation.components.TheHouseImagePlaceholder
import com.thehouse.app.presentation.components.TheHouseSecondaryButton
import com.thehouse.app.ui.theme.IconSizes
import com.thehouse.app.ui.theme.Spacing

/** D18 + D20: detalle real del cuarto con fotos, campos contractuales y acciones reales. */
@Composable
fun RoomDetailScreen(
    roomId: String,
    factory: ViewModelProvider.Factory,
    onBack: () -> Unit,
    onRequireLogin: () -> Unit,
    modifier: Modifier = Modifier,
    onContact: (String) -> Unit = {},
    onScheduleVisit: (String) -> Unit = {},
) {
    val vm: RoomDetailViewModel = viewModel(factory = factory)
    val state by vm.uiState.collectAsStateWithLifecycle()

    Scaffold(modifier = modifier, containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Spacing.xl, vertical = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            TheHouseHeader(
                title = "Detalle del cuarto",
                subtitle = "Información publicada del cuarto",
                modifier = Modifier.semantics { contentDescription = "Título detalle de cuarto" },
            )
            when (val s = state) {
                RoomDetailUiState.Loading -> LoadingView(
                    message = "Cargando detalle del cuarto…",
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
                is RoomDetailUiState.Content -> RoomDetailContent(
                    room = s.room,
                    isFavorite = s.isFavorite,
                    onFavorite = { vm.onEvent(RoomDetailEvent.FavoriteClicked) },
                    onContact = { onContact(roomId) },
                    onScheduleVisit = { onScheduleVisit(roomId) },
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
                RoomDetailUiState.NotFound -> EmptyStateView(
                    title = "Cuarto no encontrado",
                    message = "El cuarto ya no está disponible.",
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    ctaLabel = "Volver",
                    onCtaClick = onBack,
                )
                RoomDetailUiState.RequiresAuth -> EmptyStateView(
                    title = "Inicia sesión",
                    message = "Necesitas iniciar sesión para ver el detalle del cuarto.",
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    ctaLabel = "Iniciar sesión",
                    onCtaClick = onRequireLogin,
                )
                is RoomDetailUiState.Error -> ErrorStateView(
                    title = "No pudimos cargar el cuarto",
                    message = UiErrorMessages.map(s.error),
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    onRetry = { vm.onEvent(RoomDetailEvent.Retry) },
                )
            }
        }
    }
}

@Composable
private fun RoomDetailContent(
    room: RoomDetail,
    isFavorite: Boolean,
    onFavorite: () -> Unit,
    onContact: () -> Unit,
    onScheduleVisit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        item {
            RoomPhotos(room)
        }
        item {
            TheHouseCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(Spacing.md)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Text(room.title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                    androidx.compose.material3.IconButton(onClick = onFavorite) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = if (isFavorite) "Quitar favorito" else "Guardar favorito",
                            tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = "$${room.price} ${room.currency} / mes",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                room.roomType?.let {
                    Text(
                        "Tipo de cuarto: ${RoomSearchCatalog.label(RoomSearchCatalog.Kind.ROOM_TYPE, it)}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                room.bathroom?.let {
                    Text(
                        "Baño: ${RoomSearchCatalog.label(RoomSearchCatalog.Kind.BATHROOM, it)}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                room.sizeM2?.let { Text("Tamaño: $it m²", style = MaterialTheme.typography.bodyMedium) }
                Text("Distancia: ${room.distanceKm} km · ${room.walkingMinutes} min a pie (aprox.)")
                Text("Ubicación aproximada: ${room.approxLocation.lat}, ${room.approxLocation.lng}")
            }
        }
        // Acciones reales (D20): enlazadas a solicitudes y visitas, sin "próximamente".
        item {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                TheHousePrimaryButton(text = "Contactar", onClick = onContact)
                TheHouseSecondaryButton(text = "Agendar visita", onClick = onScheduleVisit)
            }
        }
        item {
            TheHouseCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(Spacing.md)) {
                Text("Edificio", style = MaterialTheme.typography.titleMedium)
                Text(room.building.name)
                Text(
                    "Tipo: ${RoomSearchCatalog.label(RoomSearchCatalog.Kind.BUILDING_TYPE, room.building.type)} · " +
                        "Inquilinos: ${RoomSearchCatalog.label(RoomSearchCatalog.Kind.ALLOWED_TENANTS, room.building.allowedTenants)}",
                )
                room.nearestUniversity?.let { Text("Universidad cercana: ${it.name}") }
                room.building.rules?.let { Text("Reglas: $it") }
            }
        }
        if (room.furniture.isNotEmpty() || room.building.services.isNotEmpty()) {
            item {
                TheHouseCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(Spacing.md)) {
                    Text("Servicios y mobiliario", style = MaterialTheme.typography.titleMedium)
                    room.building.services.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                    room.furniture.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
        if (room.availableFrom != null || room.minContract != null) {
            item {
                TheHouseCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(Spacing.md)) {
                    Text("Disponibilidad", style = MaterialTheme.typography.titleMedium)
                    room.availableFrom?.let { Text("Desde: $it") }
                    room.minContract?.let { Text("Contrato mínimo: $it") }
                }
            }
        }
    }
}

@Composable
private fun RoomPhotos(room: RoomDetail) {
    val urls = buildList {
        room.thumbnailUrl?.takeIf { it.isNotBlank() }?.let(::add)
        room.photos.forEach { photo ->
            photo.url.takeIf { it.isNotBlank() }?.let(::add)
                ?: photo.thumbUrl.takeIf { it.isNotBlank() }?.let(::add)
        }
    }.distinct().take(4)

    if (urls.isEmpty()) {
        // Sin fotografías: placeholder compacto, sin reservar altura excesiva.
        TheHouseImagePlaceholder(
            title = room.title,
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .clip(RoundedCornerShape(16.dp)),
        )
        return
    }

    // Galería compacta: una foto principal y hasta tres miniaturas. Cada
    // elemento muestra el placeholder si falla, sin dejar contenedores vacíos.
    val primary = urls.first()
    TheHouseImage(
        url = primary,
        title = room.title,
        perfOperation = "image.load.room_detail",
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(16.dp)),
    )
    if (urls.size > 1) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            urls.drop(1).take(3).forEachIndexed { index, url ->
                TheHouseImage(
                    url = url,
                    title = "${room.title} (${index + 2})",
                    perfOperation = "image.load.room_detail_thumb",
                    modifier = Modifier
                        .weight(1f)
                        .height(72.dp)
                        .clip(RoundedCornerShape(12.dp)),
                )
            }
        }
    }
}
