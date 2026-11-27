package com.thehouse.app.domain.device

import java.util.UUID

/**
 * Identificador persistente de instalación.
 *
 * Contrato Backend: GUID con formato "D" (minúsculas, 36 chars con guiones),
 * verificado en `PushRules.InstallationId` del Backend. Un UUID inválido
 * produce 404 en `PUT|DELETE /users/me/devices/{installationId}`.
 *
 * No es un identificador de hardware: no usa IMEI, MAC ni Android ID.
 */
@JvmInline
value class InstallationId(val value: String) {

    init {
        require(isValid(value)) { "installationId must be a lowercase GUID in D format" }
    }

    companion object {
        private val GUID_D = Regex(
            "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$",
        )

        fun isValid(value: String?): Boolean = value != null && GUID_D.matches(value)

        fun generate(): InstallationId = InstallationId(UUID.randomUUID().toString())
    }
}
