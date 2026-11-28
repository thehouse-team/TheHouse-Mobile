package com.thehouse.app.domain.device

import com.thehouse.app.domain.connectivity.ConnectivityObserver
import com.thehouse.app.domain.device.DeviceError
import com.thehouse.app.domain.device.UnregisterOutcome
import com.thehouse.app.domain.model.SessionState
import com.thehouse.app.domain.repository.DeviceException
import com.thehouse.app.domain.repository.DeviceRepository
import com.thehouse.app.domain.repository.InstallationIdStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/**
 * Coordina el registro/desvinculación de dispositivos con el Backend.
 *
 * Garantías:
 * - Un solo registro por clave (userId, installationId, token): ni duplicados
 *   por recomposición ni registros concurrentes.
 * - Ningún registro para guest.
 * - Al cambiar de cuenta se cancelan trabajos pendientes y se limpia el token
 *   en memoria para que la cuenta B no herede sincronizaciones de la A.
 * - Reintento único ante recuperación de red cuando existió un fallo de red.
 */
class DeviceSyncCoordinator(
    private val repository: DeviceRepository,
    private val installationIdStore: InstallationIdStore,
    private val sessionState: StateFlow<SessionState>,
    private val connectivity: ConnectivityObserver,
    private val appScope: CoroutineScope,
    private val tokenProvider: () -> String?,
    private val tokenClear: () -> Unit,
    private val logger: (String) -> Unit = {},
) {
    private val registrationMutex = Mutex()
    private val inFlight = ConcurrentHashMap<String, Job>()
    /** Claves ya sincronizadas con éxito para el token vigente (anti-recomposición). */
    private val syncedKeys = ConcurrentHashMap.newKeySet<String>()
    private val pendingRetry = ConcurrentHashMap<String, String>()
    private var activeAccount: String? = null
    private var connectivityJob: Job? = null

    /** Registro idempotente: dedup por (cuenta, installationId, token). */
    suspend fun syncIfPossible(): DeviceSyncStatus {
        val state = sessionState.value
        if (state !is SessionState.Active) {
            logger("device_sync_skipped_guest")
            return DeviceSyncStatus.SkippedGuest
        }
        val token = tokenProvider()
        if (token.isNullOrBlank()) {
            logger("device_sync_skipped_no_token")
            return DeviceSyncStatus.SkippedNoToken
        }
        val installationId = installationIdStore.getOrCreate()
        val key = "${state.session.userId}|${installationId.value}|$token"
        return registrationMutex.withLock {
            if (inFlight[key]?.isActive == true || syncedKeys.contains(key)) {
                logger("device_sync_deduplicated")
                return@withLock DeviceSyncStatus.AlreadySynced
            }
            activeAccount = state.session.userId
            val job = appScope.launch {
                val result = try {
                    repository.register(
                        installationId = installationId,
                        fcmToken = token,
                        appVersion = null,
                        locale = "es-MX",
                    )
                } finally {
                    inFlight.remove(key)
                }
                result.fold(
                    onSuccess = {
                        syncedKeys.add(key)
                        pendingRetry.remove(key)
                        logger("device_sync_registered")
                    },
                    onFailure = { error ->
                        syncedKeys.remove(key)
                        val reason = (error as? DeviceException)?.reason
                        if (reason == DeviceError.Network || reason == DeviceError.Timeout) {
                            pendingRetry[key] = token
                            observeConnectivityOnce()
                        }
                        logger("device_sync_failed")
                    },
                )
            }
            inFlight[key] = job
            DeviceSyncStatus.Scheduled
        }
    }

    /** Llamado desde `onNewToken`: el token nuevo invalida la clave anterior. */
    fun onNewToken() {
        appScope.launch { syncIfPossible() }
    }

    /** Restauración de sesión / login: sincroniza si hay sesión y token. */
    fun onSessionRestored() {
        appScope.launch { syncIfPossible() }
    }

    private fun observeConnectivityOnce() {
        if (connectivityJob?.isActive == true) return
        connectivityJob = appScope.launch {
            // Solo reintentar en la transición offline → online. Si ya estaba
            // online cuando falló la red, no disparar un reintento inmediato.
            var sawOffline = !connectivity.isOnline.value
            connectivity.isOnline.collect { online ->
                if (!online) {
                    sawOffline = true
                    return@collect
                }
                if (!sawOffline) return@collect
                val snapshot = pendingRetry.toMap()
                snapshot.forEach { (key, token) ->
                    pendingRetry.remove(key)
                    val parts = key.split("|", limit = 3)
                    if (parts.size == 3) {
                        appScope.launch {
                            val outcome = try {
                                repository.register(
                                    installationId = InstallationId(parts[1]),
                                    fcmToken = token,
                                    appVersion = null,
                                    locale = "es-MX",
                                )
                            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                                return@launch
                            } catch (network: java.io.IOException) {
                                return@launch // reintento en la próxima transición offline→online
                            }
                            // Result se conserva para no afirmar registro remoto no confirmado.
                            outcome.onFailure { pendingRetry[key] = token }
                        }
                    }
                }
                connectivityJob?.cancel()
            }
        }
    }

    /**
     * Desvinculación en logout: se llama MIENTRAS la sesión sigue activa,
     * antes de revocar credenciales. Nunca bloquea: timeout acotado por OkHttp.
     * Devuelve el outcome real; 404 se considera "ya no presente".
     */
    suspend fun unregisterOnLogout(): UnregisterOutcome {
        val state = sessionState.value
        if (state !is SessionState.Active) return UnregisterOutcome.LocalOnly
        val id = installationIdStore.getOrCreate()
        val result = repository.unregister(id)
        return result.fold(
            onSuccess = {
                logger("device_unregistered")
                UnregisterOutcome.RemoteDeleted
            },
            onFailure = { error ->
                val reason = (error as? DeviceException)?.reason
                logger("device_unregister_failed")
                when (reason) {
                    DeviceError.NotFound -> UnregisterOutcome.AlreadyAbsent
                    else -> UnregisterOutcome.RemoteFailure
                }
            },
        )
    }

    /** Aísla la cuenta anterior al hacer logout o cambiar de cuenta. */
    fun clearForAccountSwitch() {
        inFlight.values.forEach { it.cancel() }
        inFlight.clear()
        syncedKeys.clear()
        pendingRetry.clear()
        connectivityJob?.cancel()
        connectivityJob = null
        activeAccount = null
        tokenClear()
        logger("device_sync_cleared_for_switch")
    }

    fun currentAccount(): String? = activeAccount
}

sealed interface DeviceSyncStatus {
    data object Scheduled : DeviceSyncStatus
    data object AlreadySynced : DeviceSyncStatus
    data object SkippedGuest : DeviceSyncStatus
    data object SkippedNoToken : DeviceSyncStatus
}
