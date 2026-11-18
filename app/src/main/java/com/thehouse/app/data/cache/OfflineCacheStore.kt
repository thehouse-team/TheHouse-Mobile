package com.thehouse.app.data.cache

import com.thehouse.app.data.preferences.PreferencesStore
import kotlinx.coroutines.flow.first
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class CacheEnvelope<T>(
    val updatedAt: Long,
    val userScope: String? = null,
    val payload: T,
)

/**
 * Cache JSON simple sobre [PreferencesStore] (DataStore). Sin segunda base Room.
 * Keys estables; scope opcional para datos privados por usuario.
 */
class OfflineCacheStore(
    private val preferences: PreferencesStore,
    val json: Json,
) {
    suspend fun <T> put(
        key: String,
        value: T,
        serializer: KSerializer<T>,
        updatedAt: Long = System.currentTimeMillis(),
        userScope: String? = null,
    ) {
        val envelope = CacheEnvelope(updatedAt = updatedAt, userScope = userScope, payload = value)
        val raw = json.encodeToString(CacheEnvelope.serializer(serializer), envelope)
        preferences.putString(key, raw)
    }

    suspend fun <T> get(key: String, serializer: KSerializer<T>): CacheEnvelope<T>? {
        val raw = preferences.observeString(key).first() ?: return null
        return runCatching { json.decodeFromString(CacheEnvelope.serializer(serializer), raw) }.getOrNull()
    }

    suspend fun remove(key: String) {
        preferences.remove(key)
    }

    object Keys {
        const val UNIVERSITIES_PAGE_1 = "cache_public_universities_p1"
        const val PROPERTIES_PAGE_1 = "cache_public_properties_p1"
        fun propertyDetail(propertyId: String) = "cache_public_property_$propertyId"

        fun favorites(userId: String) = "cache_user_${userId}_favorites"
        fun favoriteIds(userId: String) = "cache_user_${userId}_favorite_ids"
        fun profile(userId: String) = "cache_user_${userId}_profile"
        fun preferences(userId: String) = "cache_user_${userId}_preferences"

        fun privateKeysFor(userId: String): List<String> = listOf(
            favorites(userId),
            favoriteIds(userId),
            profile(userId),
            preferences(userId),
        )
    }
}