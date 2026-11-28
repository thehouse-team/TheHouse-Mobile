package com.thehouse.app.presentation.requests

import com.thehouse.app.domain.model.QuickQuestion

/**
 * Catálogo único de etiquetas visibles de las preguntas rápidas.
 *
 * Los códigos API (`available_now`, etc.) son los que viajan en el JSON; nunca se
 * muestran al usuario. Este catálogo evita que el composer y el detalle divergan.
 */
object QuickQuestionLabels {

    private val labels: Map<QuickQuestion, String> = mapOf(
        QuickQuestion.AVAILABLE_NOW to "¿Disponible ahora?",
        QuickQuestion.SERVICES_INCLUDED to "¿Qué servicios están incluidos?",
        QuickQuestion.DEPOSIT to "¿Cuánto es el depósito?",
        QuickQuestion.MINIMUM_CONTRACT to "¿Cuál es el contrato mínimo?",
        QuickQuestion.VISIT_POSSIBLE to "¿Se pueden hacer visitas?",
        QuickQuestion.PETS_ALLOWED to "¿Se aceptan mascotas?",
        QuickQuestion.ROOMMATES to "¿Hay roommates?",
    )

    fun label(question: QuickQuestion): String = labels[question] ?: question.api

    fun all(): List<Pair<QuickQuestion, String>> = labels.toList()
}
