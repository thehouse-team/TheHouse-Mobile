package com.thehouse.app.data.remote

import java.time.Duration

/**
 * Timeouts base de red. No contiene URLs, endpoints ni secretos.
 */
data class NetworkTimeouts(
    val connect: Duration = Duration.ofSeconds(15),
    val read: Duration = Duration.ofSeconds(30),
    val write: Duration = Duration.ofSeconds(30),
    val call: Duration = Duration.ofSeconds(60),
) {
    init {
        requirePositive(connect, "connect")
        requirePositive(read, "read")
        requirePositive(write, "write")
        requirePositive(call, "call")
    }

    private fun requirePositive(value: Duration, name: String) {
        require(!value.isNegative && !value.isZero) {
            "$name timeout must be positive"
        }
    }
}
