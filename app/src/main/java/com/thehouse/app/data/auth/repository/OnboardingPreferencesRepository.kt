package com.thehouse.app.data.auth.repository

import com.thehouse.app.data.preferences.PreferencesStore
import com.thehouse.app.domain.repository.OnboardingRepository
import kotlinx.coroutines.flow.first

/**
 * Implementación del repositorio de onboarding con DataStore.
 *
 * `onboardingCompleted` es un dato NO sensible por usuario. La clave combina el
 * identificador disponible (id del usuario, o "local" si el backend no lo
 * entrega). El DataStore queda excluido de backup por las reglas de backup U2.
 */
class OnboardingPreferencesRepository(
    private val preferences: PreferencesStore,
) : OnboardingRepository {

    override fun keyFor(userId: String?): String =
        userId?.takeIf { it.isNotBlank() }?.lowercase() ?: DEFAULT_USER_KEY

    override suspend fun isCompleted(key: String): Boolean =
        preferences.observeBoolean(storageKey(key), false).first()

    override suspend fun setCompleted(key: String, completed: Boolean) {
        preferences.putBoolean(storageKey(key), completed)
    }

    fun observeCompleted(key: String): kotlinx.coroutines.flow.Flow<Boolean> =
        preferences.observeBoolean(storageKey(key), false)

    private fun storageKey(key: String): String =
        "onboarding_completed_" + (key.ifBlank { DEFAULT_USER_KEY })

    private companion object {
        const val DEFAULT_USER_KEY = "local"
    }
}
