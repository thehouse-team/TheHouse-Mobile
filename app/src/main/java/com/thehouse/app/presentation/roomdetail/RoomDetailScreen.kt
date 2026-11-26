package com.thehouse.app.presentation.roomdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModelProvider
import com.thehouse.app.domain.room.RoomDetail
import com.thehouse.app.presentation.common.UiErrorMessages
import com.thehouse.app.presentation.components.EmptyStateView
import com.thehouse.app.presentation.components.ErrorStateView
import com.thehouse.app.presentation.components.LoadingView
import com.thehouse.app.presentation.components.TheHouseCard
import com.thehouse.app.presentation.components.TheHouseHeader
import com.thehouse.app.ui.theme.Spacing

/** D18: pantalla real del detalle de cuarto. Solo muestra campos del contrato. */
@Composable
fun RoomDetailScreen(
    roomId: String,
    factory: ViewModelProvider.Factory,
    onBack: () -> Unit,
    onRequireLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val vm: RoomDetailViewModel = viewModel(factory = factory)
    val state by vm.uiState.collectAsStateWithLifecycle()

    Scaffold(modifier = modifier, containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = Spacing.xl, vertical = Spacing.lg),
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

                is RoomDetailUiState.Content -> RoomDetailContent(s.room, Modifier.fillMaxWidth().weight(1f))

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
private fun RoomDetailContent(room: RoomDetail, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        item {
            TheHouseCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(Spacing.md)) {
                Text(room.title, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    "${room.price} ${room.currency}",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                room.roomType?.let { Text("Tipo: $it", style = MaterialTheme.typography.bodyMedium) }
                room.bathroom?.let { Text("Baño: $it", style = MaterialTheme.typography.bodyMedium) }
                room.sizeM2?.let { Text("Tamaño: $it m²", style = MaterialTheme.typography.bodyMedium) }
                Text("Distancia: ${room.distanceKm} km · ${room.walkingMinutes} min a pie")
            }
        }
        item {
            TheHouseCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(Spacing.md)) {
                Text("Edificio", style = MaterialTheme.typography.titleMedium)
                Text(room.building.name)
                Text("Tipo: ${room.building.type} · Inquilinos: ${room.building.allowedTenants}")
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
        if (room.photos.isNotEmpty()) {
            item { Text("Fotos (${room.photos.size})", style = MaterialTheme.typography.titleMedium) }
            items(room.photos, key = { it.id }) { photo ->
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(photo.id, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
