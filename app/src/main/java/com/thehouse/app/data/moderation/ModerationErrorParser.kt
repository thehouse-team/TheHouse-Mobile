package com.thehouse.app.data.moderation

import com.thehouse.app.domain.moderation.ModerationFailure
import com.thehouse.app.domain.moderation.ModerationOutcome
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.Response

/**
 * Clasifica respuestas HTTP del Backend según el contrato de moderación.
 *
 * El cuerpo de error se lee UNA SOLA VEZ (OkHttp no permite releerlo) y solo
 * se extraen códigos/categorías controladas. Nunca se lee ni se expone el
 * contenido que el usuario envió.
 */
object ModerationErrorParser {

    private val ALLOWED_CATEGORIES = setOf(
        "hate", "sexual", "violence", "self_harm", "blocklist",
    )

    fun parse(response: Response<*>, json: Json): ModerationOutcome {
        if (response.isSuccessful) return ModerationOutcome.Accepted
        val raw = runCatching { response.errorBody()?.string() }.getOrNull().orEmpty()
        val obj = runCatching { json.parseToJsonElement(raw) as? JsonObject }.getOrNull()
        val code = obj?.get("code")?.runCatching { jsonPrimitive.content }?.getOrNull()
        return when {
            response.code() == 422 && code == "content_rejected" ->
                ModerationOutcome.Rejected(
                    ModerationFailure.Rejected(
                        field = obj.readString("field"),
                        categories = obj.readCategories(),
                    ),
                )
            response.code() == 503 && code == "moderation_unavailable" ->
                ModerationOutcome.Unavailable(ModerationFailure.Unavailable)
            else -> ModerationOutcome.Other(response.code(), code)
        }
    }

    private fun JsonObject?.readString(key: String): String? =
        this?.get(key)?.runCatching { jsonPrimitive.content }?.getOrNull()?.take(64)

    private fun JsonObject?.readCategories(): List<String> {
        val array = this?.get("categories") as? JsonArray ?: return emptyList()
        return array.mapNotNull { element ->
            runCatching { element.jsonPrimitive.content }
                .getOrNull()
                ?.takeIf { it in ALLOWED_CATEGORIES }
        }
    }
}
