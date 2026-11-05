package com.thehouse.app.domain.usecase

import com.thehouse.app.domain.model.SessionState
import com.thehouse.app.domain.repository.AuthRepository
import kotlinx.coroutines.flow.StateFlow

/**
 * Caso de uso de observación de sesión.
 *
 * Estado inicial y posterior a logout describen la sesión observable sin
 * exponer tokens. La renovación automática es diseño documentado en NV-04
 * y permanece BLOCKED-BACKEND mientras `/auth/refresh` no esté confirmado;
 * ante un token vencido la sesión se limpia y la UI regresa a Login.
 */
class ObserveSessionUseCase(private val repository: AuthRepository) {

    val state: StateFlow<SessionState> = repository.session
}
