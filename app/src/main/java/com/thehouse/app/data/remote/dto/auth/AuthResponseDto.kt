package com.thehouse.app.data.remote.dto.auth

import kotlinx.serialization.Serializable

/**
 * DTOs de autenticación del backend TheHouse.
 *
 * Solo se declaran los endpoints confirmados:
 * POST /api/v1/auth/register, POST /api/v1/auth/login, POST /api/v1/auth/logout.
 * La respuesta se modela de forma tolerante (campos opcionales) porque el
 * contrato detallado de respuesta aún no se capturó: no se inventan campos
 * obligatorios ni endpoints adicionales.
 */
/**
 * Contrato real confirmado del backend (POST /api/v1/auth/register):
 * body con EXACTAMENTE `email` y `password`. Campos extra NO permitidos:
 * no se envía `name` ni consentimientos (seman en la UI, no viajan al backend).
 */
@Serializable
data class RegisterRequestDto(
    val email: String,
    val password: String,
)

@Serializable
data class LoginRequestDto(
    val email: String,
    val password: String,
)

/**
 * Respuesta tolerante de register/login.
 *
 * - `token`/`accessToken`: access token devuelto por el backend, si existe.
 * - `refreshToken`: si existe, se cifra (AES-GCM + Keystore) antes de persistir.
 * - `user`: objeto anidado opcional con los datos del usuario.
 */
@Serializable
data class AuthResponseDto(
    val id: String? = null,
    val userId: String? = null,
    val name: String? = null,
    val displayName: String? = null,
    val email: String? = null,
    val token: String? = null,
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val user: AuthUserDto? = null,
)

@Serializable
data class AuthUserDto(
    val id: String? = null,
    val userId: String? = null,
    val name: String? = null,
    val email: String? = null,
)

/**
 * RFC 9457 Problem Details (errores del backend, como los que retorna .NET 10).
 */
@Serializable
data class ProblemDetailsDto(
    val type: String? = null,
    val title: String? = null,
    val status: Int? = null,
    val detail: String? = null,
)
