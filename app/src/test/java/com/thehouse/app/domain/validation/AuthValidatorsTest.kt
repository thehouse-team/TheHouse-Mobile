package com.thehouse.app.domain.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pruebas unitarias de validadores de auth (RF-01/RF-02). */
class AuthValidatorsTest {

    @Test
    fun `login válido no reporta errores`() {
        assertNull(AuthValidators.validateEmail("usuario@correo.mx"))
        assertNull(AuthValidators.validatePassword("password123"))
        val result = AuthValidators.validateLogin("usuario@correo.mx", "password123")
        assertTrue(result.isValid)
        assertNull(result.asError())
    }

    @Test
    fun `email inválido se traduce a español`() {
        val empty = AuthValidators.validateEmail("")
        val bad = AuthValidators.validateEmail("usuarioATcorreo.mx")
        assertTrue(empty != null && empty.contains("correo", ignoreCase = true))
        assertTrue(bad != null && bad.contains("correo", ignoreCase = true))
    }

    @Test
    fun `password corta muestra mínimo en español`() {
        val message = AuthValidators.validatePassword("abc")
        assertTrue(message != null && message.contains("8"))
    }

    @Test
    fun `nombre muy corto se rechaza`() {
        val message = AuthValidators.validateName("a")
        assertTrue(message != null && message.contains("nombre", ignoreCase = true))
    }

    @Test
    fun `registro sin consentimientos falla`() {
        val result = AuthValidators.validateRegistration(
            name = "Victor",
            email = "usuario@correo.mx",
            password = "password123",
            acceptedTerms = false,
            acceptedPrivacy = false,
        )
        assertFalse(result.isValid)
        assertEquals(2, result.errors.count { it.contains("aviso de privacidad", ignoreCase = true) || it.contains("términos", ignoreCase = true) })
    }

    @Test
    fun `registro completo es válido`() {
        val result = AuthValidators.validateRegistration(
            name = "Victor",
            email = "usuario@correo.mx",
            password = "password123",
            acceptedTerms = true,
            acceptedPrivacy = true,
        )
        assertTrue(result.isValid)
    }
}
