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
    /**
     * Estado honesto cuando la URL no está publicada (D19.1).
     * La app NO contiene texto legal propio: afirmar lo contrario sería una
     * declaración falsa. Solo se abre una URL HTTPS válida del Backend.
     */
    val privacyStatus: String
        get() = if (privacyUrl.isNullOrBlank()) {
            "El aviso de privacidad aún no ha sido publicado."
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
