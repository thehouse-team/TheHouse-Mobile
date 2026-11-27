package com.thehouse.app.domain.location

/**
 * Parser puro de coordenadas para el formulario "Nuevo edificio".
 *
 * El Backend valida (BuildingService.Location, verificado en solo lectura):
 * - latitud finita en 14.0..33.0 (inclusive)
 * - longitud finita en -118.5..-86.0 (inclusive)
 *
 * Durante la escritura se conserva el texto original: el parser solo decide
 * si el valor actual es válido y normaliza los caracteres que Android/IME
 * pueden insertar (coma decimal, signo menos Unicode, guion en dash,
 * espacios y caracteres invisibles de pegado). No se concatenan grupos
 * separados por espacios internos ambiguos y no se redondea ni se limita
 * la cantidad de decimales.
 */
object CoordinateParser {

    const val MIN_LAT = 14.0
    const val MAX_LAT = 33.0
    const val MIN_LNG = -118.5
    const val MAX_LNG = -86.0

    /** Caracteres invisibles que pueden aparecer al pegar desde hojas de cálculo o PDF. */
    private val INVISIBLE_CHARS = charArrayOf('\u200B', '\u200C', '\u200D', '\u2060', '\uFEFF')

    sealed interface Result {
        data class Valid(val value: Double) : Result
        data object Partial : Result
        data object Invalid : Result
    }

    /**
     * Normaliza y convierte una coordenada.
     * - Partial: campo vacío, solo signo, solo separador decimal o texto en edición.
     * - Invalid: formato ambiguo, no finito o fuera del rango contractual.
     */
    fun parse(raw: String): Result {
        var text = raw.trim()
        INVISIBLE_CHARS.forEach { text = text.replace(it.toString(), "") }
        text = text
            .replace('\u2212', '-')  // U+2212 MINUS SIGN
            .replace('\u2013', '-')  // U+2013 EN DASH cuando actúa como signo inicial
            .replace(',', '.')       // coma decimal de teclado localizado

        if (text.isEmpty()) return Result.Partial
        if (text == "-" || text == "+" || text == ".") return Result.Partial
        // El usuario acaba de escribir el separador decimal: entrada parcial.
        val signless = text.removePrefix("-").removePrefix("+")
        if (signless.endsWith('.')) return Result.Partial

        // Un único signo inicial permitido.
        val signed = if (text.first() == '-' || text.first() == '+') {
            val rest = text.substring(1)
            if (rest.isEmpty()) return Result.Partial
            if (rest.first() == '-' || rest.first() == '+' || rest.first() == '.') return Result.Invalid
            text
        } else {
            if (text.contains('-') || text.contains('+')) return Result.Invalid
            text
        }

        // Un único separador decimal, sin espacios internos ni agrupadores ambiguos.
        if (signed.any { it.isWhitespace() }) return Result.Invalid
        if (signed.count { it == '.' } > 1) return Result.Invalid
        val digits = signed.filter { it != '.' && it != '-' && it != '+' }
        if (digits.isEmpty() || !digits.all { it.isDigit() }) return Result.Invalid

        val value = signed.toDoubleOrNull() ?: return Result.Invalid
        if (!value.isFinite()) return Result.Invalid
        return Result.Valid(value)
    }

    fun isValidLatitude(raw: String): Boolean = parse(raw) is Result.Valid &&
        (parse(raw) as Result.Valid).value in MIN_LAT..MAX_LAT

    fun isValidLongitude(raw: String): Boolean = parse(raw) is Result.Valid &&
        (parse(raw) as Result.Valid).value in MIN_LNG..MAX_LNG

    fun latitudeOrNull(raw: String): Double? = parse(raw).let {
        if (it is Result.Valid && it.value in MIN_LAT..MAX_LAT) it.value else null
    }

    fun longitudeOrNull(raw: String): Double? = parse(raw).let {
        if (it is Result.Valid && it.value in MIN_LNG..MAX_LNG) it.value else null
    }
}
