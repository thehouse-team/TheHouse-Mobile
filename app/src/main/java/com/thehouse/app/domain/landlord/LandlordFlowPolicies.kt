package com.thehouse.app.domain.landlord

import java.time.LocalDate
import java.time.ZoneId

/** Políticas puras del flujo arrendador, derivadas del contrato real del Backend. */
object LandlordFlowPolicies {
    /** Rangos contractuales inclusivos (`BuildingService.Location`, NV-12). */
    const val LATITUDE_MIN = 14.0
    const val LATITUDE_MAX = 33.0
    const val LONGITUDE_MIN = -118.5
    const val LONGITUDE_MAX = -86.0
    const val COORDINATE_HINT = "Latitud: 14 a 33. Longitud: -118.5 a -86 (México)."

    /** `LandlordStatsRules.Today` usa la zona de Ciudad de México. */
    val MEXICO_CITY_ZONE: ZoneId = ZoneId.of("America/Mexico_City")
    const val STATS_DEFAULT_DAYS = 30
    const val STATS_MAX_DAYS = 90

    /** Distancia a partir de la cual se muestra advertencia (no bloqueo) para alojamiento estudiantil. */
    const val EXCESSIVE_DISTANCE_KM = 25.0

    fun statsRange(todayInMexico: LocalDate = LocalDate.now(MEXICO_CITY_ZONE)): Pair<String, String> {
        val from = todayInMexico.minusDays((STATS_DEFAULT_DAYS - 1).toLong())
        return from.toString() to todayInMexico.toString()
    }

    fun excessiveDistance(distanceKm: Double?): Boolean =
        distanceKm != null && distanceKm.isFinite() && distanceKm > EXCESSIVE_DISTANCE_KM

    fun distanceWarning(distanceKm: Double?): String? = if (excessiveDistance(distanceKm)) {
        "Esta distancia es muy alta para un alojamiento estudiantil. " +
            "Revisa la universidad seleccionada y las coordenadas del inmueble."
    } else {
        null
    }
}
