package com.thehouse.app.data.multimedia

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.provider.OpenableColumns
import com.thehouse.app.domain.multimedia.MediaValidation
import com.thehouse.app.domain.multimedia.MediaValidationError
import com.thehouse.app.domain.multimedia.MediaValidationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest

data class MediaMetadata(
    val uri: Uri,
    val displayName: String?,
    val mimeType: String?,
    val sizeBytes: Long?,
)

sealed interface PreparedMedia {
    data class Success(
        val sourceUri: Uri,
        val sanitizedFile: File,
        val mimeType: String,
        val sizeBytes: Long,
        val width: Int,
        val height: Int,
        /** Hash corto no sensible para trazabilidad local/debug controlado (no PII). */
        val contentFingerprint: String,
    ) : PreparedMedia

    data class Failure(val reason: String, val validationError: MediaValidationError? = null) : PreparedMedia
    data object Cancelled : PreparedMedia
}

object MediaInspector {

    suspend fun queryMetadata(context: Context, uri: Uri): MediaMetadata? = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            val mime = resolver.getType(uri)
            var name: String? = null
            var size: Long? = null
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
                ?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIndex >= 0) name = cursor.getString(nameIndex)
                        if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) size = cursor.getLong(sizeIndex)
                    }
                }
            // Fallback de tamaño si provider no lo expone.
            if (size == null || size < 0L) {
                size = resolver.openInputStream(uri)?.use { input ->
                    inputLength(input)
                }
            }
            MediaMetadata(uri = uri, displayName = name, mimeType = mime, sizeBytes = size)
        }.getOrNull()
    }

    suspend fun validateImageUri(
        context: Context,
        uri: Uri,
        maxBytes: Long = MediaValidation.DEFAULT_MAX_IMAGE_BYTES,
        allowedMimeTypes: Set<String> = MediaValidation.DEFAULT_ALLOWED_IMAGE_MIME_TYPES,
    ): MediaValidationResult = withContext(Dispatchers.IO) {
        val metadata = queryMetadata(context, uri)
            ?: return@withContext MediaValidationResult.Invalid(MediaValidationError.Unreadable)
        MediaValidation.validateImage(
            mimeType = metadata.mimeType,
            sizeBytes = metadata.sizeBytes,
            maxBytes = maxBytes,
            allowedMimeTypes = allowedMimeTypes,
        )
    }

    /**
     * Prepara imagen para upload futuro: valida, corrige orientación y recompone a JPEG
     * en caché de app para no propagar EXIF GPS/metadatos innecesarios (best-effort).
     * No realiza upload ni moderación remota.
     */
    suspend fun sanitizeImageForUpload(
        context: Context,
        sourceUri: Uri,
        maxBytes: Long = MediaValidation.DEFAULT_MAX_IMAGE_BYTES,
        allowedMimeTypes: Set<String> = MediaValidation.DEFAULT_ALLOWED_IMAGE_MIME_TYPES,
        maxDimension: Int = 2048,
        jpegQuality: Int = 85,
    ): PreparedMedia = withContext(Dispatchers.IO) {
        val validation = validateImageUri(context, sourceUri, maxBytes, allowedMimeTypes)
        val valid = when (validation) {
            is MediaValidationResult.Valid -> validation
            is MediaValidationResult.Invalid -> return@withContext PreparedMedia.Failure(
                reason = "Validación local fallida",
                validationError = validation.error,
            )
        }

        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(sourceUri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            ?: return@withContext PreparedMedia.Failure("No se pudo leer el archivo", MediaValidationError.Unreadable)

        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            return@withContext PreparedMedia.Failure("Archivo de imagen vacío o corrupto", MediaValidationError.EmptyFile)
        }

        val sample = calculateInSampleSize(bounds.outWidth, bounds.outHeight, maxDimension, maxDimension)
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = resolver.openInputStream(sourceUri)?.use { BitmapFactory.decodeStream(it, null, decodeOptions) }
            ?: return@withContext PreparedMedia.Failure("No se pudo decodificar la imagen", MediaValidationError.Unreadable)

        val rotated = applyExifOrientation(resolver, sourceUri, decoded)
        val outDir = File(context.cacheDir, "prepared_media").apply { mkdirs() }
        val outFile = File(outDir, "img_${System.currentTimeMillis()}_${sourceUri.hashCode().toUInt()}.jpg")
        FileOutputStream(outFile).use { output ->
            if (!rotated.compress(Bitmap.CompressFormat.JPEG, jpegQuality.coerceIn(60, 95), output)) {
                rotated.recycle()
                if (rotated !== decoded) decoded.recycle()
                outFile.delete()
                return@withContext PreparedMedia.Failure("No se pudo procesar la imagen")
            }
        }
        if (rotated !== decoded) decoded.recycle()
        rotated.recycle()

        val outSize = outFile.length()
        if (outSize <= 0L) {
            outFile.delete()
            return@withContext PreparedMedia.Failure("Archivo procesado vacío", MediaValidationError.EmptyFile)
        }
        // Si el saneado crece sobre el límite contractual/local, se rechaza (no se afirma listo).
        if (outSize > maxBytes) {
            outFile.delete()
            return@withContext PreparedMedia.Failure(
                "La imagen procesada excede el tamaño permitido",
                MediaValidationError.FileTooLarge(outSize, maxBytes),
            )
        }

        val fingerprint = runCatching { fingerprint(outFile) }.getOrDefault("n/a")
        PreparedMedia.Success(
            sourceUri = sourceUri,
            sanitizedFile = outFile,
            mimeType = "image/jpeg",
            sizeBytes = outSize,
            width = bounds.outWidth,
            height = bounds.outHeight,
            contentFingerprint = fingerprint,
        )
    }

    private fun inputLength(input: InputStream): Long {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0L
        while (true) {
            val read = input.read(buffer)
            if (read <= 0) break
            total += read
        }
        return total
    }

    private fun calculateInSampleSize(width: Int, height: Int, reqWidth: Int, reqHeight: Int): Int {
        var inSampleSize = 1
        var halfHeight = height / 2
        var halfWidth = width / 2
        while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
            inSampleSize *= 2
        }
        return inSampleSize.coerceAtLeast(1)
    }

    private fun applyExifOrientation(resolver: ContentResolver, uri: Uri, bitmap: Bitmap): Bitmap {
        val orientation = runCatching {
            resolver.openInputStream(uri)?.use { input ->
                ExifInterface(input).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.preScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.preScale(-1f, 1f)
            }
            else -> return bitmap
        }
        return runCatching {
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }.getOrDefault(bitmap)
    }

    private fun fingerprint(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().take(6).joinToString("") { b -> "%02x".format(b) }
    }
}
