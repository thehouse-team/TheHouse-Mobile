package com.thehouse.app.domain.error

/**
 * Excepción de portabilidad de error de dominio.
 *
 * Permite portar [DomainError] a través de `Result.failure` circunscrito al
 * dominio, sin exponer excepciones o tipos de infraestructura a Presentation.
 */
class DomainException(val error: DomainError) : Exception()
