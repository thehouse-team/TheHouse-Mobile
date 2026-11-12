package com.thehouse.app.domain.repository

import com.thehouse.app.domain.model.University
import com.thehouse.app.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** Contrato de Domain para el perfil del usuario (D11). Data lo implementa. */
interface ProfileRepository {
    val profileState: Flow<UserProfile?>
    val universityState: Flow<University?>
    val quickSetupState: StateFlow<Boolean>

    suspend fun loadProfile()
    suspend fun updateProfile(name: String?)
    suspend fun selectUniversity(universityId: String?)
}
