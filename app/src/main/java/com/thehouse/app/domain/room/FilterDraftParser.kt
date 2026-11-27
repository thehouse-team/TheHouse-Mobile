package com.thehouse.app.domain.room

/**
 * Parseo de borradores de filtros: el texto se conserva tal cual durante la
 * edición (permite campo vacío, dígitos parciales y borrado) y solo se convierte
 * al tipo contractual cuando el usuario pulsa «Aplicar filtros».
 *
 * Nunca sustituye una entrada parcial por cero ni lanza consultas durante la
 * edición: esta función es pura y no tiene efectos de red.
 */
object FilterDraftParser {

    data class PriceResult(val value: Int?, val error: String?) {
        val isValid: Boolean get() = error == null
    }

    fun parsePrice(raw: String, field: String): PriceResult {
        val text = raw.trim()
        if (text.isEmpty()) return PriceResult(null, null)
        val number = text.toIntOrNull()
            ?: return PriceResult(null, "$field: usa solo dígitos.")
        if (number < RoomSearchCatalog.MIN_PRICE || number > RoomSearchCatalog.MAX_PRICE) {
            return PriceResult(
                null,
                "$field: usa un valor entre ${RoomSearchCatalog.MIN_PRICE} y ${RoomSearchCatalog.MAX_PRICE} MXN.",
            )
        }
        return PriceResult(number, null)
    }

    data class DistanceResult(val value: Double?, val error: String?) {
        val isValid: Boolean get() = error == null
    }

    fun parseDistance(raw: String): DistanceResult {
        val text = raw.trim().replace(',', '.')
        if (text.isEmpty()) return DistanceResult(RoomSearchCatalog.DEFAULT_DISTANCE_KM, null)
        val number = text.toDoubleOrNull()
            ?: return DistanceResult(null, "Distancia: usa un número válido.")
        if (number < RoomSearchCatalog.MIN_DISTANCE_KM || number > RoomSearchCatalog.MAX_DISTANCE_KM) {
            return DistanceResult(
                null,
                "Distancia: usa un valor entre ${RoomSearchCatalog.MIN_DISTANCE_KM} y ${RoomSearchCatalog.MAX_DISTANCE_KM} km.",
            )
        }
        return DistanceResult(number, null)
    }

    /** Validación cruzada de límites de precio tras convertir ambos campos. */
    fun priceRangeError(min: Int?, max: Int?): String? = when {
        min != null && max != null && min > max ->
            "El precio mínimo no puede ser mayor que el máximo."
        else -> null
    }
}
