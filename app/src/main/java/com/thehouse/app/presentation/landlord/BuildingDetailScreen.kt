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
import com.thehouse.app.domain.landlord.DocumentUploadPolicy
import com.thehouse.app.domain.landlord.LandlordFlowPolicies
import com.thehouse.app.domain.landlord.PublicationGuide
import com.thehouse.app.domain.landlord.publicationStepLabel
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
            when (val prepared = prepareDocument(context, uri)) {
                is DocumentPreparation.Success -> {
                    localMessage = null
                    viewModel.onEvent(
                        BuildingDetailEvent.UploadIne(prepared.file, prepared.mimeType),
                    )
                }
                is DocumentPreparation.Rejected -> localMessage = prepared.message
            }
        }
    }

    val propertyProofPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            when (val prepared = prepareDocument(context, uri)) {
                is DocumentPreparation.Success -> {
                    localMessage = null
                    viewModel.onEvent(
                        BuildingDetailEvent.UploadPropertyProof(prepared.file, prepared.mimeType),
                    )
                }
                is DocumentPreparation.Rejected -> localMessage = prepared.message
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
                        PublicationGuideCard(
                            guide = state.guide,
                            statusLabel = LandlordErrorMessages.statusLabel(building.status.api),
                        )
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
                        BuildingSummaryCard(building = building)
                    }
                    // 2. Fotografía del edificio.
                    item {
                        BuildingPhotoCard(
                            building = building,
                            isBusy = state.isUploadingPhoto,
                            onPick = onPickBuildingPhoto,
                            onDelete = onDeleteBuildingPhoto,
                        )
                    }
                    // 3. Agregar y configurar habitaciones (acciones + listado real).
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
                    // 4. Documentación de verificación.
                    item {
                        DocumentsCard(
                            documents = state.documents,
                            isBusy = state.isUploadingDocument,
                            onPickIne = onPickIne,
                            onPickPropertyProof = onPickPropertyProof,
                        )
                    }
                    state.verification?.let { verification ->
                        item {
                            VerificationCard(verification = verification)
                        }
                    }
                    // 5. Enviar a revisión (estado y motivo reales del Backend).
                    item {
                        PublicationCard(
                            building = building,
                            publication = state.publication,
                            guide = state.guide,
                            isPublishing = state.isPublishing,
                            onPublish = onPublish,
                        )
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
            LandlordFlowPolicies.distanceWarning(building.distanceToUniversityKm)?.let { warning ->
                InlineMessage(text = warning, tone = StatusTone.Warning)
            }
        }
    }
}

/**
 * Tarjeta guía: muestra el progreso calculado sobre los requisitos REALES del endpoint
 * `publication`, separando completos y pendientes. No inventa porcentajes: si el Backend
 * no devuelve pasos, muestra «Sin porcentaje disponible».
 */
@Composable
private fun PublicationGuideCard(
    guide: PublicationGuide,
    statusLabel: String,
    modifier: Modifier = Modifier,
) {
    TheHouseCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(
                text = "Cómo publicar tu alojamiento",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Sigue estos pasos en orden. El progreso se actualiza solo cuando el Backend confirma cada cambio.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            InfoRow(label = "Estado de publicación", value = statusLabel)
            InfoRow(
                label = "Progreso real",
                value = if (guide.totalCount == 0 || guide.progressPercent == null) {
                    "Sin porcentaje disponible"
                } else {
                    "${guide.completedCount} de ${guide.totalCount} requisitos" +
                        " · ${guide.progressPercent}%"
                },
            )
            guide.steps.forEachIndexed { index, step ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${index + 1}.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
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
                        text = publicationStepLabel(step.id),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    StatusChip(
                        label = if (step.done) "Completo" else "Pendiente",
                        tone = if (step.done) StatusTone.Success else StatusTone.Warning,
                    )
                }
            }
            guide.blockReason?.let { reason ->
                InlineMessage(text = reason, tone = StatusTone.Warning)
            }
        }
    }
}

@Composable
private fun PublicationCard(
    building: Building,
    publication: com.thehouse.app.domain.model.PublicationStatus?,
    guide: PublicationGuide,
    isPublishing: Boolean,
    onPublish: () -> Unit,
) {
    TheHouseCard {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(
                text = "Enviar a revisión",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            when (building.status) {
                com.thehouse.app.domain.model.BuildingStatus.IN_REVIEW ->
                    InlineMessage(
                        text = "Tu publicación está en revisión.",
                        tone = StatusTone.Warning,
                    )
                com.thehouse.app.domain.model.BuildingStatus.VERIFIED ->
                    InlineMessage(text = "Verificado", tone = StatusTone.Success)
                com.thehouse.app.domain.model.BuildingStatus.DRAFT -> {
                    if (!guide.canPublish) {
                        guide.blockReason?.let { reason -> InlineMessage(text = reason) }
                    }
                    TheHousePrimaryButton(
                        text = if (isPublishing) "Enviando…" else "Enviar a revisión",
                        onClick = onPublish,
                        enabled = !isPublishing && publication?.canPublish == true,
                        loading = isPublishing,
                    )
                }
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
                text = "Formatos aceptados: ${DocumentUploadPolicy.DISPLAY_FORMATS}. " +
                    "Tamaño máximo: ${DocumentUploadPolicy.DISPLAY_MAX_SIZE} por archivo. " +
                    "El formato se valida por su contenido real, no por el nombre del archivo.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
private val ALLOWED_IMAGE_MIME = setOf("image/jpeg", "image/png", "image/webp")

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

/** Resultado de la validación previa de un documento (antes de cualquier petición HTTP). */
private sealed interface DocumentPreparation {
    data class Success(val file: File, val mimeType: String) : DocumentPreparation
    data class Rejected(val message: String) : DocumentPreparation
}

/**
 * Copia el documento al caché privado de la app y valida tamaño yFIRMA DE BYTES real,
 * igual que el Backend. Así se evitan 413/415 evitables y no se expone contenido de la
 * identificación: el archivo se envía directamente al endpoint y se borra tras la subida.
 */
private suspend fun prepareDocument(context: Context, uri: Uri): DocumentPreparation =
    withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val declaredSize = resolver.openAssetFileDescriptor(uri, "r")?.use { afd ->
            afd.length.takeIf { it >= 0 }
        }
        if (declaredSize != null && declaredSize > DocumentUploadPolicy.MAX_DOCUMENT_BYTES) {
            return@withContext DocumentPreparation.Rejected(
                DocumentUploadPolicy.message(DocumentUploadPolicy.Validation.TooLarge),
            )
        }
        val bytes = runCatching {
            resolver.openInputStream(uri)?.use { it.readBytes() }
        }.getOrNull() ?: return@withContext DocumentPreparation.Rejected(
            DocumentUploadPolicy.message(DocumentUploadPolicy.Validation.EmptyOrUnreadable),
        )
        val validation = DocumentUploadPolicy.validate(bytes, declaredSize ?: bytes.size.toLong())
        if (validation != DocumentUploadPolicy.Validation.Valid) {
            return@withContext DocumentPreparation.Rejected(DocumentUploadPolicy.message(validation))
        }
        // El MIME se deriva de los bytes reales, nunca del ContentType declarado.
        val mime = DocumentUploadPolicy.detectMime(bytes)!!
        val outDir = File(context.cacheDir, "prepared_docs").apply { mkdirs() }
        val outFile = File(
            outDir,
            "doc_${System.currentTimeMillis()}.${DocumentUploadPolicy.extensionFor(mime)}",
        )
        val copied = runCatching {
            outFile.outputStream().use { it.write(bytes) }
            outFile.length()
        }.getOrDefault(-1L)
        if (copied <= 0L) {
            outFile.delete()
            DocumentPreparation.Rejected(
                DocumentUploadPolicy.message(DocumentUploadPolicy.Validation.EmptyOrUnreadable),
            )
        } else {
            DocumentPreparation.Success(outFile, mime)
        }
    }
