package com.thehouse.app.presentation.landlord

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.thehouse.app.data.multimedia.MediaInspector
import com.thehouse.app.domain.error.DomainError
import com.thehouse.app.domain.error.LandlordErrorCode
import com.thehouse.app.domain.error.LandlordException
import com.thehouse.app.domain.model.BathroomType
import com.thehouse.app.domain.model.Furniture
import com.thehouse.app.domain.model.MinContract
import com.thehouse.app.domain.model.RoomType
import com.thehouse.app.presentation.components.ErrorStateView
import com.thehouse.app.presentation.components.InlineMessage
import com.thehouse.app.presentation.components.LoadingView
import com.thehouse.app.presentation.components.StatusTone
import com.thehouse.app.presentation.components.TheHouseCard
import com.thehouse.app.presentation.components.TheHousePrimaryButton
import com.thehouse.app.presentation.components.TheHouseScreen
import com.thehouse.app.presentation.components.TheHouseSecondaryButton
import com.thehouse.app.presentation.components.TheHouseTextField
import com.thehouse.app.presentation.multimedia.MultimediaContracts
import com.thehouse.app.ui.theme.Spacing
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun RoomFormDestination(
    buildingId: String,
    roomId: String?,
    factory: com.thehouse.app.presentation.U2ViewModelFactory,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val formFactory = remember(buildingId, roomId) { factory.roomForm(buildingId, roomId) }
    val viewModel: RoomFormViewModel = viewModel(factory = formFactory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var localMessage by remember { mutableStateOf<String?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val prepared = prepareRoomPhoto(context, uri)
            if (prepared == null) {
                localMessage = "Selecciona una imagen JPEG, PNG o WebP de hasta 5 MB."
            } else {
                localMessage = null
                viewModel.onEvent(RoomFormEvent.UploadPhoto(prepared.first, prepared.second))
            }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            if (effect is RoomFormEffect.Saved || effect is RoomFormEffect.Deleted) onSaved()
        }
    }
    RoomFormScreen(
        state = state,
        localMessage = localMessage,
        onBack = onBack,
        onEvent = { viewModel.onEvent(it) },
        onReload = { viewModel.onEvent(RoomFormEvent.Retry) },
        onPickPhoto = {
            localMessage = null
            photoPicker.launch(MultimediaContracts.pickSingleImageRequest())
        },
    )
}

@Composable
fun RoomFormScreen(
    state: RoomFormUiState,
    localMessage: String?,
    onBack: () -> Unit,
    onEvent: (RoomFormEvent) -> Unit,
    onReload: () -> Unit,
    onPickPhoto: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TheHouseScreen(
        title = if (state.isEdit) "Editar cuarto" else "Agregar cuartos",
        subtitle = "Configura precio, tipo y servicios",
        onBack = onBack,
        modifier = modifier,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.xl, vertical = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            when {
                state.isLoading -> LoadingView(
                    message = "Cargando cuarto…",
                    modifier = Modifier.fillMaxWidth(),
                )
                state.versionConflict -> ErrorStateView(
                    title = "Conflicto de edición",
                    message = LandlordErrorMessages.map(
                        LandlordException(DomainError.Conflict, LandlordErrorCode.VERSION_CONFLICT),
                    ),
                    retryLabel = "Recargar datos",
                    onRetry = onReload,
                )
                else -> {
                    if (!state.isEdit) {
                        FormSection(title = "Generación automática") {
                            Text(
                                text = "Los cuartos se generan por piso con numeración " +
                                    "automática (101, 102, 201…).",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                                TheHouseTextField(
                                    value = state.floors,
                                    onValueChange = { onEvent(RoomFormEvent.FloorsChanged(it)) },
                                    label = "Pisos",
                                    isError = state.floorsError != null,
                                    errorMessage = state.floorsError,
                                    modifier = Modifier.weight(1f),
                                )
                                TheHouseTextField(
                                    value = state.roomsPerFloor,
                                    onValueChange = {
                                        onEvent(RoomFormEvent.RoomsPerFloorChanged(it))
                                    },
                                    label = "Cuartos por piso",
                                    isError = state.roomsPerFloorError != null,
                                    errorMessage = state.roomsPerFloorError,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    } else {
                        FormSection(title = "Identificación") {
                            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                                TheHouseTextField(
                                    value = state.number,
                                    onValueChange = { onEvent(RoomFormEvent.NumberChanged(it)) },
                                    label = "Número",
                                    modifier = Modifier.weight(1f),
                                )
                                TheHouseTextField(
                                    value = state.floor,
                                    onValueChange = { onEvent(RoomFormEvent.FloorChanged(it)) },
                                    label = "Piso",
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }

                    FormSection(title = "Precio y tamaño") {
                        TheHouseTextField(
                            value = state.price,
                            onValueChange = { onEvent(RoomFormEvent.PriceChanged(it)) },
                            label = "Renta mensual (MXN)",
                            isError = state.priceError != null,
                            errorMessage = state.priceError,
                        )
                        TheHouseTextField(
                            value = state.sizeM2,
                            onValueChange = { onEvent(RoomFormEvent.SizeChanged(it)) },
                            label = "Tamaño (m²)",
                            isError = state.sizeError != null,
                            errorMessage = state.sizeError,
                        )
                    }

                    FormSection(title = "Características") {
                        Text(
                            text = "Tipo de cuarto",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                            RoomType.entries.forEach { type ->
                                FilterChip(
                                    selected = state.type == type,
                                    onClick = { onEvent(RoomFormEvent.TypeChanged(type)) },
                                    label = { Text(typeLabel(type)) },
                                )
                            }
                        }
                        Text(
                            text = "Baño",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                            BathroomType.entries.forEach { bath ->
                                FilterChip(
                                    selected = state.bathroom == bath,
                                    onClick = { onEvent(RoomFormEvent.BathroomChanged(bath)) },
                                    label = {
                                        Text(
                                            if (bath == BathroomType.PRIVATE) "Privado" else "Compartido",
                                        )
                                    },
                                )
                            }
                        }
                        Text(
                            text = "Muebles",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Furniture.entries.chunked(2).forEach { rowItems ->
                            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                                rowItems.forEach { item ->
                                    FilterChip(
                                        selected = item in state.furniture,
                                        onClick = { onEvent(RoomFormEvent.FurnitureToggled(item)) },
                                        label = { Text(furnitureLabel(item)) },
                                    )
                                }
                            }
                        }
                    }

                    FormSection(title = "Disponibilidad") {
                        TheHouseTextField(
                            value = state.availableFrom,
                            onValueChange = {
                                onEvent(RoomFormEvent.AvailableFromChanged(it))
                            },
                            label = "Disponible desde (YYYY-MM-DD)",
                            isError = state.availableFromError != null,
                            errorMessage = state.availableFromError,
                        )
                        Text(
                            text = "Contrato mínimo",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                            MinContract.entries.forEach { contract ->
                                FilterChip(
                                    selected = state.minContract == contract,
                                    onClick = {
                                        onEvent(RoomFormEvent.MinContractChanged(contract))
                                    },
                                    label = { Text(contractLabel(contract)) },
                                )
                            }
                        }
                    }

                    if (state.isEdit) {
                        FormSection(title = "Fotos del cuarto") {
                            Text(
                                text = "Máximo 4 fotos. La primera es la principal; si no hay " +
                                    "fotos se usa la del edificio.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            state.photos.forEach { photo ->
                                AsyncImage(
                                    model = photo.thumbUrl.ifBlank { photo.url },
                                    contentDescription = "Foto del cuarto ${state.number}",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(160.dp)
                                        .clip(MaterialTheme.shapes.medium),
                                )
                                TheHouseSecondaryButton(
                                    text = if (state.isUploadingPhoto) {
                                        "…"
                                    } else {
                                        "Eliminar esta foto"
                                    },
                                    onClick = { onEvent(RoomFormEvent.DeletePhoto(photo.id)) },
                                    enabled = !state.isUploadingPhoto,
                                    contentColor = MaterialTheme.colorScheme.error,
                                )
                            }
                            TheHouseSecondaryButton(
                                text = when {
                                    state.isUploadingPhoto -> "Subiendo…"
                                    state.photos.size >= 4 -> "Límite de 4 fotos alcanzado"
                                    else -> "Agregar foto"
                                },
                                onClick = onPickPhoto,
                                enabled = !state.isUploadingPhoto && state.photos.size < 4,
                                loading = state.isUploadingPhoto,
                            )
                        }
                    }

                    localMessage?.let { message ->
                        InlineMessage(text = message)
                    }
                    state.infoMessage?.let { message ->
                        InlineMessage(text = message, tone = StatusTone.Success)
                    }
                    state.errorMessage?.let { message ->
                        InlineMessage(text = message)
                    }

                    TheHousePrimaryButton(
                        text = when {
                            state.isSaving -> "Guardando…"
                            state.isEdit -> "Guardar cambios"
                            else -> "Generar cuartos"
                        },
                        onClick = { onEvent(RoomFormEvent.Submit) },
                        enabled = !state.isLoading && state.isValid,
                        loading = state.isSaving,
                    )
                    if (state.isEdit) {
                        TheHouseSecondaryButton(
                            text = if (state.isSaving) "…" else "Eliminar cuarto",
                            onClick = { onEvent(RoomFormEvent.Delete) },
                            enabled = !state.isSaving,
                            contentColor = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FormSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    TheHouseCard {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            content()
        }
    }
}

private fun typeLabel(type: RoomType): String = when (type) {
    RoomType.SINGLE -> "Individual"
    RoomType.DOUBLE -> "Doble"
    RoomType.SHARED -> "Compartido"
}

private fun furnitureLabel(furniture: Furniture): String = when (furniture) {
    Furniture.BED -> "Cama"
    Furniture.DESK -> "Escritorio"
    Furniture.CHAIR -> "Silla"
    Furniture.CLOSET -> "Clóset"
    Furniture.DRESSER -> "Cómoda"
    Furniture.TV -> "TV"
    Furniture.FRIDGE -> "Refri"
    Furniture.MICROWAVE -> "Micro"
}

private fun contractLabel(contract: MinContract): String = when (contract) {
    MinContract.FOUR_MONTHS -> "4 meses"
    MinContract.SIX_MONTHS -> "6 meses"
    MinContract.ANNUAL -> "Anual"
}

private const val MAX_ROOM_PHOTO_BYTES = 5L * 1024L * 1024L
private val ALLOWED_ROOM_IMAGE_MIME = setOf("image/jpeg", "image/png", "image/webp")

private suspend fun prepareRoomPhoto(context: Context, uri: Uri): Pair<File, String>? =
    withContext(Dispatchers.IO) {
        val result = MediaInspector.sanitizeImageForUpload(
            context = context,
            sourceUri = uri,
            maxBytes = MAX_ROOM_PHOTO_BYTES,
            allowedMimeTypes = ALLOWED_ROOM_IMAGE_MIME,
        )
        when (result) {
            is com.thehouse.app.data.multimedia.PreparedMedia.Success ->
                result.sanitizedFile to result.mimeType
            else -> null
        }
    }
