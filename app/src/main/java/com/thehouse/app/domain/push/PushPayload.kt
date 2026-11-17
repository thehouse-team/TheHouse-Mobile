package com.thehouse.app.domain.push

import com.thehouse.app.domain.model.NotificationCategory
import com.thehouse.app.domain.model.NotificationTarget
import com.thehouse.app.domain.model.NotificationTargetKind

/**
 * Contrato de datos FCM definido por Mobile (mensajes **data-only**).
 *
 * El sender (Backend/Firebase) debe respetar estas claves. El contenido nunca
 * se interpreta como instrucciones: todo se valida antes de mostrarse o navegar.
 */
object PushPayloadKeys {
    const val NOTIFICATION_ID = "notificationId"
    const val CATEGORY = "category"
    const val TITLE = "title"
    const val BODY = "body"
    const val TARGET_KIND = "targetKind"
    const val TARGET_ID = "targetId"
}

const val PUSH_NOTIFICATION_ID_MAX_LENGTH = 128
const val PUSH_TARGET_ID_MAX_LENGTH = 128
const val PUSH_TITLE_MAX_LENGTH = 200
const val PUSH_BODY_MAX_LENGTH = 1000

private val SAFE_ID = Regex("^[A-Za-z0-9][A-Za-z0-9._:-]{0,127}$")

/**
 * Valida y resuelve un target de notificación. Devuelve `null` para kinds
 * desconocidos, ids vacíos o ids con caracteres no permitidos.
 */
object NotificationTargetResolver {

    fun resolve(kind: String?, id: String?): NotificationTarget? {
        if (kind.isNullOrBlank()) return null
        val parsedKind = NotificationTargetKind.fromApi(kind.trim())
        if (parsedKind == NotificationTargetKind.UNKNOWN) return null
        val safeId = id?.trim().orEmpty()
        if (!SAFE_ID.matches(safeId)) return null
        return NotificationTarget(kind = parsedKind, id = safeId)
    }

    fun isSafeId(value: String): Boolean = SAFE_ID.matches(value)
}

enum class PushPayloadWarning {
    /** Llegó un target no soportado o inválido; navegación cae a Notification Center. */
    TARGET_FALLBACK,

    /** Llegó una categoría desconocida; se usa el canal de sistema. */
    CATEGORY_FALLBACK,
}

enum class PushPayloadRejection {
    MISSING_NOTIFICATION_ID,
    INVALID_NOTIFICATION_ID,
    MISSING_TITLE,
    TITLE_TOO_LONG,
    BODY_TOO_LONG,
    UNSAFE_TEXT,
}

data class PushPayload(
    val notificationId: String,
    val category: NotificationCategory?,
    val title: String,
    val body: String,
    val target: NotificationTarget?,
)

sealed interface PushPayloadParseResult {
    data class Valid(
        val payload: PushPayload,
        val warnings: Set<PushPayloadWarning> = emptySet(),
    ) : PushPayloadParseResult

    data class Invalid(val rejection: PushPayloadRejection) : PushPayloadParseResult
}

/**
 * Parser puro y defensivo de payloads FCM. No usa Android ni ejecuta acciones.
 */
object PushPayloadParser {

    fun parse(data: Map<String, String>): PushPayloadParseResult {
        val notificationId = data[PushPayloadKeys.NOTIFICATION_ID]?.trim().orEmpty()
        if (notificationId.isEmpty()) {
            return PushPayloadParseResult.Invalid(PushPayloadRejection.MISSING_NOTIFICATION_ID)
        }
        if (!NotificationTargetResolver.isSafeId(notificationId)) {
            return PushPayloadParseResult.Invalid(PushPayloadRejection.INVALID_NOTIFICATION_ID)
        }

        val title = data[PushPayloadKeys.TITLE]?.trim().orEmpty()
        if (title.isEmpty()) {
            return PushPayloadParseResult.Invalid(PushPayloadRejection.MISSING_TITLE)
        }
        if (title.length > PUSH_TITLE_MAX_LENGTH) {
            return PushPayloadParseResult.Invalid(PushPayloadRejection.TITLE_TOO_LONG)
        }

        val body = data[PushPayloadKeys.BODY]?.trim().orEmpty()
        if (body.length > PUSH_BODY_MAX_LENGTH) {
            return PushPayloadParseResult.Invalid(PushPayloadRejection.BODY_TOO_LONG)
        }
        if (hasUnsafeControlCharacters(title) || hasUnsafeControlCharacters(body)) {
            return PushPayloadParseResult.Invalid(PushPayloadRejection.UNSAFE_TEXT)
        }

        val warnings = mutableSetOf<PushPayloadWarning>()

        val rawCategory = data[PushPayloadKeys.CATEGORY]?.trim().orEmpty()
        val category = NotificationCategory.fromApi(rawCategory)
        if (rawCategory.isNotEmpty() && category == null) {
            warnings += PushPayloadWarning.CATEGORY_FALLBACK
        }

        val rawTargetKind = data[PushPayloadKeys.TARGET_KIND]?.trim().orEmpty()
        val rawTargetId = data[PushPayloadKeys.TARGET_ID]?.trim().orEmpty()
        val target = NotificationTargetResolver.resolve(rawTargetKind, rawTargetId)
        if (rawTargetKind.isNotEmpty() && target == null) {
            warnings += PushPayloadWarning.TARGET_FALLBACK
        }

        return PushPayloadParseResult.Valid(
            payload = PushPayload(
                notificationId = notificationId,
                category = category,
                title = title,
                body = body,
                target = target,
            ),
            warnings = warnings,
        )
    }

    private fun hasUnsafeControlCharacters(value: String): Boolean =
        value.any { it.isISOControl() && it != '\n' && it != '\t' }
}
