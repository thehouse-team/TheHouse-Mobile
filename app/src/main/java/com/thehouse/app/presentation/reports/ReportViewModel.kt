package com.thehouse.app.presentation.reports

import com.thehouse.app.domain.error.DomainError
import com.thehouse.app.domain.error.ReportErrorCode
import com.thehouse.app.domain.error.ReportException
import com.thehouse.app.domain.model.ReportReason
import com.thehouse.app.domain.model.ReportTargetType
import com.thehouse.app.domain.usecase.CreateReportUseCase
import com.thehouse.app.presentation.common.BaseViewModel
import com.thehouse.app.presentation.common.UiEvent

/** Formulario de reporte (contrato real NV-21 §6.1). */
data class ReportUiState(
    val targetType: ReportTargetType,
    val targetId: String,
    val reason: ReportReason? = null,
    val details: String = "",
    val isSubmitting: Boolean = false,
    val submitted: Boolean = false,
    val errorCode: ReportErrorCode? = null,
    val errorMessage: String? = null,
) {
    /** details: opcional, ≤ 500 caracteres (contrato). */
    val detailsError: String? =
        if (details.length <= MAX_DETAILS) null else "Máximo 500 caracteres."
    val isValid: Boolean = reason != null && detailsError == null

    companion object {
        const val MAX_DETAILS = 500
    }
}

sealed interface ReportEvent : UiEvent {
    data class ReasonChanged(val reason: ReportReason) : ReportEvent
    data class DetailsChanged(val value: String) : ReportEvent
    data object Submit : ReportEvent
}

sealed interface ReportEffect : com.thehouse.app.presentation.common.UiEffect {
    data object Submitted : ReportEffect
}

class ReportViewModel(
    private val targetType: ReportTargetType,
    private val targetId: String,
    private val createReport: CreateReportUseCase,
) : BaseViewModel<ReportUiState, ReportEvent, ReportEffect>(
    ReportUiState(targetType = targetType, targetId = targetId),
) {

    override fun onEvent(event: ReportEvent) {
        when (event) {
            is ReportEvent.ReasonChanged -> updateState { it.copy(reason = event.reason) }
            is ReportEvent.DetailsChanged ->
                updateState { it.copy(details = event.value.take(ReportUiState.MAX_DETAILS)) }
            ReportEvent.Submit -> submit()
        }
    }

    /** Protección contra doble submit: un guard en isSubmitting. */
    private fun submit() {
        val current = uiState.value
        if (current.isSubmitting || current.submitted || !current.isValid) return
        updateState { it.copy(isSubmitting = true, errorCode = null, errorMessage = null) }
        launch {
            createReport(
                targetType = targetType,
                targetId = targetId,
                reason = current.reason!!,
                details = current.details.takeIf { it.isNotBlank() },
            ).fold(
                onSuccess = { _ ->
                    updateState { it.copy(isSubmitting = false, submitted = true) }
                    sendEffect(ReportEffect.Submitted)
                },
                onFailure = { throwable ->
                    val exception = throwable as? ReportException
                    updateState {
                        it.copy(
                            isSubmitting = false,
                            errorCode = exception?.code,
                            errorMessage = mapError(exception),
                        )
                    }
                },
            )
        }
    }

    private fun mapError(exception: ReportException?): String {
        if (exception == null) return "Ocurrió un error inesperado. Intenta de nuevo."
        return when {
            exception.code == ReportErrorCode.REPORT_EXISTS ->
                "Ya reportaste esto y el reporte sigue en revisión."
            exception.code == ReportErrorCode.SELF_REPORT ->
                "No puedes reportar tu propia cuenta."
            else -> when (exception.domainError) {
                DomainError.Network -> "No hay conexión. Verifica tu red e intenta de nuevo."
                DomainError.Timeout -> "La conexión tardó demasiado. Intenta de nuevo."
                DomainError.Unauthorized -> "Inicia sesión para continuar."
                DomainError.AccountSuspended -> "Tu cuenta está suspendida."
                DomainError.NotFound -> "No se encontró el contenido a reportar."
                DomainError.Conflict -> "El reporte entró en conflicto. Intenta de nuevo."
                DomainError.Gone -> "El contenido ya no está disponible."
                DomainError.RateLimited -> "Demasiadas solicitudes. Espera un momento e intenta de nuevo."
                DomainError.Server -> "El servicio no está disponible ahora. Intenta más tarde."
                DomainError.Unknown -> "Revisa los datos e intenta de nuevo."
            }
        }
    }
}
