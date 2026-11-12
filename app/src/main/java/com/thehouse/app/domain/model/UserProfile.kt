package com.thehouse.app.domain.model

/**
 * Perfil de usuario obtenido de /users/me/profile.
 * Estado snapshottado que se observa en Presentation.
 */
data class UserProfile(
    val name: String?,               // editable vía PATCH /users/me/profile
    val university: University?,     // universidad del usuario (puede ser null)
    val quickSetupCompleted: Boolean // indica si el quick setup ya fue completado
)
