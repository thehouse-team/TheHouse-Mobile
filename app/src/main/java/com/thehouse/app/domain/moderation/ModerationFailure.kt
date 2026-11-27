package com.thehouse.app.domain.moderation

/**
 * Errores de moderación del Backend, centralizados y tipados.
 *
 * Backend (verificado en `UserEndpointSupport.cs:107-119`):
 * - 422 `content_rejected` + `field` + `categories`
 * - 503 `moderation_unavailable` + `Retry-After: 30`
 *
 * El texto original NO viaja en el error: se conserva en el estado de la UI
 * para que el usuario pueda corregirlo sin reescribirlo.
 */
sealed interface ModerationFailure {
    data class Rejected(val field: String?, val categories: List<String>) : ModerationFailure
    data object Unavailable : ModerationFailure

    val isRetryable: Boolean get() = this is Unavailable
}

/** Resultado de clasificar una respuesta HTTP del Backend. */
sealed interface ModerationOutcome {
    data object Accepted : ModerationOutcome
    data class Rejected(val failure: ModerationFailure.Rejected) : ModerationOutcome
    data class Unavailable(val failure: ModerationFailure.Unavailable) : ModerationOutcome
    data class Other(val statusCode: Int, val code: String?) : ModerationOutcome
}
