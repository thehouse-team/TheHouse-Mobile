package com.thehouse.app.data.remote

import com.thehouse.app.data.serialization.JsonProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class NetworkingFoundationTest {

    @Test
    fun `builds retrofit with placeholder base url without executing requests`() {
        val retrofit = RetrofitFactory.create(
            baseUrl = "https://example.invalid/",
            client = HttpClientFactory.create(enableLogging = false, cacheDir = null),
            json = JsonProvider.create(),
        )

        assertEquals("https://example.invalid/", retrofit.baseUrl().toString())
    }

    @Test
    fun `rejects non https base url`() {
        assertThrows(IllegalArgumentException::class.java) {
            RetrofitFactory.create(
                baseUrl = "http://example.invalid/",
                client = HttpClientFactory.create(enableLogging = false, cacheDir = null),
                json = JsonProvider.create(),
            )
        }
    }
}
