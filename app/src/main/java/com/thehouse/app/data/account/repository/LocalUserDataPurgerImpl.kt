package com.thehouse.app.data.account.repository

import com.thehouse.app.data.favorites.state.FavoriteIdsStore
import com.thehouse.app.data.notifications.push.PendingNotificationNavigation
import com.thehouse.app.domain.auth.LocalSessionCleaner
import com.thehouse.app.domain.common.DispatcherProvider
import com.thehouse.app.domain.push.LocalPushNotifier
import com.thehouse.app.domain.repository.LocalUserDataPurger
import com.thehouse.app.domain.repository.OnboardingRepository
import kotlinx.coroutines.withContext
import okhttp3.Cache

/**
 * Limpieza local posterior a una eliminación de cuenta confirmada.
 *
 * Alcance deliberadamente acotado:
 * - sesión y refresh token (primero, para que un reinicio no restaure nada);
 * - marca de onboarding del usuario eliminado;
 * - ids de favoritos en memoria;
 * - target pendiente de push y notificaciones locales;
 * - caché HTTP (puede contener respuestas autenticadas).
 *
 * NO borra la selección global de universidad, imágenes públicas de Coil ni
 * preferencias de dispositivo no asociadas a la cuenta.
 */
class LocalUserDataPurgerImpl(
    private val onboardingRepository: OnboardingRepository,
    private val favoritesStore: FavoriteIdsStore,
    private val pendingNotificationNavigation: PendingNotificationNavigation,
    private val localPushNotifier: LocalPushNotifier,
    private val httpCache: Cache?,
    private val localSessionCleaner: LocalSessionCleaner,
    private val dispatchers: DispatcherProvider,
) : LocalUserDataPurger {

    override suspend fun purge(userId: String?) {
        // 1. Seguridad primero: sin sesión local aunque el resto falle.
        runCatching { localSessionCleaner.clearLocalSession() }

        // 2. Solo la marca del usuario eliminado; nunca la clave global "local"
        //    ni la de otro usuario.
        val normalizedUserId = userId?.takeIf { it.isNotBlank() }
        if (normalizedUserId != null) {
            runCatching {
                onboardingRepository.clear(onboardingRepository.keyFor(normalizedUserId))
            }
        }

        // 3. Estado/caché asociado a la cuenta.
        runCatching { favoritesStore.clear() }
        runCatching { pendingNotificationNavigation.clear() }
        runCatching { localPushNotifier.cancelAll() }
        withContext(dispatchers.io) {
            runCatching { httpCache?.evictAll() }
        }
    }
}
