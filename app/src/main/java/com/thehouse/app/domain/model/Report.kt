package com.thehouse.app.domain.model

/** Modelos D13. Enums EXACTOS del contrato Backend NV-21. */

enum class ReportTargetType(val api: String) {
    BUILDING("building"),
    ROOM("room"),
    USER("user");
}

enum class ReportReason(val api: String) {
    FAKE("fake"),
    FRAUD("fraud"),
    INAPPROPRIATE("inappropriate"),
    SPAM("spam"),
    OTHER("other");
}

data class ReportCreated(
    val id: String,
    val status: String,
    val createdAt: String,
)

/** Estado de verificación del edificio (dueño). Estados reales de NV-12+NV-21. */
enum class VerificationDecision(val api: String) {
    APPROVED("approved"),
    CHANGES_REQUESTED("changes_requested"),
    REJECTED("rejected");

    companion object {
        fun fromApi(value: String): VerificationDecision =
            entries.firstOrNull { it.api == value }
                ?: CHANGES_REQUESTED
    }
}

data class VerificationLastDecision(
    val decision: VerificationDecision,
    val reason: String?,
    val decidedAt: String,
)

data class VerificationInfo(
    val status: com.thehouse.app.domain.model.BuildingStatus,
    val lastDecision: VerificationLastDecision?,
)
