package com.thehouse.app.data.serialization

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import org.junit.Assert.assertEquals
import org.junit.Test

@Serializable
private data class SamplePayload(val value: String)

@Serializable
private data class PayloadWithDefault(val value: String = "default")

class JsonProviderTest {

    @Test
    fun `ignores unknown keys`() {
        val decoded = JsonProvider.create().decodeFromString<SamplePayload>(
            """{"value":"ok","extra":true}""",
        )

        assertEquals("ok", decoded.value)
    }

    @Test
    fun `encodes default values`() {
        val encoded = JsonProvider.create().encodeToString(PayloadWithDefault())

        assertEquals("""{"value":"default"}""", encoded)
    }
}
