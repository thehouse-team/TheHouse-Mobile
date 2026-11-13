package com.thehouse.app.presentation.navigation

import com.thehouse.app.domain.model.ReportTargetType

/** Ruta D13: reporte de contenido con target real del contrato. */
data class ReportRoute(val targetType: ReportTargetType, val targetId: String) : AppRoute {
    override val route: String = "reporte/${targetType.api}/" +
        java.net.URLEncoder.encode(targetId, "UTF-8")

    companion object {
        const val TYPE_ARGUMENT = "targetType"
        const val ID_ARGUMENT = "targetId"
        const val PATTERN = "reporte/{$TYPE_ARGUMENT}/{$ID_ARGUMENT}"
    }
}
