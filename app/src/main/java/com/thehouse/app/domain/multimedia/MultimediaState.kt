package com.thehouse.app.domain.multimedia

/**
 * Estados honestos para flujos multimedia futuros.
 * No afirma upload/moderación real sin contrato ni ejecución verificable.
 */
enum class MultimediaProcessingStage {
    SELECTING,
    PROCESSING,
    READY,
    VALIDATION_ERROR,
    MODERATION_PENDING,
    MODERATION_REJECTED,
    UPLOAD_PENDING,
}

enum class ModerationContractStatus {
    /** No existe contrato backend ejecutable para moderación automática previa al upload. */
    BLOCKED_NO_BACKEND_CONTRACT,
    /** Contrato confirmado (no usado actualmente). */
    AVAILABLE,
}

data class ModerationUiState(
    val contractStatus: ModerationContractStatus = ModerationContractStatus.BLOCKED_NO_BACKEND_CONTRACT,
    val canClaimModerated: Boolean = false,
    val canClaimUploaded: Boolean = false,
    val notice: String = when (contractStatus) {
        ModerationContractStatus.BLOCKED_NO_BACKEND_CONTRACT ->
            "MULTIMEDIA-MODERATION-BACKEND-BLOCKED: no existe contrato de moderación/upload ejecutable; no se afirma contenido moderado ni subido."
        ModerationContractStatus.AVAILABLE ->
            "Contrato de moderación disponible; validar resultado real antes de afirmar estado."
    },
)
