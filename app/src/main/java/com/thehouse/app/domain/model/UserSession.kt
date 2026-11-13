package com.thehouse.app.domain.model

/**
 * Sesión activa de usuario en el dominio.
 *
 * No contiene tokens: los tokens son un detalle de infraestructura de Data.
 * El access token vive solo en memoria (Data) y nunca cruza a Presentation.
 */
data class UserSession(
    val userId: String?,
    val displayName: String,
    val email: String,
    /** Campo confirmado de `GET /api/v1/users/me`; null cuando la sesión aún no se hidrató con /me. */
    val createdAt: String? = null,
)

/**
 * Estado observable de la sesión.
 */
sealed interface SessionState {
    data object Anonymous : SessionState
    data class Active(val session: UserSession) : SessionState
    /** Confirmada por Backend; conserva identidad, no equivale a logout. */
    data class Suspended(val session: UserSession?) : SessionState
}
