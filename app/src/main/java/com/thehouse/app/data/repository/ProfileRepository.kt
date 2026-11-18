package com.thehouse.app.data.repository

import com.thehouse.app.data.cache.OfflineCacheStore
import com.thehouse.app.data.cache.ProfileSnapshot
import com.thehouse.app.data.remote.api.PatchProfileRequest
import com.thehouse.app.data.remote.api.ProfileApi
import com.thehouse.app.data.remote.api.PutUniversityRequest
import com.thehouse.app.data.remote.dto.profile.toDomain
import com.thehouse.app.domain.model.University
import com.thehouse.app.domain.model.UserProfile
import com.thehouse.app.domain.repository.ProfileRepository
import java.io.IOException
import java.net.SocketTimeoutException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import retrofit2.HttpException

class ProfileRepositoryImpl(
    private val api: ProfileApi,
    private val cache: OfflineCacheStore? = null,
    private val currentUserId: () -> String? = { null },
) : ProfileRepository {

    private val _profileState = MutableStateFlow<UserProfile?>(null)
    override val profileState: Flow<UserProfile?> = _profileState.asStateFlow()

    private val _universityState = MutableStateFlow<University?>(null)
    override val universityState: Flow<University?> = _universityState.asStateFlow()

    private val _quickSetupState = MutableStateFlow(false)
    override val quickSetupState: StateFlow<Boolean> = _quickSetupState.asStateFlow()

    override suspend fun loadProfile() {
        val userId = currentUserId()
        val cacheKey = userId?.let { OfflineCacheStore.Keys.profile(it) }
        try {
            val dto = api.getProfile()
            val profile = dto.toDomain()
            applyProfile(profile)
            if (cacheKey != null) {
                cache?.put(cacheKey, ProfileSnapshot.from(profile), ProfileSnapshot.serializer(), userScope = userId)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: Exception) {
            val cached = cacheKey?.let { cache?.get(it, ProfileSnapshot.serializer()) }?.payload?.toDomain()
            if (cached != null && isCacheFallbackEligible(e)) {
                applyProfile(cached)
                return
            }
            throw e
        }
    }

    override suspend fun updateProfile(name: String?) {
        api.patchProfile(PatchProfileRequest(name = name))
        loadProfile()
    }

    override suspend fun selectUniversity(universityId: String?) {
        api.putUniversity(PutUniversityRequest(universityId = universityId))
        loadProfile()
    }

    private fun applyProfile(profile: UserProfile) {
        _profileState.value = profile
        _quickSetupState.value = profile.quickSetupCompleted
        _universityState.value = profile.university
    }

    private fun isCacheFallbackEligible(e: Exception): Boolean =
        e is IOException || e is SocketTimeoutException || e is HttpException
}