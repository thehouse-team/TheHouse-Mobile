package com.thehouse.app.presentation.landlord

import com.thehouse.app.domain.model.AgreementStatus
import com.thehouse.app.presentation.components.StatusTone

/** Etiquetas y tonos de estado de acuerdos (solo presentación). */
internal fun agreementStatusLabel(status: AgreementStatus): String = when (status) {
    AgreementStatus.PENDING -> "Pendiente"
    AgreementStatus.ACTIVE -> "Activo"
    AgreementStatus.ENDED -> "Finalizado"
    AgreementStatus.CANCELLED -> "Cancelado"
}

internal fun agreementStatusTone(status: AgreementStatus): StatusTone = when (status) {
    AgreementStatus.PENDING -> StatusTone.Warning
    AgreementStatus.ACTIVE -> StatusTone.Success
    AgreementStatus.ENDED -> StatusTone.Neutral
    AgreementStatus.CANCELLED -> StatusTone.Error
}
