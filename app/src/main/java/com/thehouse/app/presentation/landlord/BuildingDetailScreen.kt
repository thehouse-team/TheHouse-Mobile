package com.thehouse.app.presentation.landlord

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.thehouse.app.data.multimedia.MediaInspector
import com.thehouse.app.domain.model.Building
import com.thehouse.app.domain.model.Room
import com.thehouse.app.domain.model.RoomStatus
import com.thehouse.app.domain.multimedia.MediaValidation
import com.thehouse.app.domain.multimedia.MediaValidationResult
import com.thehouse.app.presentation.components.ErrorStateView
import com.thehouse.app.presentation.components.LoadingView
import com.thehouse.app.presentation.multimedia.MultimediaContracts
import com.thehouse.app.ui.theme.Heights
import com.thehouse.app.ui.theme.Spacing
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
        onPickIne = { localMessage = null; inePicker.launch(arrayOf("application/pdf", "image/jpeg", "image/png")) },
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
                        VerificationCard(state.verification)
                    }
                    item {
                        localMessage?.let { message ->
                            Text(
                                message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
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
            com.thehouse.app.domain.model.BuildingStatus.REJECTED ->
                Text(
                    "Tu publicación fue rechazada.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            com.thehouse.app.domain.model.BuildingStatus.SUSPENDED ->
                Text(
                    "Tu publicación está suspendida.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
        }
    }
}

/**
 * Tarjeta de verificación (NV-21 §6.1): estados reales y último motivo.
 * No revela quién revisó (el Backend tampoco lo envía).
 */
@Composable
private fun VerificationCard(
    verification: com.thehouse.app.domain.model.VerificationInfo?,
) {
    if (verification == null) return
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text("Verificación", style = MaterialTheme.typography.titleMedium)
        Text(
            "Estado: ${LandlordErrorMessages.statusLabel(verification.status.api)}",
            style = MaterialTheme.typography.bodyMedium,
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
                buildString {
                    append("Última decisión: $decisionLabel")
                    decision.reason?.let { append(" — $it") }
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
                if (room.photos.isNotEmpty()) {
                    Text(
                        "${room.photos.size} foto(s)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
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

@Composable
private fun BuildingPhotoCard(
    building: Building,
    isBusy: Boolean,
    onPick: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text("Foto del edificio", style = MaterialTheme.typography.titleMedium)
        val photo = building.photo
        if (photo != null) {
            AsyncImage(
                model = photo.thumbUrl.ifBlank { photo.url },
                contentDescription = "Foto del edificio ${building.name}",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(12.dp)),
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text("Sin foto del edificio", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            OutlinedButton(
                onClick = onPick,
                enabled = !isBusy,
                modifier = Modifier.weight(1f).heightIn(min = Heights.button),
            ) { Text(if (isBusy) "Subiendo…" else if (photo == null) "Subir foto" else "Reemplazar foto") }
            if (photo != null) {
                OutlinedButton(
                    onClick = onDelete,
                    enabled = !isBusy,
                    modifier = Modifier.weight(1f).heightIn(min = Heights.button),
                ) { Text(if (isBusy) "…" else "Eliminar foto") }
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
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text("Documentos", style = MaterialTheme.typography.titleMedium)
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
            "Solo se usan metadatos. No se muestra ni descarga el contenido del documento.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
        Text(
            if (document != null) "$label: cargado" else "$label: pendiente",
            style = MaterialTheme.typography.bodyMedium,
        )
        OutlinedButton(
            onClick = onPick,
            enabled = !isBusy,
            modifier = Modifier.fillMaxWidth().heightIn(min = Heights.button),
        ) {
            Text(
                when {
                    isBusy -> "Subiendo…"
                    document != null -> "Reemplazar $label"
                    else -> "Subir $label"
                },
            )
        }
    }
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
