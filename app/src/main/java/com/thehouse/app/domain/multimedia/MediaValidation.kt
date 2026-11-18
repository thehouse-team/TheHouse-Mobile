package com.thehouse.app.domain.multimedia

/**
 * Validación local de multimedia antes de cualquier upload futuro.
 * No depende de Android ni de contratos de backend inventados.
 */
object MediaValidation {

    val DEFAULT_ALLOWED_IMAGE_MIME_TYPES: Set<String> = setOf(
        "image/jpeg",
        "image/png",
        "image/webp",
    )

    /** Límite conservador por defecto para imágenes cuando no existe contrato backend. */
    const val DEFAULT_MAX_IMAGE_BYTES: Long = 10L * 1024L * 1024L

    /** Límite defensivo para texto libre asociado a contenido/publicación/reporte. */
    const val DEFAULT_MAX_TEXT_LENGTH: Int = 2000

    fun validateImage(
        mimeType: String?,
        sizeBytes: Long?,
        maxBytes: Long = DEFAULT_MAX_IMAGE_BYTES,
        allowedMimeTypes: Set<String> = DEFAULT_ALLOWED_IMAGE_MIME_TYPES,
    ): MediaValidationResult {
        val normalizedMime = mimeType?.trim()?.lowercase()
        if (normalizedMime.isNullOrBlank()) {
            return MediaValidationResult.Invalid(MediaValidationError.MissingMimeType)
        }
        if (normalizedMime !in allowedMimeTypes) {
            return MediaValidationResult.Invalid(MediaValidationError.UnsupportedMimeType(normalizedMime))
        }
        val size = sizeBytes ?: return MediaValidationResult.Invalid(MediaValidationError.UnknownSize)
        if (size <= 0L) {
            return MediaValidationResult.Invalid(MediaValidationError.EmptyFile)
        }
        if (size > maxBytes) {
            return MediaValidationResult.Invalid(MediaValidationError.FileTooLarge(size, maxBytes))
        }
        return MediaValidationResult.Valid(
            normalizedMimeType = normalizedMime,
            sizeBytes = size,
        )
    }

    fun validateTextInput(
        text: String?,
        maxLength: Int = DEFAULT_MAX_TEXT_LENGTH,
        allowEmpty: Boolean = false,
    ): TextValidationResult {
        val value = text.orEmpty()
        if (!allowEmpty && value.trim().isEmpty()) {
            return TextValidationResult.Invalid(TextValidationError.EmptyOrWhitespace)
        }
        if (value.length > maxLength) {
            return TextValidationResult.Invalid(TextValidationError.TooLong(value.length, maxLength))
        }
        if (value.any { it.isISOControl() && it != '\n' && it != '\r' && it != '\t' }) {
            return TextValidationResult.Invalid(TextValidationError.InvalidControlCharacters)
        }
        return TextValidationResult.Valid(value)
    }

    fun sanitizeExtensionHint(fileName: String?): String? =
        fileName
            ?.substringAfterLast('.', missingDelimiterValue = "")
            ?.trim()
            ?.lowercase()
            ?.takeIf { it.isNotBlank() }
}

sealed interface MediaValidationResult {
    data class Valid(
        val normalizedMimeType: String,
        val sizeBytes: Long,
    ) : MediaValidationResult

    data class Invalid(val error: MediaValidationError) : MediaValidationResult
}

sealed interface MediaValidationError {
    data object MissingMimeType : MediaValidationError
    data class UnsupportedMimeType(val mimeType: String) : MediaValidationError
    data object UnknownSize : MediaValidationError
    data object EmptyFile : MediaValidationError
    data class FileTooLarge(val actualBytes: Long, val maxBytes: Long) : MediaValidationError
    data object Unreadable : MediaValidationError
}

sealed interface TextValidationResult {
    data class Valid(val value: String) : TextValidationResult
    data class Invalid(val error: TextValidationError) : TextValidationResult
}

sealed interface TextValidationError {
    data object EmptyOrWhitespace : TextValidationError
    data class TooLong(val actualLength: Int, val maxLength: Int) : TextValidationError
    data object InvalidControlCharacters : TextValidationError
}
