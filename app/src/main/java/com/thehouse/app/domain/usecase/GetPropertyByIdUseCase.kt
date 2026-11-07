package com.thehouse.app.domain.usecase

import com.thehouse.app.domain.error.DomainError
import com.thehouse.app.domain.error.DomainException
import com.thehouse.app.domain.model.Property
import com.thehouse.app.domain.repository.PropertyRepository

/** D6: reutiliza el repositorio/contrato D5, sin un segundo cliente de red. */
class GetPropertyByIdUseCase(private val repository: PropertyRepository) {
    suspend operator fun invoke(id: String): Result<Property> =
        if (id.isBlank()) {
            Result.failure(DomainException(DomainError.NotFound))
        } else {
            repository.getPropertyById(id)
        }
}
