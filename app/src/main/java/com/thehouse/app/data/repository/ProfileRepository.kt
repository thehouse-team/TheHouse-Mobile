package com.thehouse.app.data.repository

import com.thehouse.app.data.remote.api.ProfileApi
import com.thehouse.app.data.remote.api.PutUniversityRequest
import com.thehouse.app.data.remote.api.PatchProfileRequest
import com.thehouse.app.data.remote.dto.profile.toDomain
import com.thehouse.app.domain.model.University
import com.thehouse.app.domain.model.UserProfile
import com.thehouse.app.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Implementación Data del contrato de Domain (D11). Sin lógica de negocio. */
class ProfileRepositoryImpl(
    private val api: ProfileApi
) : ProfileRepository {

    private val _profileState = MutableStateFlow<UserProfile?>(null)
    override val profileState: Flow<UserProfile?> = _profileState.asStateFlow()

    private val _universityState = MutableStateFlow<University?>(null)
    override val universityState: Flow<University?> = _universityState.asStateFlow()

    private val _quickSetupState = MutableStateFlow(false)
    override val quickSetupState: StateFlow<Boolean> = _quickSetupState.asStateFlow()

    override suspend fun loadProfile() {
        val dto = api.getProfile()
        _profileState.value = dto.toDomain()
        _quickSetupState.value = dto.quickSetupCompleted
        // Extraer universidad aparte para observación independiente
        _universityState.value = dto.university?.toDomain()
    }

    override suspend fun updateProfile(name: String?) {
        api.patchProfile(PatchProfileRequest(name = name))
        loadProfile()
    }

    override suspend fun selectUniversity(universityId: String?) {
        api.putUniversity(PutUniversityRequest(universityId = universityId))
        loadProfile()
    }
}
