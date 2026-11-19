package com.thehouse.app.presentation.landlord

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.thehouse.app.data.multimedia.MediaInspector
import com.thehouse.app.domain.model.Building
import com.thehouse.app.domain.model.Room
import com.thehouse.app.domain.model.RoomStatus
import com.thehouse.app.presentation.components.ErrorStateView
import com.thehouse.app.presentation.components.InlineMessage
import com.thehouse.app.presentation.components.InfoRow
import com.thehouse.app.presentation.components.LoadingView
import com.thehouse.app.presentation.components.SectionHeader
import com.thehouse.app.presentation.components.StatusChip
import com.thehouse.app.presentation.components.StatusTone
import com.thehouse.app.presentation.components.TheHouseCard
import com.thehouse.app.presentation.components.TheHousePrimaryButton
import com.thehouse.app.presentation.components.TheHouseScreen
import com.thehouse.app.presentation.components.TheHouseSecondaryButton
import com.thehouse.app.presentation.components.TheHouseTextAction
import com.thehouse.app.presentation.multimedia.MultimediaContracts
import com.thehouse.app.ui.theme.Spacing
import com.thehouse.app.ui.theme.theHouseColors
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    var localMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val buildingPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val prepared = prepareBuildingPhoto(context, uri)
            if (prepared == null) {
                localMessage = "Selecciona una imagen JPEG, PNG o WebP de hasta 5 MB."
            } else {
                localMessage = null
                viewModel.onEvent(BuildingDetailEvent.UploadBuildingPhoto(prepared.first, prepared.second))
            }
        }
    }

    val inePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val prepared = prepareDocument(context, uri)
            if (prepared == null) {
                localMessage = "Selecciona un PDF, JPEG o PNG de hasta 10 MB."
            } else {
                localMessage = null
                viewModel.onEvent(BuildingDetailEvent.UploadIne(prepared.first, prepared.second))
            }
        }
    }

    val propertyProofPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val prepared = prepareDocument(context, uri)
            if (prepared == null) {
                localMessage = "Selecciona un PDF, JPEG o PNG de hasta 10 MB."
            } else {
                localMessage = null
                viewModel.onEvent(BuildingDetailEvent.UploadPropertyProof(prepared.first, prepared.second))
            }
        }
    }

    LaunchedEffect(state.deleted) {
        if (state.deleted) onDeleted()
    }

    BuildingDetailScreen(
        state = state,
        localMessage = localMessage,
        onBack = onBack,
        onEditBuilding = { onEditBuilding(buildingId) },
        onAddRooms = { onAddRooms(buildingId) },
        onEditRoom = { roomId -> onEditRoom(buildingId, roomId) },
        onRetry = { viewModel.onEvent(BuildingDetailEvent.Retry) },
        onPublish = { viewModel.onEvent(BuildingDetailEvent.Publish) },
        onDelete = { showDeleteDialog = true },
        onPauseRoom = { viewModel.onEvent(BuildingDetailEvent.PauseRoom(it)) },
        onResumeRoom = { viewModel.onEvent(BuildingDetailEvent.ResumeRoom(it)) },
        onPickBuildingPhoto = {
            localMessage = null
            buildingPhotoPicker.launch(MultimediaContracts.pickSingleImageRequest())
        },
        onDeleteBuildingPhoto = { viewModel.onEvent(BuildingDetailEvent.DeleteBuildingPhoto) },
        onPickIne = {
            localMessage = null
            inePicker.launch(arrayOf("application/pdf", "image/jpeg", "image/png"))
        },
        onPickPropertyProof = {
            localMessage = null
            propertyProofPicker.launch(arrayOf("application/pdf", "image/jpeg", "image/png"))
        },
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
                ) { Text("Eliminar") }
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
    localMessage: String?,
    onBack: () -> Unit,
    onEditBuilding: () -> Unit,
    onAddRooms: () -> Unit,
    onEditRoom: (String) -> Unit,
    onRetry: () -> Unit,
    onPublish: () -> Unit,
    onDelete: () -> Unit,
    onPauseRoom: (String) -> Unit,
    onResumeRoom: (String) -> Unit,
    onPickBuildingPhoto: () -> Unit,
    onDeleteBuildingPhoto: () -> Unit,
    onPickIne: () -> Unit,
    onPickPropertyProof: () -> Unit,
    onOpenLandlordRequests: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val building = state.building
    TheHouseScreen(
        title = building?.name ?: "Edificio",
        subtitle = building?.let {
            "${it.roomsAvailable}/${it.roomsTotal} cuartos disponibles"
        },
        onBack = onBack,
        modifier = modifier,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Spacing.xl, vertical = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            when {
                state.isLoading -> LoadingView(
                    message = "Cargando edificio…",
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
                building == null -> ErrorStateView(
                    title = "No se pudo cargar el edificio",
                    message = state.errorMessage ?: "Intenta de nuevo.",
                    onRetry = onRetry,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
                else -> LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Spacing.lg),
                    contentPadding = PaddingValues(bottom = Spacing.lg),
                ) {
                    item {
                        BuildingSummaryCard(building = building)
                    }
                    item {
                        BuildingPhotoCard(
                            building = building,
                            isBusy = state.isUploadingPhoto,
                            onPick = onPickBuildingPhoto,
                            onDelete = onDeleteBuildingPhoto,
                        )
                    }
                    item {
                        DocumentsCard(
                            documents = state.documents,
                            isBusy = state.isUploadingDocument,
                            onPickIne = onPickIne,
                            onPickPropertyProof = onPickPropertyProof,
                        )
                    }
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            TheHouseSecondaryButton(
                                text = "Editar edificio",
                                onClick = onEditBuilding,
                            )
                            TheHouseSecondaryButton(
                                text = "Agregar cuartos",
                                onClick = onAddRooms,
                            )
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
                    state.verification?.let { verification ->
                        item {
                            VerificationCard(verification = verification)
                        }
                    }
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            localMessage?.let { message ->
                                InlineMessage(text = message)
                            }
                            state.infoMessage?.let { message ->
                                InlineMessage(text = message, tone = StatusTone.Success)
                            }
                            if (state.errorMessage != null) {
                                InlineMessage(text = state.errorMessage)
                                if (state.versionConflict) {
                                    TheHousePrimaryButton(
                                        text = "Recargar datos",
                                        onClick = onRetry,
                                    )
                                }
                            }
                        }
                    }
                    item {
                        SectionHeader(
                            title = "Cuartos",
                            subtitle = if (state.rooms.size == 1) {
                                "1 registrado"
                            } else {
                                "${state.rooms.size} registrados"
                            },
                        )
                    }
                    if (state.rooms.isEmpty()) {
                        item {
                            InlineMessage(
                                text = "Aún no hay cuartos. Usa «Agregar cuartos».",
                                tone = StatusTone.Neutral,
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
                        TheHouseSecondaryButton(
                            text = "Solicitudes de mis cuartos",
                            onClick = onOpenLandlordRequests,
                        )
                    }
                    item {
                        TheHouseSecondaryButton(
                            text = if (state.isDeleting) "Eliminando…" else "Eliminar edificio",
                            onClick = onDelete,
                            enabled = !state.isDeleting,
                            loading = state.isDeleting,
                            contentColor = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BuildingSummaryCard(
    building: Building,
    modifier: Modifier = Modifier,
) {
    TheHouseCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = building.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                StatusChip(
                    label = LandlordErrorMessages.statusLabel(building.status.api),
                    tone = buildingTone(building.status.api),
                )
            }
            InfoRow(
                label = "Disponibles",
                value = "${building.roomsAvailable}/${building.roomsTotal}",
            )
            InfoRow(
                label = "Universidad cercana",
                value = "${building.nearestUniversity.name} · " +
                    "${building.distanceToUniversityKm} km",
            )
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
    TheHouseCard {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(
                text = "Publicación",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            publication?.steps?.forEach { step ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (step.done) "✓" else "○",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (step.done) {
                            MaterialTheme.theHouseColors.success
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                    Text(
                        text = stepLabel(step.id),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            when (building.status) {
                com.thehouse.app.domain.model.BuildingStatus.IN_REVIEW ->
                    InlineMessage(
                        text = "Tu publicación está en revisión.",
                        tone = StatusTone.Warning,
                    )
                com.thehouse.app.domain.model.BuildingStatus.VERIFIED ->
                    InlineMessage(text = "Verificado", tone = StatusTone.Success)
                com.thehouse.app.domain.model.BuildingStatus.DRAFT ->
                    TheHousePrimaryButton(
                        text = if (isPublishing) "Enviando…" else "Enviar a revisión",
                        onClick = onPublish,
                        enabled = !isPublishing && publication?.canPublish == true,
                        loading = isPublishing,
                    )
                com.thehouse.app.domain.model.BuildingStatus.REJECTED ->
                    InlineMessage(text = "Tu publicación fue rechazada.")
                com.thehouse.app.domain.model.BuildingStatus.SUSPENDED ->
                    InlineMessage(text = "Tu publicación está suspendida.")
            }
        }
    }
}

/**
 * Tarjeta de verificación (NV-21 §6.1): estados reales y último motivo.
 * No revela quién revisó (el Backend tampoco lo envía).
 */
@Composable
private fun VerificationCard(
    verification: com.thehouse.app.domain.model.VerificationInfo,
) {
    TheHouseCard {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(
                text = "Verificación",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            InfoRow(
                label = "Estado",
                value = LandlordErrorMessages.statusLabel(verification.status.api),
            )
            verification.lastDecision?.let { decision ->
                val decisionLabel = when (decision.decision) {
                    com.thehouse.app.domain.model.VerificationDecision.APPROVED ->
                        "Aprobada"
                    com.thehouse.app.domain.model.VerificationDecision.CHANGES_REQUESTED ->
                        "Se pidieron correcciones"
                    com.thehouse.app.domain.model.VerificationDecision.REJECTED ->
                        "Rechazada"
                }
                Text(
                    text = buildString {
                        append("Última decisión: $decisionLabel")
                        decision.reason?.let { append(" — $it") }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun stepLabel(id: String): String = when (id) {
    "building_data" -> "Datos del edificio"
    "location" -> "Ubicación"
    "services" -> "Servicios"
    "room_ready" -> "Al menos un cuarto listo"
    "building_photo" -> "Foto del edificio"
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
    TheHouseCard(
        onClick = onClick,
        contentPadding = PaddingValues(Spacing.md),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Cuarto ${room.number} · Piso ${room.floor}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "MXN ${room.price} · ${room.sizeM2} m²",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (room.photos.isNotEmpty()) {
                        Text(
                            text = "${room.photos.size} foto(s)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                StatusChip(
                    label = LandlordErrorMessages.statusLabel(room.status.api),
                    tone = roomTone(room.status),
                )
            }
            when (room.status) {
                RoomStatus.AVAILABLE -> TheHouseSecondaryButton(
                    text = if (isBusy) "Pausando…" else "Pausar",
                    onClick = onPause,
                    enabled = !isBusy,
                    loading = isBusy,
                )
                RoomStatus.PAUSED -> TheHouseSecondaryButton(
                    text = if (isBusy) "Reanudando…" else "Reanudar",
                    onClick = onResume,
                    enabled = !isBusy,
                    loading = isBusy,
                )
                else -> Unit
            }
            TheHouseTextAction(text = "Editar cuarto", onClick = onClick)
        }
    }
}

@Composable
private fun BuildingPhotoCard(
    building: Building,
    isBusy: Boolean,
    onPick: () -> Unit,
    onDelete: () -> Unit,
) {
    TheHouseCard {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(
                text = "Foto del edificio",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            val photo = building.photo
            if (photo != null) {
                AsyncImage(
                    model = photo.thumbUrl.ifBlank { photo.url },
                    contentDescription = "Foto del edificio ${building.name}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(MaterialTheme.shapes.medium),
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(MaterialTheme.shapes.medium),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Sin foto del edificio",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            TheHouseSecondaryButton(
                text = when {
                    isBusy -> "Subiendo…"
                    photo == null -> "Subir foto"
                    else -> "Reemplazar foto"
                },
                onClick = onPick,
                enabled = !isBusy,
                loading = isBusy,
            )
            if (photo != null) {
                TheHouseSecondaryButton(
                    text = "Eliminar foto",
                    onClick = onDelete,
                    enabled = !isBusy,
                    contentColor = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun DocumentsCard(
    documents: com.thehouse.app.domain.model.BuildingDocuments?,
    isBusy: Boolean,
    onPickIne: () -> Unit,
    onPickPropertyProof: () -> Unit,
) {
    TheHouseCard {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Text(
                text = "Documentos",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            DocumentRow(
                label = "INE",
                document = documents?.ine,
                isBusy = isBusy,
                onPick = onPickIne,
            )
            DocumentRow(
                label = "Comprobante de propiedad",
                document = documents?.propertyProof,
                isBusy = isBusy,
                onPick = onPickPropertyProof,
            )
            Text(
                text = "Solo se usan metadatos. No se muestra ni descarga el contenido del documento.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DocumentRow(
    label: String,
    document: com.thehouse.app.domain.model.BuildingDocument?,
    isBusy: Boolean,
    onPick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            StatusChip(
                label = if (document != null) "Cargado" else "Pendiente",
                tone = if (document != null) StatusTone.Success else StatusTone.Warning,
            )
        }
        TheHouseSecondaryButton(
            text = when {
                isBusy -> "Subiendo…"
                document != null -> "Reemplazar $label"
                else -> "Subir $label"
            },
            onClick = onPick,
            enabled = !isBusy,
            loading = isBusy,
        )
    }
}

private fun buildingTone(status: String): StatusTone = when (status) {
    "verified" -> StatusTone.Success
    "in_review", "draft" -> StatusTone.Warning
    "rejected", "suspended" -> StatusTone.Error
    else -> StatusTone.Neutral
}

private fun roomTone(status: RoomStatus): StatusTone = when (status) {
    RoomStatus.AVAILABLE -> StatusTone.Success
    RoomStatus.RENTED -> StatusTone.Primary
    RoomStatus.PAUSED -> StatusTone.Warning
    else -> StatusTone.Neutral
}

private const val MAX_BUILDING_PHOTO_BYTES = 5L * 1024L * 1024L
private const val MAX_DOCUMENT_BYTES = 10L * 1024L * 1024L
private val ALLOWED_IMAGE_MIME = setOf("image/jpeg", "image/png", "image/webp")
private val ALLOWED_DOCUMENT_MIME = setOf("application/pdf", "image/jpeg", "image/png")

private suspend fun prepareBuildingPhoto(context: Context, uri: Uri): Pair<File, String>? =
    withContext(Dispatchers.IO) {
        val result = MediaInspector.sanitizeImageForUpload(
            context = context,
            sourceUri = uri,
            maxBytes = MAX_BUILDING_PHOTO_BYTES,
            allowedMimeTypes = ALLOWED_IMAGE_MIME,
        )
        when (result) {
            is com.thehouse.app.data.multimedia.PreparedMedia.Success ->
                result.sanitizedFile to result.mimeType
            else -> null
        }
    }

private suspend fun prepareDocument(context: Context, uri: Uri): Pair<File, String>? =
    withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val mime = resolver.getType(uri)?.lowercase()?.trim()
        if (mime == null || mime !in ALLOWED_DOCUMENT_MIME) return@withContext null
        val size = resolver.openAssetFileDescriptor(uri, "r")?.use { afd ->
            val len = afd.length
            if (len >= 0) len else null
        } ?: resolver.openInputStream(uri)?.use { input ->
            var total = 0L
            val buf = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buf)
                if (read <= 0) break
                total += read
                if (total > MAX_DOCUMENT_BYTES) return@use total
            }
            total
        } ?: return@withContext null
        if (size <= 0L || size > MAX_DOCUMENT_BYTES) return@withContext null
        val outDir = File(context.cacheDir, "prepared_docs").apply { mkdirs() }
        val ext = when (mime) {
            "application/pdf" -> "pdf"
            "image/png" -> "png"
            else -> "jpg"
        }
        val outFile = File(outDir, "doc_${System.currentTimeMillis()}.$ext")
        val copied = runCatching {
            resolver.openInputStream(uri)?.use { input ->
                outFile.outputStream().use { output -> input.copyTo(output) }
            } ?: -1L
        }.getOrDefault(-1L)
        if (copied <= 0L) {
            outFile.delete()
            null
        } else {
            outFile to mime
        }
    }
