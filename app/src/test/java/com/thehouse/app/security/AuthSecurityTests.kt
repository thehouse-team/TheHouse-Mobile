package com.thehouse.app.security

import com.thehouse.app.data.auth.session.InMemorySessionStore
import com.thehouse.app.domain.model.SessionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Security tests explícitos basados en validadores de dominio.
 * Pure functions sin dependencias de Android ni red.
 * Relación OWASP: Security Testing, Input Validation
 */
class AuthSecurityTests {

    @Test
    fun `validacion login vacio rechaza con error en español`() {
        val result = com.thehouse.app.domain.validation.AuthValidators.validateLogin("", "password123")
        assertFalse(result.isValid)
        assertTrue("debe tener error", result.asError() != null)
        assertTrue("error sobre correo", result.asError()!!.contains("correo"))
    }

    @Test
    fun `validacion email invalido rechaza`() {
        val result = com.thehouse.app.domain.validation.AuthValidators.validateEmail("usario-sin-arroba")
        assertTrue("debe tener mensaje", result != null)
    }

    @Test
    fun `validacion password corto rechaza`() {
        val result = com.thehouse.app.domain.validation.AuthValidators.validatePassword("short")
        assertTrue("debo tener mensaje de largo", result != null && result.contains("8"))
    }

    @Test
    fun `validacion password vacio requiere`() {
        val result = com.thehouse.app.domain.validation.AuthValidators.validatePassword("")
        assertTrue("debe requerir password", result != null)
    }

    @Test
    fun `validacion nombre corto rechaza`() {
        val result = com.thehouse.app.domain.validation.AuthValidators.validateName("a")
        assertTrue("debe tener mensaje", result != null && result.contains("nombre"))
    }

    @Test
    fun `validacion registro password 12-128 es valido`() {
        val result = com.thehouse.app.domain.validation.AuthValidators.validateRegistration(
            name = "Victor",
            email = "usuario@correo.mx",
            password = "PasswordSegura123!",
            acceptedTerms = true,
            acceptedPrivacy = true,
        )
        assertTrue("registro valido", result.isValid)
    }

    @Test
    fun `validacion registro password menor a 12 se rechaza`() {
        val result = com.thehouse.app.domain.validation.AuthValidators.validateRegistration(
            name = "Victor",
            email = "usuario@correo.mx",
            password = "Pass12!",
            acceptedTerms = true,
            acceptedPrivacy = true,
        )
        assertFalse("registro menor a 12 invalido", result.isValid)
        assertTrue("mensura 12", result.errors.any { it.contains("12") })
    }

    @Test
    fun `validacion registro password mayor a 128 se rechaza`() {
        val result = com.thehouse.app.domain.validation.AuthValidators.validateRegistration(
            name = "Victor",
            email = "usuario@correo.mx",
            password = "a".repeat(129),
            acceptedTerms = true,
            acceptedPrivacy = true,
        )
        assertFalse("registro mayor a 128 invalido", result.isValid)
        assertTrue("mensura 128", result.errors.any { it.contains("128") })
    }
}