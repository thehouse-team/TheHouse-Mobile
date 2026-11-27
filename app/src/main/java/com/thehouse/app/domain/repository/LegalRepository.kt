package com.thehouse.app.domain.repository

import com.thehouse.app.domain.error.AccountDeletionError
import com.thehouse.app.domain.model.LegalInfo

interface LegalRepository {
    suspend fun getLegal(): Result<LegalInfo>

    /** 409 legal_version_outdated se reporta como [AccountDeletionError.LegalVersionOutdated]. */
    suspend fun acceptLegal(termsVersion: String, privacyVersion: String): Result<Unit>
}
