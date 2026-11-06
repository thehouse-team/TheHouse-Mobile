package com.thehouse.app.data.auth.repository

import com.thehouse.app.data.auth.mapper.AuthMappers.toDomain
import com.thehouse.app.data.auth.session.InMemorySessionStore
import com.thehouse.app.data.auth.storage.SessionTokenStorage
import com.thehouse.app.data.error.DataError
import com.thehouse.app.data.error.toDomainError
import com.thehouse.app.data.remote.api.AuthApi
import com.thehouse.app.data.remote.dto.auth.AuthResponseDto
import com.thehouse.app.data.remote.dto.auth.LoginRequestDto
import com.thehouse.app.data.remote.dto.auth.RefreshRequestDto
import com.thehouse.app.data.remote.dto.auth.RegisterRequestDto
import com.thehouse.app.domain.error.DomainError
import com.thehouse.app.domain.error.DomainException
import com.thehouse.app.domain.model.SessionState
import com.thehouse.app.domain.model.UserSession
import com.thehouse.app.domain.repository.AuthRepository
import java.io.IOException
import java.net.SocketTimeoutException
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.Response

/**
 * Implementación Data de [AuthRepository] (D3: sesión persistente).
 *
 * Reglas D3:
 * - Access token: SOLO en memoria ([InMemorySessionStore]); nunca se serializa
 *   ni se loguea.
 * - Refresh token: cifrado AES-GCM + Android Keystore, persistido excluido de
 *   backup. Rotación obligatoria: tras refresh 200 SIEMPRE se reemplaza el
 *   anterior (el backend invalida el anterior).
 * - Single-flight: ante N 401 simultáneos solo UNA request ejecuta
 *   `/auth/refresh`; las demás esperan el mismo resultado (Mutex + doble
 *   verificación de token). Máximo 1 retry por request original.
 * - Refresh 401: borra access + refresh, SessionState Anonymous (a Login).
 * - `/users/me`: solo `id`, `email`, `createdAt`. Sin /me en respuesta
 *   login/register se hidrata con /me (fallback al mapeo tolerante solo
 *   si /me no responde; nunca se usa "Bienvenido" como identidad con /me OK).
 * - Logout: limpia SIEMPRE local, luego intenta backend (Bearer).
 * - Restauración: sin refresh → Anonymous; refresh 200 + /me 200 → Active;
 *   refresh 401 → limpieza (nunca se inventa sesión offline).
 */
class AuthRepositoryImpl(
    private val api: AuthApi,
    private val sessionStore: InMemorySessionStore,
    private val refreshTokenStorage: SessionTokenStorage,
) : AuthRepository {

    private val refreshMutex = Mutex()

    override val session: StateFlow<SessionState> = sessionStore.state

    override suspend fun register(
        name: String,
        email: String,
        password: String,
        acceptedTerms: Boolean,
        acceptedPrivacy: Boolean,
    ): Result<UserSession?> = handleAuthResponse(
        api.register(
            // Contrato backend: SOLO email y password. `nombre` y
            // consentimientos son solo UI y NO se envían.
            RegisterRequestDto(
                email = email.trim(),
                password = password,
            ),
        ),
    )

    override suspend fun login(email: String, password: String): Result<UserSession?> =
        handleAuthResponse(
            api.login(LoginRequestDto(email = email.trim(), password = password)),
        )

    override suspend fun logout(): Result<Unit> {
        // Capturar token antes de limpiar: logout lleva Bearer (contrato D3).
        val token = sessionStore.accessToken
        // Limpieza local SIEMPRE: criterio de seguridad de la spec (logout local).
        sessionStore.clear()
        refreshTokenStorage.clear()

        return try {
            val response = api.logout(token?.let { "Bearer $it" })
            if (response.isSuccessful || response.code() == 204) {
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

    /**
     * Refresh single-flight: N llamadores concurrentes → 1 sola llamada HTTP.
     * La doble verificación de token evita refresh duplicado con el mismo
     * refreshToken (que podría cerrar la sesión en el backend).
     */
    override suspend fun refreshAccessToken(): Result<String?> =
        refreshMutex.withLock { doRefreshLocked(oldAccessToken = null) }

    /**
     * Variante con token anterior observado por el llamador (p. ej. el
     * accessToken que recibió 401). Si otro hilo ya refrescó (token
     * distinto y no nulo), devuelve el nuevo sin otra llamada HTTP.
     */
    private suspend fun refreshIfStale(oldAccessToken: String?): Result<String?> =
        refreshMutex.withLock { doRefreshLocked(oldAccessToken = oldAccessToken) }

    private suspend fun doRefreshLocked(oldAccessToken: String?): Result<String?> {
        val current = sessionStore.accessToken
        if (oldAccessToken != null && current != null && current != oldAccessToken) {
            return Result.success(current)
        }
        val refreshToken = refreshTokenStorage.readRefreshToken()
            ?.toString(Charsets.UTF_8)
        if (refreshToken.isNullOrBlank()) {
            sessionStore.clear()
            refreshTokenStorage.clear()
            return Result.failure(DomainException(DomainError.Unauthorized))
        }
        return try {
            val response = api.refresh(RefreshRequestDto(refreshToken = refreshToken))
            val dto = response.body()
            val newAccess = dto?.accessToken ?: dto?.token
            val newRefresh = dto?.refreshToken
            when {
                response.isSuccessful && !newAccess.isNullOrBlank() -> {
                    sessionStore.updateAccessToken(newAccess)
                    // Rotación: SIEMPRE reemplazar el refresh anterior.
                    if (newRefresh != null) {
                        refreshTokenStorage.writeRefreshToken(newRefresh.toByteArray(Charsets.UTF_8))
                    } else {
                        refreshTokenStorage.clear()
                    }
                    Result.success(newAccess)
                }
                response.code() == 401 -> {
                    // Refresh 401 → cierre de sesión local total.
                    sessionStore.clear()
                    refreshTokenStorage.clear()
                    Result.failure(DomainException(DomainError.Unauthorized))
                }
                else -> {
                    Result.failure(
                        DomainException(DataError.Http(response.code()).toDomainError()),
                    )
                }
            }
        } catch (timeout: SocketTimeoutException) {
            Result.failure(DomainException(DomainError.Timeout))
        } catch (network: IOException) {
            Result.failure(DomainException(DomainError.Network))
        } catch (unexpected: Exception) {
            Result.failure(DomainException(DomainError.Unknown))
        }
    }

    override suspend fun getCurrentUser(): Result<UserSession?> {
        val token = sessionStore.accessToken
        if (token.isNullOrBlank()) {
            return Result.failure(DomainException(DomainError.Unauthorized))
        }
        val first = fetchMe(token)
        if (first.isSuccess) return first
        val error = first.exceptionOrNull() as? DomainException
        if (error?.error != DomainError.Unauthorized) return first
        // 401 → refresh single-flight (máximo 1) y retry único.
        val refreshed = refreshIfStale(oldAccessToken = token).getOrNull()
        if (refreshed.isNullOrBlank()) {
            // Refresh 401 ya limpió sesión (doRefreshLocked); 401 se propaga.
            return Result.failure(DomainException(DomainError.Unauthorized))
        }
        if (sessionStore.state.value is SessionState.Anonymous) {
            return Result.failure(DomainException(DomainError.Unauthorized))
        }
        val retry = fetchMe(refreshed)
        if (retry.isFailure) {
            val retryError = retry.exceptionOrNull() as? DomainException
            if (retryError?.error == DomainError.Unauthorized) {
                sessionStore.clear()
                refreshTokenStorage.clear()
            }
        }
        return retry
    }

    /** Fetch de `/users/me` sin lógica de refresh (usable para restore sin doble refresh). */
    private suspend fun fetchMe(accessToken: String): Result<UserSession?> {
        return try {
            val response = api.getCurrentUser("Bearer $accessToken")
            val dto = response.body()
            when {
                response.isSuccessful && dto != null -> {
                    val user = dto.toDomain()
                    sessionStore.activate(user, accessToken)
                    Result.success(user)
                }
                else -> Result.failure(
                    DomainException(DataError.Http(response.code()).toDomainError()),
                )
            }
        } catch (timeout: SocketTimeoutException) {
            Result.failure(DomainException(DomainError.Timeout))
        } catch (network: IOException) {
            Result.failure(DomainException(DomainError.Network))
        } catch (unexpected: Exception) {
            Result.failure(DomainException(DomainError.Unknown))
        }
    }

    override suspend fun restoreSession(): Result<UserSession?> {
        val refreshToken = refreshTokenStorage.readRefreshToken()
            ?.toString(Charsets.UTF_8)
        if (refreshToken.isNullOrBlank()) {
            sessionStore.clear()
            return Result.success(null)
        }
        // Refresh single-flight (ruta de arranque; secencial, sin doble refresh).
        val newAccess = refreshAccessToken().getOrNull()
        if (newAccess.isNullOrBlank()) {
            // Refresh 401 ya limpió; otros errores dejan Anonymous sin inventar sesión.
            sessionStore.clear()
            return Result.failure(DomainException(DomainError.Unauthorized))
        }
        // hidratar identidad con /me (sin segundo refresh: token recién estrenado).
        val me = fetchMe(newAccess)
        return if (me.isSuccess) {
            me
        } else {
            val error = me.exceptionOrNull() as? DomainException
            if (error?.error == DomainError.Unauthorized) {
                sessionStore.clear()
                refreshTokenStorage.clear()
            } else {
                // Red/timeout al hidratar: no se inventa sesión offline.
                sessionStore.clear()
            }
            Result.failure(DomainException(error?.error ?: DomainError.Unknown))
        }
    }

    /**
     * Maneja la respuesta de register/login: guarda tokens, hidrata con
     * `/users/me` si la respuesta no trae identidad, activa la sesión.
     * Si /me está disponible, la identidad es real (nunca "Bienvenido").
     */
    private suspend fun handleAuthResponse(
        response: Response<AuthResponseDto>,
    ): Result<UserSession?> {
        val payload = try {
            toSessionPayload(response).getOrThrow()
        } catch (auth: DomainException) {
            sessionStore.clear()
            return Result.failure(auth)
        } catch (timeout: SocketTimeoutException) {
            sessionStore.clear()
            return Result.failure(DomainException(DomainError.Timeout))
        } catch (network: IOException) {
            sessionStore.clear()
            return Result.failure(DomainException(DomainError.Network))
        } catch (unexpected: Exception) {
            sessionStore.clear()
            return Result.failure(DomainException(DomainError.Unknown))
        }

        // Guardar tokens: access solo en memoria; refresh cifrado.
        sessionStore.updateAccessToken(payload.accessToken)
        payload.refreshToken?.let {
            refreshTokenStorage.writeRefreshToken(it.toByteArray(Charsets.UTF_8))
        }

        // Si la respuesta ya trae identidad, usarla (compat U2).
        if (!payload.needsCurrentUser || payload.accessToken.isNullOrBlank()) {
            sessionStore.activate(payload.user, payload.accessToken)
            return Result.success(payload.user)
        }

        // La respuesta solo trae tokens (contrato D3: login = tokens, sin user).
        // Hidratar con /me; si falla, fallback al mapeo tolerante (sin 401 local).
        val hydrated = try {
            fetchMe(payload.accessToken).getOrNull()
        } catch (_: Exception) {
            null
        }
        return if (hydrated != null) {
            Result.success(hydrated)
        } else {
            // /me no disponible en esta respuesta: no inventar identidad real;
            // se conserva el mapeo tolerante mínimo (placeholder) para no
            // perder la sesión recién autenticada. El refresh posterior con
            // /me lo reemplazará cuando el backend lo sirva.
            sessionStore.activate(payload.user, payload.accessToken)
            Result.success(payload.user)
        }
    }

    private fun toSessionPayload(response: Response<AuthResponseDto>): Result<SessionPayload> =
        when {
            response.isSuccessful && response.body() != null -> {
                val dto = response.body()!!
                val user = dto.toDomain()
                Result.success(
                    SessionPayload(
                        user = user,
                        accessToken = dto.accessToken ?: dto.token,
                        refreshToken = dto.refreshToken,
                        needsCurrentUser = (dto.id == null && dto.userId == null &&
                            dto.email == null && dto.user?.id == null &&
                            dto.user?.userId == null && dto.user?.email == null),
                    ),
                )
            }
            response.isSuccessful ->
                Result.failure(DomainException(DomainError.Unknown))
            else ->
                Result.failure(DomainException(DataError.Http(response.code()).toDomainError()))
        }

    private data class SessionPayload(
        val user: UserSession,
        val accessToken: String?,
        val refreshToken: String?,
        val needsCurrentUser: Boolean,
    )
}
