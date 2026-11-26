package com.thehouse.app.data.remote.api

import com.thehouse.app.data.remote.dto.account.AccountDeletionRequestDto
import com.thehouse.app.data.remote.dto.account.DeletionPreviewDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Contrato Backend confirmado (780b7be, §3 de docs/mobile/contrato-pendiente-mobile.md):
 * - `GET /api/v1/users/me/deletion` → preview (canDelete, blockers, effects).
 * - `POST /api/v1/users/me/deletion` → 204; body exacto `{password, confirmation:"ELIMINAR"}`.
 */
interface AccountApi {

    @GET("api/v1/users/me/deletion")
    suspend fun getDeletionPreview(): Response<DeletionPreviewDto>

    @POST("api/v1/users/me/deletion")
    suspend fun deleteAccount(@Body body: AccountDeletionRequestDto): Response<Unit>
}
