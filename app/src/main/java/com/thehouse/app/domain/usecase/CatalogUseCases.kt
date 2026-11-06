package com.thehouse.app.domain.usecase

import com.thehouse.app.domain.model.PagedResult
import com.thehouse.app.domain.model.Property
import com.thehouse.app.domain.model.University
import com.thehouse.app.domain.repository.PropertyRepository
import com.thehouse.app.domain.repository.SelectedUniversityRepository
import com.thehouse.app.domain.repository.UniversityRepository
import kotlinx.coroutines.flow.StateFlow

class GetUniversitiesUseCase(private val repository: UniversityRepository) {
    suspend operator fun invoke(
        page: Int = 1,
        pageSize: Int = 20,
    ): Result<PagedResult<University>> = repository.getUniversities(page, pageSize)
}

class GetPropertiesUseCase(private val repository: PropertyRepository) {
    suspend operator fun invoke(
        page: Int = 1,
        pageSize: Int = 20,
    ): Result<PagedResult<Property>> = repository.getProperties(page, pageSize)
}

/** Búsqueda local sobre campos reales (título/ciudad), sin backend query. */
class SearchPropertiesUseCase {
    operator fun invoke(
        properties: List<Property>,
        query: String,
    ): List<Property> {
        val normalized = query.trim().lowercase()
        if (normalized.isBlank()) return properties
        return properties.filter { property ->
            property.title.lowercase().contains(normalized) ||
                property.city.lowercase().contains(normalized)
        }
    }
}

class ObserveSelectedUniversityUseCase(
    private val repository: SelectedUniversityRepository,
) {
    val selectedId: StateFlow<String?> = repository.selectedUniversityId
}

class SelectUniversityUseCase(
    private val repository: SelectedUniversityRepository,
) {
    suspend operator fun invoke(universityId: String) {
        repository.select(universityId)
    }
}
