package com.thehouse.app.domain.usecase

import com.thehouse.app.domain.room.RoomDetail

/** Contrato de Domain para el detalle de cuarto; Data lo implementa. */
interface RoomDetailRepository {
    suspend fun getRoomDetail(roomId: String): Result<RoomDetail>
}

class GetRoomDetailUseCase(
    private val repository: RoomDetailRepository,
) {
    suspend operator fun invoke(roomId: String): Result<RoomDetail> = repository.getRoomDetail(roomId)
}
