package com.thehouse.app.data.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AppConfigTest {

    @Test
    fun `accepts a non productive https placeholder`() {
        val config = AppConfig(
            apiBaseUrl = "https://example.invalid/",
            httpLoggingEnabled = false,
        )

        assertEquals("https://example.invalid/", config.apiBaseUrl)
    }

    @Test
    fun `rejects non https base url`() {
        assertThrows(IllegalArgumentException::class.java) {
            AppConfig(
                apiBaseUrl = "http://example.invalid/",
                httpLoggingEnabled = false,
            )
        }
    }

    @Test
    fun `rejects base url without trailing slash`() {
        assertThrows(IllegalArgumentException::class.java) {
            AppConfig(
                apiBaseUrl = "https://example.invalid",
                httpLoggingEnabled = false,
            )
        }
    }
}
