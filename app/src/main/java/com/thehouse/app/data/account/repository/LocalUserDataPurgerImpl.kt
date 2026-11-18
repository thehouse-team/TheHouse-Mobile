package com.thehouse.app.data.account.repository

import com.thehouse.app.data.cache.OfflineCacheStore
import com.thehouse.app.data.favorites.state.FavoriteIdsStore
import com.thehouse.app.data.notifications.push.PendingNotificationNavigation
import com.thehouse.app.domain.auth.LocalSessionCleaner
import com.thehouse.app.domain.common.DispatcherProvider
import com.thehouse.app.domain.push.LocalPushNotifier
import com.thehouse.app.domain.repository.LocalUserDataPurger
import com.thehouse.app.domain.repository.OnboardingRepository
import kotlinx.coroutines.withContext
import okhttp3.Cache

class LocalUserDataPurgerImpl(
    private val onboardingRepository: OnboardingRepository,
    private val favoritesStore: FavoriteIdsStore,
    private val pendingNotificationNavigation: PendingNotificationNavigation,
    private val localPushNotifier: LocalPushNotifier,
    private val httpCache: Cache?,
    private val localSessionCleaner: LocalSessionCleaner,
    private val dispatchers: DispatcherProvider,
    private val offlineCacheStore: OfflineCacheStore? = null,
) : LocalUserDataPurger {

    override suspend fun purge(userId: String?) {
        runCatching { localSessionCleaner.clearLocalSession() }

        val normalizedUserId = userId?.takeIf { it.isNotBlank() }
        if (normalizedUserId != null) {
            runCatching {
                onboardingRepository.clear(onboardingRepository.keyFor(normalizedUserId))
            }
            runCatching {
                OfflineCacheStore.Keys.privateKeysFor(normalizedUserId).forEach { key ->
                    offlineCacheStore?.remove(key)
                }
            }
        }

        runCatching { favoritesStore.clear() }
        runCatching { pendingNotificationNavigation.clear() }
        runCatching { localPushNotifier.cancelAll() }
        withContext(dispatchers.io) {
            runCatching { httpCache?.evictAll() }
        }
    }
}