package com.thehouse.app.domain.common

import kotlinx.coroutines.CoroutineDispatcher

/**
 * Contrato de dispatchers para evitar que Domain y Presentation dependan de
 * decisiones concretas de infraestructura.
 */
interface DispatcherProvider {
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
}
