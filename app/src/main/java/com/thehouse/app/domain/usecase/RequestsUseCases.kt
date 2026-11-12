package com.thehouse.app.domain.usecase

import com.thehouse.app.domain.model.PagedResult
import com.thehouse.app.domain.model.QuickQuestion
import com.thehouse.app.domain.model.RequestStatus
import com.thehouse.app.domain.model.RoomRequest
import com.thehouse.app.domain.repository.RequestsRepository

class CreateRequestUseCase(private val repository: RequestsRepository) {
    suspend operator fun invoke(
        roomId: String,
        quickQuestions: List<QuickQuestion>,
        message: String?,
    ): Result<RoomRequest> = repository.createRequest(roomId, quickQuestions, message)
}

class GetUserRequestsUseCase(private val repository: RequestsRepository) {
    suspend operator fun invoke(
        status: RequestStatus?,
        page: Int = 1,
        pageSize: Int = 20,
    ): Result<PagedResult<RoomRequest>> = repository.getRequests(status, page, pageSize)
}

class GetRequestUseCase(private val repository: RequestsRepository) {
    suspend operator fun invoke(id: String): Result<RoomRequest> = repository.getRequest(id)
}

class CancelRequestUseCase(private val repository: RequestsRepository) {
    suspend operator fun invoke(id: String): Result<RoomRequest> = repository.cancelRequest(id)
}

class GetLandlordRequestsUseCase(private val repository: RequestsRepository) {
    suspend operator fun invoke(
        status: RequestStatus?,
        roomId: String? = null,
        page: Int = 1,
        pageSize: Int = 20,
    ): Result<PagedResult<RoomRequest>> =
        repository.getLandlordRequests(status, roomId, page, pageSize)
}

class AcceptRequestUseCase(private val repository: RequestsRepository) {
    suspend operator fun invoke(id: String): Result<RoomRequest> = repository.acceptRequest(id)
}

class RejectRequestUseCase(private val repository: RequestsRepository) {
    suspend operator fun invoke(id: String, reason: String, note: String?): Result<RoomRequest> =
        repository.rejectRequest(id, reason, note)
}
