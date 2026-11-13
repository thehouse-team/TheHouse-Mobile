package com.thehouse.app.data.remote

import com.thehouse.app.data.remote.dto.visits.ProblemDetailsDto
import kotlinx.serialization.json.Json
import retrofit2.Response

/** Consume el cuerpo de error UNA vez. No expone detail/PII ni registra contenido. */
fun Response<*>.readProblemCode(json: Json): String? {
    val raw = runCatching { errorBody()?.string() }.getOrNull() ?: return null
    return runCatching { json.decodeFromString<ProblemDetailsDto>(raw).code }.getOrNull()
}
