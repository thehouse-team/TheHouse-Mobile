package com.thehouse.app.data.remote.dto.legal

import kotlinx.serialization.Serializable

/**
 * Espejo del contrato Backend confirmado (780b7be):
 * `GET /api/v1/legal` → `LegalResponse`. Versiones son STRING (p. ej. "2026-11").
 * Las URLs son opcionales: el backend puede devolverlas null.
 */
@Serializable
data class LegalResponseDto(
    val termsVersion: String,
    val privacyVersion: String,
    val landlordTermsVersion: String? = null,
    val termsUrl: String? = null,
    val privacyUrl: String? = null,
    val accountDeletionUrl: String? = null,
    val updatedAt: String? = null,
)

/** `PUT /api/v1/users/me/legal` → `LegalAcceptanceResponse`. */
@Serializable
data class AcceptLegalResponseDto(
    val termsVersion: String,
    val privacyVersion: String,
    val acceptedAt: String? = null,
)
