package com.thehouse.app.domain.landlord

/**
 * Validación previa al envío de documentos, espejo del contrato real del Backend
 * (`MediaEndpoint.cs` / `MediaServices.cs`). El Backend detecta el tipo por MAGIC BYTES,
 * no por extensión ni por Content-Type: validar aquí evita 413/415 evitables.
 */
object DocumentUploadPolicy {
    /** 10 MB exactos (`MediaServices.MaximumDocumentBytes`). */
    const val MAX_DOCUMENT_BYTES: Long = 10L * 1024L * 1024L
    /** Overhead multipart del Backend (16 KB). */
    const val MULTIPART_OVERHEAD_BYTES: Long = 16L * 1024L
    const val MAX_REQUEST_BYTES: Long = MAX_DOCUMENT_BYTES + MULTIPART_OVERHEAD_BYTES

    const val MIME_PDF = "application/pdf"
    const val MIME_JPEG = "image/jpeg"
    const val MIME_PNG = "image/png"

    val DISPLAY_FORMATS = "PDF, JPEG o PNG"
    val DISPLAY_MAX_SIZE = "10 MB"

    sealed interface Validation {
        data object Valid : Validation
        /** Tamaño declarado superior al límite contractual (413 evitable). */
        data object TooLarge : Validation
        /** Firma de bytes no reconocida (415 evitable). */
        data object UnsupportedFormat : Validation
        /** Archivo vacío o ilegible. */
        data object EmptyOrUnreadable : Validation
    }

    private val JPEG_MAGIC = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())
    private val PNG_MAGIC = byteArrayOf(
        0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
    )
    private val PDF_MAGIC = "%PDF-".toByteArray(Charsets.US_ASCII)

    /**
     * Detecta el MIME real por firma de bytes, igual que `MediaRules.DocumentType`.
     * Devuelve null para cualquier otro contenido (incluido WebP, no admitido en documentos).
     */
    fun detectMime(bytes: ByteArray): String? = when {
        bytes.size >= PDF_MAGIC.size && bytes.copyOf(PDF_MAGIC.size).contentEquals(PDF_MAGIC) -> MIME_PDF
        bytes.size >= JPEG_MAGIC.size && bytes.copyOf(JPEG_MAGIC.size).contentEquals(JPEG_MAGIC) -> MIME_JPEG
        bytes.size >= PNG_MAGIC.size && bytes.copyOf(PNG_MAGIC.size).contentEquals(PNG_MAGIC) -> MIME_PNG
        else -> null
    }

    fun validate(bytes: ByteArray, declaredSize: Long = bytes.size.toLong()): Validation = when {
        bytes.isEmpty() || declaredSize <= 0L -> Validation.EmptyOrUnreadable
        declaredSize > MAX_DOCUMENT_BYTES || bytes.size.toLong() > MAX_DOCUMENT_BYTES -> Validation.TooLarge
        detectMime(bytes) == null -> Validation.UnsupportedFormat
        else -> Validation.Valid
    }

    fun message(validation: Validation): String = when (validation) {
        Validation.Valid -> ""
        Validation.TooLarge ->
            "El archivo supera los $DISPLAY_MAX_SIZE permitidos. Elige un documento más pequeño."
        Validation.UnsupportedFormat ->
            "Formato no admitido. Sube un archivo $DISPLAY_FORMATS válido; el Backend rechaza otros formatos."
        Validation.EmptyOrUnreadable ->
            "No pudimos leer el archivo. Selecciona un documento $DISPLAY_FORMATS de hasta $DISPLAY_MAX_SIZE."
    }

    fun extensionFor(mime: String): String = when (mime) {
        MIME_PDF -> "pdf"
        MIME_PNG -> "png"
        MIME_JPEG -> "jpg"
        else -> "bin"
    }
}
