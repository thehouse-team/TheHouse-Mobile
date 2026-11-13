package com.thehouse.app.domain.error

/**
 * Códigos de negocio confirmados del contrato NV-21 (reportes).
 * Nunca se muestran al usuario: Presentation los traduce a texto amigable.
 */
enum class ReportErrorCode {
    REPORT_EXISTS,
    SELF_REPORT,
    ACCOUNT_SUSPENDED,
    VALIDATION,
    UNKNOWN;

    companion object {
        fun fromApi(value: String?): ReportErrorCode = when (value) {
            "report_exists" -> REPORT_EXISTS
            "self_report" -> SELF_REPORT
            "account_suspended" -> ACCOUNT_SUSPENDED
            else -> UNKNOWN
        }
    }
}

class ReportException(
    val domainError: DomainError,
    val code: ReportErrorCode = ReportErrorCode.UNKNOWN,
    val retryAfterSeconds: Long? = null,
) : Exception()
