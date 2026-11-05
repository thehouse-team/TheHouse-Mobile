package com.thehouse.app.data.auth.repository

import com.thehouse.app.data.auth.mapper.AuthMappers.toDomain
import com.thehouse.app.data.auth.session.InMemorySessionStore
import com.thehouse.app.data.auth.storage.SessionTokenStorage
import com.thehouse.app.data.error.DataError
import com.thehouse.app.data.error.toDomainError
import com.thehouse.app.data.remote.api.AuthApi
import com.thehouse.app.data.remote.dto.auth.AuthResponseDto
import com.thehouse.app.data.remote.dto.auth.LoginRequestDto
import com.thehouse.app.data.remote.dto.auth.RegisterRequestDto
import com.thehouse.app.domain.error.DomainError
import com.thehouse.app.domain.error.DomainException
import com.thehouse.app.domain.model.SessionState
import com.thehouse.app.domain.model.UserSession
import com.thehouse.app.domain.repository.AuthRepository
import java.io.IOException
import java.net.SocketTimeoutException
import kotlinx.coroutines.flow.StateFlow
import retrofit2.Response

/**
 * Implementación Data de [AuthRepository].
 *
 * Reglas U2 (spec NV-04):
 * - Access token: SOLO en memoria ([InMemorySessionStore]); nunca se serializa
 *   ni se loguea.
 * - Refresh token: cifrado AES-GCM + Android Keystore, persistido excluido de
 *   backup. Si el backend no entrega refresh token, no se persiste nada.
 * - `/auth/refresh` NO está confirmado: NO se implementa Authenticator
 *   ficticio. Ante 401, se limpia la sesión y la UI regresa a Login.
 * - Logout: intenta POST /api/v1/auth/logout y SIEMPRE limpia el estado local,
 *   incluso sin red.
 */
class AuthRepositoryImpl(
    private val api: AuthApi,
    private val sessionStore: InMemorySessionStore,
    private val refreshTokenStorage: SessionTokenStorage,
) : AuthRepository {

    override val session: StateFlow<SessionState> = sessionStore.state

    override suspend fun register(
        name: String,
        email: String,
        password: String,
        acceptedTerms: Boolean,
        acceptedPrivacy: Boolean,
    ): Result<UserSession?> = runNetwork {
        toSessionResult(
            api.register(
                RegisterRequestDto(
                    name = name.trim(),
                    email = email.trim(),
                    password = password,
                    acceptedTerms = acceptedTerms,
                    acceptedPrivacy = acceptedPrivacy,
                ),
            ),
        )
    }

    override suspend fun login(email: String, password: String): Result<UserSession?> =
        runNetwork {
            toSessionResult(
                api.login(LoginRequestDto(email = email.trim(), password = password)),
            )
        }

    override suspend fun logout(): Result<Unit> {
        // Limpieza local SIEMPRE: criterio de seguridad de la spec (logout local).
        sessionStore.clear()
        refreshTokenStorage.clear()

        return try {
            val response = api.logout()
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(DomainException(DataError.Http(response.code()).toDomainError()))
            }
        } catch (timeout: SocketTimeoutException) {
            // Sin red: logout local inmediato ya aplicado.
            Result.failure(DomainException(DomainError.Timeout))
        } catch (network: IOException) {
            Result.failure(DomainException(DomainError.Network))
        } catch (unexpected: Exception) {
            Result.failure(DomainException(DomainError.Unknown))
        }
    }

    private suspend fun runNetwork(
        block: suspend () -> Result<SessionPayload>,
    ): Result<UserSession?> =
        try {
            val payload = block().getOrThrow()
            if (payload.user == null) {
                // Sin datos de usuario: no se asume login automático; la sesión no
                // se activa y la UI ve Anonymous (contrato de respuesta no verificado).
                sessionStore.clear()
                Result.success(null)
            } else {
                sessionStore.activate(payload.user, payload.accessToken)
                payload.refreshToken?.let {
                    refreshTokenStorage.writeRefreshToken(it.toByteArray(Charsets.UTF_8))
                }
                Result.success(payload.user)
            }
        } catch (auth: DomainException) {
            sessionStore.clear()
            Result.failure(auth)
        } catch (timeout: SocketTimeoutException) {
            sessionStore.clear()
            Result.failure(DomainException(DomainError.Timeout))
        } catch (network: IOException) {
            sessionStore.clear()
            Result.failure(DomainException(DomainError.Network))
        } catch (unexpected: Exception) {
            sessionStore.clear()
            Result.failure(DomainException(DomainError.Unknown))
        }

    private fun toSessionResult(response: Response<AuthResponseDto>): Result<SessionPayload> =
        when {
            response.isSuccessful && response.body() != null -> {
                val dto = response.body()!!
                Result.success(
                    SessionPayload(
                        user = dto.toDomain(),
                        accessToken = dto.accessToken ?: dto.token,
                        refreshToken = dto.refreshToken,
                    ),
                )
            }
            response.isSuccessful ->
                Result.failure(DomainException(DomainError.Unknown))
            else ->
                Result.failure(DomainException(DataError.Http(response.code()).toDomainError()))
        }

    private data class SessionPayload(
        val user: UserSession?,
        val accessToken: String?,
        val refreshToken: String?,
    )
}
