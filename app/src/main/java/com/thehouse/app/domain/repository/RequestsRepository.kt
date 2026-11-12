package com.thehouse.app.domain.repository

import com.thehouse.app.domain.error.RequestException
import com.thehouse.app.domain.model.PagedResult
import com.thehouse.app.domain.model.QuickQuestion
import com.thehouse.app.domain.model.RequestStatus
import com.thehouse.app.domain.model.RoomRequest

interface RequestsRepository {

    suspend fun createRequest(
        roomId: String,
        quickQuestions: List<QuickQuestion>,
        message: String?,
    ): Result<RoomRequest>

    suspend fun getRequests(
        status: RequestStatus?,
        page: Int,
        pageSize: Int,
    ): Result<PagedResult<RoomRequest>>

    suspend fun getRequest(id: String): Result<RoomRequest>

    /** Cancel cancela (student). Reintento con estado ya cancelado es idempotente (200). */
    suspend fun cancelRequest(id: String): Result<RoomRequest>

    /** Lista del arrendador; falla con landlord_required si el modo no está activo. */
    suspend fun getLandlordRequests(
        status: RequestStatus?,
        roomId: String?,
        page: Int,
        pageSize: Int,
    ): Result<PagedResult<RoomRequest>>

    suspend fun getLandlordRequest(id: String): Result<RoomRequest>

    suspend fun acceptRequest(id: String): Result<RoomRequest>

    suspend fun rejectRequest(id: String, reason: String, note: String?): Result<RoomRequest>
}

/** Excepción típica de failure: [RequestException]. */
