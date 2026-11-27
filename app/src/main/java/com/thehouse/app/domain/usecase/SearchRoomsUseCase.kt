package com.thehouse.app.domain.usecase

import com.thehouse.app.domain.room.RoomSearchFilters
import com.thehouse.app.domain.room.RoomSearchPage

/** Contrato de Domain para la búsqueda real de cuartos. Data lo implementa. */
interface RoomSearchRepository {
    suspend fun searchRooms(filters: RoomSearchFilters): Result<RoomSearchPage>
}

class SearchRoomsUseCase(
    private val repository: RoomSearchRepository,
) {
    suspend operator fun invoke(filters: RoomSearchFilters): Result<RoomSearchPage> =
        repository.searchRooms(filters)
}
