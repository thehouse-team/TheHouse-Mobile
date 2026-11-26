package com.thehouse.app.domain.error

/**
 * Perfil incompleto según el contrato NV-23 v1.2.0 del Backend.
 *
 * `missing` son los campos que el Backend reporta en el Problem Details 422
 * (`university`, `budget.max`, `livingWith`, `placeType`). Se transporta como
 * excepción de dominio para no filtrar Retrofit ni el cuerpo HTTP a Presentation.
 */
class SmartMatchProfileIncompleteException(
    val missing: List<String> = emptyList(),
) : Exception()
