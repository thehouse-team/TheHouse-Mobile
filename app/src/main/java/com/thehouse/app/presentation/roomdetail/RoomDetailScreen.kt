package com.thehouse.app.presentation.roomdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.thehouse.app.domain.model.GeoPoint
import com.thehouse.app.domain.model.RoomBuilding
import com.thehouse.app.domain.model.RoomDetail
import com.thehouse.app.domain.model.RoomUniversity
import com.thehouse.app.presentation.common.UiError
import com.thehouse.app.presentation.common.UiErrorMessages
import com.thehouse.app.presentation.components.SearchLabels
import com.thehouse.app.presentation.components.formatDistance

@Composable
fun RoomDetailDestination(
    roomId: String,
    factory: ViewModelProvider.Factory,
    onBack: () -> Unit,
    onRequireLogin: () -> Unit,
) {
    val viewModel: RoomDetailViewModel = viewModel(
        key = "room-detail:$roomId",
        factory = factory,
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel, onRequireLogin) {
        viewModel.effects.collect { effect ->
            when (effect) {
                RoomDetailEffect.RequireLogin -> onRequireLogin()
            }
        }
    }

    RoomDetailScreen(
        state = state,
        onBack = onBack,
        onRetry = { viewModel.onEvent(RoomDetailEvent.Retry) },
    )
}

/** UI stateless: no red, DTO, sesión ni lógica de negocio. */
@Composable
fun RoomDetailScreen(
    state: RoomDetailUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            Row {
                TextButton(
                    onClick = onBack,
                    modifier = Modifier.padding(horizontal = 16.dp).heightIn(min = 48.dp),
                ) {
                    Text("Volver")
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text(
                    "Detalle del cuarto",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.semantics { heading() },
                )
            }
            when (state) {
                RoomDetailUiState.Loading -> item {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .testTag("room_detail_loading")
                            .semantics { contentDescription = "Cargando cuarto" },
                    )
                }
                RoomDetailUiState.NotFound -> item {
                    DetailFailure(
                        message = "Cuarto no encontrado.",
                        onRetry = onRetry,
                        tag = "room_detail_not_found",
                    )
                }
                is RoomDetailUiState.Error -> item {
                    DetailFailure(
                        message = UiErrorMessages.map(state.error),
                        onRetry = onRetry,
                        tag = "room_detail_error",
                    )
                }
                is RoomDetailUiState.Content -> {
                    item { RoomImage(state.room) }
                    item { RoomInformation(state.room) }
                    item { BuildingInformation(state.room) }
                }
            }
        }
    }
}

@Composable
private fun DetailFailure(message: String, onRetry: () -> Unit, tag: String) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.testTag(tag).semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Text(message, style = MaterialTheme.typography.bodyLarge)
        OutlinedButton(onClick = onRetry, modifier = Modifier.heightIn(min = 48.dp)) {
            Text("Reintentar")
        }
    }
}

@Composable
private fun RoomImage(room: RoomDetail) {
    if (room.thumbnailUrl != null) {
        AsyncImage(
            model = room.thumbnailUrl,
            contentDescription = "Imagen de ${room.title}",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(16.dp),
                )
                .testTag("room_image_placeholder"),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "Sin imagen disponible",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun RoomInformation(room: RoomDetail) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.testTag("room_detail_content"),
    ) {
        Text(room.title, style = MaterialTheme.typography.headlineMedium)
        Text(
            "$${room.price} ${room.currency}/mes",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            "A ${formatDistance(room.distanceKm)} km · ${room.walkingMinutes} min a pie",
            style = MaterialTheme.typography.bodyMedium,
        )
        val basics = listOfNotNull(
            SearchLabels.roomType(room.roomType),
            SearchLabels.bathroom(room.bathroom),
            room.sizeM2?.let { "$it m²" },
        )
        if (basics.isNotEmpty()) {
            Text(basics.joinToString(" · "), style = MaterialTheme.typography.bodyMedium)
        }
        if (room.demo) {
            Text(
                "Publicación de ejemplo",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val availability = listOfNotNull(
            room.availableFrom?.let { "Disponible desde $it" },
            SearchLabels.minContract(room.minContract)?.let { "Contrato mínimo: $it" },
        )
        availability.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
        if (room.furniture.isNotEmpty()) {
            Text("Mobiliario", style = MaterialTheme.typography.titleMedium)
            Text(
                room.furniture.joinToString(" · ") { SearchLabels.furniture(it) },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Text(
            "Ubicación aproximada. La dirección exacta no se muestra.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun BuildingInformation(room: RoomDetail) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.testTag("room_building_content"),
    ) {
        Text("Edificio", style = MaterialTheme.typography.titleMedium)
        Text(room.building.name, style = MaterialTheme.typography.bodyLarge)
        Text(
            SearchLabels.buildingType(room.building.type),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            "Pueden vivir: ${SearchLabels.allowedTenants(room.building.allowedTenants)}",
            style = MaterialTheme.typography.bodyMedium,
        )
        if (room.building.services.isNotEmpty()) {
            Text(
                "Servicios: " + room.building.services.joinToString(" · ") {
                    SearchLabels.service(it)
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        room.building.rules?.takeIf { it.isNotBlank() }?.let { rules ->
            Text("Reglas", style = MaterialTheme.typography.titleSmall)
            Text(rules, style = MaterialTheme.typography.bodyMedium)
        }
        room.nearestUniversity?.let { university ->
            Text(
                "Universidad cercana: ${university.name} (${university.code})",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

private val previewRoom = RoomDetail(
    id = "6578a8b8c29c000000000010",
    buildingId = "6578a8b8c29c000000000011",
    title = "Cuarto individual cerca del campus",
    price = 3500,
    currency = "MXN",
    roomType = "single",
    bathroom = "shared",
    sizeM2 = 14.0,
    furniture = listOf("bed", "desk", "closet"),
    availableFrom = "2026-12-01",
    minContract = "six_months",
    distanceKm = 1.2,
    walkingMinutes = 15,
    approxLocation = GeoPoint(latitude = 20.08, longitude = -98.36),
    nearestUniversity = RoomUniversity(
        id = "6578a8b8c29c000000000001",
        code = "UTTT",
        name = "Universidad Tecnológica de Tulancingo",
    ),
    building = RoomBuilding(
        id = "6578a8b8c29c000000000011",
        name = "Casa Universidad",
        type = "house_rooms",
        allowedTenants = "any",
        services = listOf("wifi", "water", "electricity"),
        rules = "No fiestas después de las 22:00.",
    ),
    thumbnailUrl = null,
    demo = true,
)

@Preview(showBackground = true)
@Composable
private fun RoomDetailContentPreview() {
    MaterialTheme {
        RoomDetailScreen(
            state = RoomDetailUiState.Content(previewRoom),
            onBack = {},
            onRetry = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RoomDetailErrorPreview() {
    MaterialTheme {
        RoomDetailScreen(
            state = RoomDetailUiState.Error(UiError.Network),
            onBack = {},
            onRetry = {},
        )
    }
}
