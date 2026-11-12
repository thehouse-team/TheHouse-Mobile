package com.thehouse.app.presentation.landlord

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thehouse.app.domain.model.Building
import com.thehouse.app.domain.model.Room
import com.thehouse.app.domain.model.RoomStatus
import com.thehouse.app.presentation.components.ErrorStateView
import com.thehouse.app.presentation.components.LoadingView
import com.thehouse.app.ui.theme.Heights
import com.thehouse.app.ui.theme.Spacing

@Composable
fun BuildingDetailDestination(
    buildingId: String,
    factory: com.thehouse.app.presentation.U2ViewModelFactory,
    onBack: () -> Unit,
    onEditBuilding: (String) -> Unit,
    onAddRooms: (String) -> Unit,
    onEditRoom: (String, String) -> Unit,
    onDeleted: () -> Unit,
    onOpenLandlordRequests: () -> Unit,
) {
    val detailFactory = remember(buildingId) { factory.buildingDetail(buildingId) }
    val viewModel: BuildingDetailViewModel = viewModel(factory = detailFactory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }
    LaunchedEffect(state.deleted) {
        if (state.deleted) onDeleted()
    }

    BuildingDetailScreen(
        state = state,
        onBack = onBack,
        onEditBuilding = { onEditBuilding(buildingId) },
        onAddRooms = { onAddRooms(buildingId) },
        onEditRoom = { roomId -> onEditRoom(buildingId, roomId) },
        onRetry = { viewModel.onEvent(BuildingDetailEvent.Retry) },
        onPublish = { viewModel.onEvent(BuildingDetailEvent.Publish) },
        onDelete = { showDeleteDialog = true },
        onPauseRoom = { viewModel.onEvent(BuildingDetailEvent.PauseRoom(it)) },
        onResumeRoom = { viewModel.onEvent(BuildingDetailEvent.ResumeRoom(it)) },
        onOpenLandlordRequests = onOpenLandlordRequests,
    )

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar edificio") },
            text = { Text("Se eliminarán el edificio y sus cuartos. Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.onEvent(BuildingDetailEvent.Delete)
                    },
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") }
            },
        )
    }
}

@Composable
fun BuildingDetailScreen(
    state: BuildingDetailUiState,
    onBack: () -> Unit,
    onEditBuilding: () -> Unit,
    onAddRooms: () -> Unit,
    onEditRoom: (String) -> Unit,
    onRetry: () -> Unit,
    onPublish: () -> Unit,
    onDelete: () -> Unit,
    onPauseRoom: (String) -> Unit,
    onResumeRoom: (String) -> Unit,
    onOpenLandlordRequests: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val building = state.building
    Scaffold(modifier = modifier) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            TextButton(onClick = onBack, modifier = Modifier.heightIn(min = Heights.button)) {
                Text("Volver")
            }

            when {
                state.isLoading -> LoadingView(
                    message = "Cargando edificio…",
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
                building == null -> ErrorStateView(
                    title = "No se pudo cargar el edificio",
                    message = state.errorMessage ?: "Intenta de nuevo.",
                    onRetry = onRetry,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
                else -> LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    item {
                        Text(
                            building.name,
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.semantics { heading() },
                        )
                        Text(
                            "Estado: ${LandlordErrorMessages.statusLabel(building.status.api)} · " +
                                "${building.roomsAvailable}/${building.roomsTotal} disponibles · " +
                                "a ${building.distanceToUniversityKm} km de " +
                                building.nearestUniversity.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            OutlinedButton(
                                onClick = onEditBuilding,
                                modifier = Modifier.weight(1f).heightIn(min = Heights.button),
                            ) { Text("Editar edificio") }
                            OutlinedButton(
                                onClick = onAddRooms,
                                modifier = Modifier.weight(1f).heightIn(min = Heights.button),
                            ) { Text("Agregar cuartos") }
                        }
                    }
                    item {
                        PublicationCard(
                            building = building,
                            publication = state.publication,
                            isPublishing = state.isPublishing,
                            onPublish = onPublish,
                        )
                    }
                    item {
                        state.infoMessage?.let { message ->
                            Text(
                                message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        if (state.errorMessage != null) {
                            Text(
                                state.errorMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                            )
                            if (state.versionConflict) {
                                Button(
                                    onClick = onRetry,
                                    modifier = Modifier.fillMaxWidth().heightIn(min = Heights.button),
                                ) { Text("Recargar datos") }
                            }
                        }
                    }
                    item {
                        Text("Cuartos", style = MaterialTheme.typography.titleMedium)
                    }
                    if (state.rooms.isEmpty()) {
                        item {
                            Text(
                                "Aún no hay cuartos. Usa «Agregar cuartos».",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        items(state.rooms, key = { it.id }) { room ->
                            RoomRow(
                                room = room,
                                isBusy = state.busyRoomId == room.id,
                                onClick = { onEditRoom(room.id) },
                                onPause = { onPauseRoom(room.id) },
                                onResume = { onResumeRoom(room.id) },
                            )
                        }
                    }
                    item {
                        OutlinedButton(
                            onClick = onOpenLandlordRequests,
                            modifier = Modifier.fillMaxWidth().heightIn(min = Heights.button),
                        ) { Text("Solicitudes de mis cuartos") }
                    }
                    item {
                        Button(
                            onClick = onDelete,
                            enabled = !state.isDeleting,
                            modifier = Modifier.fillMaxWidth().heightIn(min = Heights.button),
                        ) {
                            Text(
                                if (state.isDeleting) "Eliminando…" else "Eliminar edificio",
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PublicationCard(
    building: Building,
    publication: com.thehouse.app.domain.model.PublicationStatus?,
    isPublishing: Boolean,
    onPublish: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text("Publicación", style = MaterialTheme.typography.titleMedium)
        if (publication != null) {
            publication.steps.forEach { step ->
                Text(
                    "${if (step.done) "✓" else "○"} ${stepLabel(step.id)}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        when (building.status) {
            com.thehouse.app.domain.model.BuildingStatus.IN_REVIEW ->
                Text(
                    "Tu publicación está en revisión.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            com.thehouse.app.domain.model.BuildingStatus.VERIFIED ->
                Text("Verificado", style = MaterialTheme.typography.bodyMedium)
            com.thehouse.app.domain.model.BuildingStatus.DRAFT ->
                Button(
                    onClick = onPublish,
                    enabled = !isPublishing && publication?.canPublish == true,
                    modifier = Modifier.fillMaxWidth().heightIn(min = Heights.button),
                ) {
                    Text(if (isPublishing) "Enviando…" else "Enviar a revisión")
                }
        }
    }
}

private fun stepLabel(id: String): String = when (id) {
    "building_data" -> "Datos del edificio"
    "location" -> "Ubicación"
    "services" -> "Servicios"
    "room_ready" -> "Al menos un cuarto listo"
    "documents" -> "Documentos"
    else -> id
}

@Composable
private fun RoomRow(
    room: Room,
    isBusy: Boolean,
    onClick: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Cuarto ${room.number} · Piso ${room.floor}",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    "MXN ${room.price} · ${room.sizeM2} m² · " +
                        LandlordErrorMessages.statusLabel(room.status.api),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(
                onClick = onClick,
                modifier = Modifier.heightIn(min = Heights.button),
            ) { Text("Editar") }
        }
        when (room.status) {
            RoomStatus.AVAILABLE -> OutlinedButton(
                onClick = onPause,
                enabled = !isBusy,
                modifier = Modifier.heightIn(min = Heights.button),
            ) { Text(if (isBusy) "…" else "Pausar") }
            RoomStatus.PAUSED -> OutlinedButton(
                onClick = onResume,
                enabled = !isBusy,
                modifier = Modifier.heightIn(min = Heights.button),
            ) { Text(if (isBusy) "…" else "Reanudar") }
            else -> Unit
        }
    }
}
