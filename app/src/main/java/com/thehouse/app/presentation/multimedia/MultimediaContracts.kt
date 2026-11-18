package com.thehouse.app.presentation.multimedia

import android.net.Uri
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts

/**
 * Helpers para selección multimedia segura.
 * Preferir Photo Picker (sin permisos amplios de almacenamiento) sobre accesos directos al filesystem.
 */
object MultimediaContracts {

    fun pickSingleImageRequest(): PickVisualMediaRequest =
        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)

    fun pickSingleVisualMediaRequest(): PickVisualMediaRequest =
        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)

    /** Resultado seguro de selección: cancelado o URI. No expone rutas del filesystem. */
    sealed interface SelectionResult {
        data object Cancelled : SelectionResult
        data class Selected(val uri: Uri) : SelectionResult
    }

    fun fromPickerResult(uri: Uri?): SelectionResult =
        if (uri == null) SelectionResult.Cancelled else SelectionResult.Selected(uri)
}

/**
 * Política pura de permiso de cámara para uso futuro.
 * No debe invocarse si no existe función real de captura en la UI.
 */
object CameraPermissionPolicy {

    fun requiresRuntimePermission(): Boolean = true

    enum class Status { GRANTED, DENIED, PERMANENTLY_DENIED }

    fun resolveStatus(
        granted: Boolean,
        askedOnce: Boolean,
        showRationale: Boolean,
    ): Status = when {
        granted -> Status.GRANTED
        askedOnce && !showRationale -> Status.PERMANENTLY_DENIED
        else -> Status.DENIED
    }
}
