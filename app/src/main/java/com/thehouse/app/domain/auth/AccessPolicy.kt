package com.thehouse.app.domain.auth

/**
 * Acciones que requieren autenticación (D4, sin backend).
 *
 * Guest SIEMPRE ve un aviso claro y es llevado a Login. No se crea
 * sesión falsa, usuario fake ni tokens fake.
 */
enum class ProtectedAction {
    SaveFavorite,
    Contact,
    Publish,
    ScheduleVisit,
    ViewRoomDetails,
}

/**
 * Política de acceso pura y testeable (Domain, sin Android).
 */
object AccessPolicy {

    fun requiresAuthentication(action: ProtectedAction): Boolean = true

    /**
     * ¿La acción protegida puede continuar?
     * Guest (false) → bloqueada (ir a Login). Autenticado (true) → permitida.
     */
    fun canPerform(action: ProtectedAction, isAuthenticated: Boolean): Boolean =
        !requiresAuthentication(action) || isAuthenticated

    fun shouldRedirectToLogin(action: ProtectedAction, isAuthenticated: Boolean): Boolean =
        requiresAuthentication(action) && !isAuthenticated
}
