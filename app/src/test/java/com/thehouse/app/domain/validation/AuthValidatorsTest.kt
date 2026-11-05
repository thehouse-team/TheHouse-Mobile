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
    fun `password de registro menor a 12 se rechaza`() {
        val result = AuthValidators.validateRegistration(
            name = "Victor",
            email = "usuario@correo.mx",
            password = "Pass12!",
            acceptedTerms = true,
            acceptedPrivacy = true,
        )
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("12") })
    }

    @Test
    fun `password de registro mayor a 128 se rechaza`() {
        val result = AuthValidators.validateRegistration(
            name = "Victor",
            email = "usuario@correo.mx",
            password = "a".repeat(129),
            acceptedTerms = true,
            acceptedPrivacy = true,
        )
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("128") })
    }

    @Test
    fun `password de registro de 12 caracteres es válida`() {
        val result = AuthValidators.validateRegistration(
            name = "Victor",
            email = "usuario@correo.mx",
            password = "PasswordSegura123!",
            acceptedTerms = true,
            acceptedPrivacy = true,
        )
        assertTrue(result.isValid)
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
            password = "PasswordSegura123!",
            acceptedTerms = true,
            acceptedPrivacy = true,
        )
        assertTrue(result.isValid)
    }

    @Test
    fun `registro con password de 12 caracteres válido`() {
        val result = AuthValidators.validateRegistration(
            name = "Victor",
            email = "usuario@correo.mx",
            password = "PasswordSeg1",
            acceptedTerms = true,
            acceptedPrivacy = true,
        )
        assertTrue(result.isValid)
    }
}
