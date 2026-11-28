package com.thehouse.app.domain.landlord

import com.thehouse.app.domain.model.PublicationStatus
import com.thehouse.app.domain.model.PublicationStep

/**
 * Guía de publicación derivada EXCLUSIVAMENTE de los requisitos reales devueltos por
 * `GET /api/v1/landlord/buildings/{id}/publication`.
 *
 * No inventa requisitos ni porcentajes: cuenta los pasos del Backend y calcula el progreso
 * como `completados / total` sobre esa lista. Si el Backend no envía pasos, el progreso es
 * `null` (sin porcentaje) en lugar de asumir 0 % o 100 %.
 */
data class PublicationGuide(
    val steps: List<PublicationStep>,
    val completedCount: Int,
    val totalCount: Int,
    val progressPercent: Int?,
    val pendingSteps: List<PublicationStep>,
    val completedSteps: List<PublicationStep>,
    val canPublish: Boolean,
    /** Motivo concreto por el que no se puede enviar a revisión (sin códigos internos). */
    val blockReason: String?,
) {
    companion object {
        /**
         * Orden guiado solicitado por el usuario. Sólo reordena los ids reales del Backend:
         * nunca agrega ni quita requisitos. Los ids desconocidos se conservan al final.
         */
        private val ORDER = listOf(
            PublicationStep.STEP_BUILDING_DATA,
            PublicationStep.STEP_LOCATION,
            PublicationStep.STEP_SERVICES,
            PublicationStep.STEP_ROOM_READY,
            PublicationStep.STEP_BUILDING_PHOTO,
            PublicationStep.STEP_DOCUMENTS,
        )

        fun from(status: PublicationStatus?, draft: Boolean): PublicationGuide {
            val steps = status?.steps.orEmpty()
            val ordered = steps.sortedBy { step: PublicationStep ->
                ORDER.indexOf(step.id).takeIf { it >= 0 } ?: ORDER.size
            }
            val completed = ordered.filter { it.done }
            val pending = ordered.filter { !it.done }
            val total = steps.size
            val percent = if (total > 0) completed.size * 100 / total else null
            val can = status?.canPublish == true
            val reason = when {
                status == null -> "Aún no pudimos cargar los requisitos de publicación."
                can -> null
                !draft -> "El edificio no está en borrador; su estado actual no permite enviarlo a revisión."
                pending.isEmpty() -> "Faltan requisitos por completar."
                else -> "Faltan por completar: ${pending.joinToString(", ") { stepLabel(it.id) }}."
            }
            return PublicationGuide(
                steps = ordered,
                completedCount = completed.size,
                totalCount = total,
                progressPercent = percent,
                pendingSteps = pending,
                completedSteps = completed,
                canPublish = can,
                blockReason = reason,
            )
        }
    }
}

/** Etiquetas en español para los ids literales del contrato; fallback seguro para ids futuros. */
fun publicationStepLabel(id: String): String = when (id) {
    PublicationStep.STEP_BUILDING_DATA -> "Datos del inmueble"
    PublicationStep.STEP_LOCATION -> "Ubicación y universidad"
    PublicationStep.STEP_SERVICES -> "Servicios"
    PublicationStep.STEP_ROOM_READY -> "Habitación lista"
    PublicationStep.STEP_BUILDING_PHOTO -> "Fotografía del edificio"
    PublicationStep.STEP_DOCUMENTS -> "Documentación de verificación"
    else -> id.replace('_', ' ').replaceFirstChar { it.uppercase() }
}

private fun stepLabel(id: String): String = publicationStepLabel(id)
