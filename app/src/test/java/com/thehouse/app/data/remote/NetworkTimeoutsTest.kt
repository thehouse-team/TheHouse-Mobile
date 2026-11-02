package com.thehouse.app.data.remote

import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class NetworkTimeoutsTest {

    @Test
    fun `exposes positive default timeouts`() {
        val timeouts = NetworkTimeouts()

        assertEquals(Duration.ofSeconds(15), timeouts.connect)
        assertEquals(Duration.ofSeconds(30), timeouts.read)
        assertEquals(Duration.ofSeconds(30), timeouts.write)
        assertEquals(Duration.ofSeconds(60), timeouts.call)
    }

    @Test
    fun `rejects non positive timeouts`() {
        assertThrows(IllegalArgumentException::class.java) {
            NetworkTimeouts(connect = Duration.ZERO)
        }
    }
}
