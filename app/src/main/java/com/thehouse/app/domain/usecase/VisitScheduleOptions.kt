package com.thehouse.app.domain.usecase

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Opciones válidas para solicitar una visita, derivadas del contrato real del
 * Backend (`VisitSchedulePolicy`): America/Mexico_City, 08:00–20:00, pasos de
 * 15 minutos, anticipación mínima de 2 h y ventana máxima de 60 días.
 *
 * Son horarios PERMITIDOS PARA SOLICITAR, no disponibilidad confirmada del
 * arrendador: el Backend conserva la autoridad final (409 `schedule_overlap`).
 */
object VisitScheduleOptions {
    val SPANISH_DATE: DateTimeFormatter =
        DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-MX"))

    /** Fecha más temprana que puede solicitarse (en zona CDMX). */
    fun earliestDate(now: Instant = Instant.now()): LocalDate =
        now.atZone(VisitValidator.TIME_ZONE).toLocalDate()

    /** Última fecha que puede caer dentro de la ventana de 60 días. */
    fun latestDate(now: Instant = Instant.now()): LocalDate =
        now.atZone(VisitValidator.TIME_ZONE).plusDays(60).toLocalDate()

    /** true cuando el día está dentro de la ventana contractual. */
    fun isDateAllowed(date: LocalDate, now: Instant = Instant.now()): Boolean =
        !date.isBefore(earliestDate(now)) && !date.isAfter(latestDate(now))

    /** Horarios de 15 minutos entre 08:00 y 20:00 (ambos inclusivos). */
    fun timeSlots(): List<LocalTime> {
        val slots = mutableListOf<LocalTime>()
        var time = VisitValidator.OPENING_TIME
        while (!time.isAfter(VisitValidator.CLOSING_TIME)) {
            slots += time
            time = time.plusMinutes(15)
        }
        return slots
    }

    /** Formatea la fecha elegida en español legible, p. ej. «30 de noviembre de 2026». */
    fun formatDate(date: LocalDate): String = SPANISH_DATE.format(date)

    /**
     * horarios permitidos para una fecha concreta. Descarta los que incumplen
     * la anticipación mínima (hoy) o la ventana máxima (último día).
     */
    fun allowedTimesFor(date: LocalDate, now: Instant = Instant.now()): List<LocalTime> {
        if (!isDateAllowed(date, now)) return emptyList()
        val earliest = now.plus(Duration.ofHours(2))
        val latest = now.plus(Duration.ofDays(60))
        return timeSlots().filter { time ->
            val instant = ZonedDateTime.of(date, time, VisitValidator.TIME_ZONE).toInstant()
            !instant.isBefore(earliest) && !instant.isAfter(latest)
        }
    }

    /** Revalida una hora al cambiar de fecha; null cuando ya no es permitida. */
    fun revalidateTime(date: LocalDate?, time: LocalTime?, now: Instant = Instant.now()): LocalTime? =
        if (date == null || time == null) null
        else allowedTimesFor(date, now).firstOrNull { it == time }

    /**
     * Convierte `selectedDateMillis` de Material3 DatePicker (fecha en UTC)
     * a LocalDate sin desplazar el día al aplicar America/Mexico_City.
     * DatePicker-state almacena el inicio del día en UTC; usar la zona local
     * del dispositivo desplazaría el día en zonas con offset negativo.
     */
    fun dateFromPickerMillis(selectedDateMillis: Long): LocalDate =
        java.time.Instant.ofEpochMilli(selectedDateMillis)
            .atZone(java.time.ZoneOffset.UTC)
            .toLocalDate()

    /** millis UTC correspondientes a una fecha para inicializar el DatePicker. */
    fun pickerMillisFromDate(date: LocalDate): Long =
        date.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
}
