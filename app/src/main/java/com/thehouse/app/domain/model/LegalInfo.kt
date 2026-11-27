package com.thehouse.app.domain.model

/** Información legal vigente (Backend `GET /api/v1/legal`). */
data class LegalInfo(
    val termsVersion: String,
    val privacyVersion: String,
    val landlordTermsVersion: String?,
    val termsUrl: String?,
    val privacyUrl: String?,
    val accountDeletionUrl: String?,
    val updatedAt: String?,
) {
    /** State seguro cuando la URL no está publicada. */
    val privacyStatus: String
        get() = if (privacyUrl.isNullOrBlank()) {
            "El aviso de privacidad está disponible dentro de la aplicación."
        } else {
            "Puedes abrir el aviso de privacidad en tu navegador."
        }
}

/** Aceptación vigente del usuario (`GET /users/me`). */
data class LegalAcceptanceStatus(
    val upToDate: Boolean,
    val acceptedTermsVersion: String?,
    val acceptedPrivacyVersion: String?,
)
