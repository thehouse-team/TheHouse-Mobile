package com.thehouse.app.device

import com.thehouse.app.data.device.DataStoreInstallationIdStore
import com.thehouse.app.data.moderation.ModerationErrorParser
import com.thehouse.app.domain.device.InstallationId
import com.thehouse.app.domain.moderation.ModerationFailure
import com.thehouse.app.domain.moderation.ModerationOutcome
import com.thehouse.app.data.serialization.JsonProvider
import com.thehouse.app.testutil.InMemoryPreferencesStore
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class D21DeviceTests {

    @Test
    fun installationId_isGuidD_andStableAcrossReads() = runTest {
        val store = DataStoreInstallationIdStore(InMemoryPreferencesStore())
        val first = store.getOrCreate()
        assertTrue(InstallationId.isValid(first.value))
        assertEquals(first, store.getOrCreate())
        assertEquals(36, first.value.length)
        assertTrue(first.value == first.value.lowercase())
    }

    @Test
    fun installationId_survivesNewStoreInstance_overSamePreferences() = runTest {
        val preferences = InMemoryPreferencesStore()
        val original = DataStoreInstallationIdStore(preferences).getOrCreate()
        val afterRestart = DataStoreInstallationIdStore(preferences).getOrCreate()
        assertEquals(original, afterRestart)
    }

    @Test
    fun installationId_rejectsNonGuidFormats() {
        assertFalse(InstallationId.isValid("no-es-uuid"))
        assertFalse(InstallationId.isValid("72D4DDF2-2FF9-4E2D-9B0B-000000000000")) // mayúsculas
        assertNotEquals(InstallationId.generate().value, InstallationId.generate().value)
    }

    private fun problemResponse(code: Int, body: String): Response<Any> =
        Response.error<Any>(
            body.toResponseBody("application/problem+json".toMediaType()),
            okhttp3.Response.Builder()
                .request(okhttp3.Request.Builder().url("https://example.test").build())
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .message("error")
                .build(),
        )

    @Test
    fun moderation_422_content_rejected_isClassifiedWithFieldAndCategories() {
        val response = problemResponse(
            422,
            """{"status":422,"code":"content_rejected","field":"message","categories":["hate","unknown-category"]}""",
        )
        val outcome = ModerationErrorParser.parse(response, JsonProvider.create())
        assertTrue(outcome is ModerationOutcome.Rejected)
        val failure = (outcome as ModerationOutcome.Rejected).failure
        assertEquals("message", failure.field)
        assertEquals(listOf("hate"), failure.categories)
        assertFalse(failure.isRetryable)
    }

    @Test
    fun moderation_503_moderation_unavailable_isRetryable() {
        val response = problemResponse(503, """{"status":503,"code":"moderation_unavailable"}""")
        val outcome = ModerationErrorParser.parse(response, JsonProvider.create())
        assertTrue(outcome is ModerationOutcome.Unavailable)
        assertTrue((outcome as ModerationOutcome.Unavailable).failure.isRetryable)
    }

    @Test
    fun moderation_422_withOtherCode_isNotTreatedAsRejected() {
        val response = problemResponse(422, """{"status":422,"code":"smart_match_profile_incomplete"}""")
        val outcome = ModerationErrorParser.parse(response, JsonProvider.create())
        assertTrue(outcome is ModerationOutcome.Other)
        assertEquals(422, (outcome as ModerationOutcome.Other).statusCode)
    }

    @Test
    fun moderation_200_isAccepted() {
        val response = Response.success<Any>(
            Unit,
            okhttp3.Response.Builder()
                .request(okhttp3.Request.Builder().url("https://example.test").build())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("ok")
                .build(),
        )
        assertEquals(ModerationOutcome.Accepted, ModerationErrorParser.parse(response, JsonProvider.create()))
    }

    @Test
    fun moderation_failure_model_keepsTextOutOfError() {
        val failure = ModerationFailure.Rejected(field = "text", categories = listOf("violence"))
        assertFalse(failure.toString().contains("contenido"))
        assertEquals(listOf("violence"), failure.categories)
    }
}
