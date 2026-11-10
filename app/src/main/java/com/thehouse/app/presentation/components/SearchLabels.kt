package com.thehouse.app.presentation.components

/** Etiquetas en español para los valores del contrato NV-14. */
object SearchLabels {

    fun service(value: String): String = when (value) {
        "wifi" -> "WiFi"
        "water" -> "Agua"
        "electricity" -> "Luz"
        "gas" -> "Gas"
        "hot_water" -> "Agua caliente"
        "laundry" -> "Lavandería"
        "kitchen" -> "Cocina"
        "furnished" -> "Amueblado"
        "parking" -> "Estacionamiento"
        "security" -> "Seguridad"
        "cleaning" -> "Limpieza"
        else -> value
    }

    fun roomType(value: String?): String? = when (value) {
        "single" -> "Cuarto individual"
        "double" -> "Cuarto doble"
        "shared" -> "Cuarto compartido"
        null, "" -> null
        else -> value
    }

    fun bathroom(value: String?): String? = when (value) {
        "private" -> "Baño privado"
        "shared" -> "Baño compartido"
        null, "" -> null
        else -> value
    }

    fun buildingType(value: String): String = when (value) {
        "rooms_building" -> "Edificio de cuartos"
        "house_rooms" -> "Casa con cuartos"
        "full_apartment" -> "Departamento completo"
        else -> value
    }

    fun allowedTenants(value: String): String = when (value) {
        "women" -> "Mujeres"
        "men" -> "Hombres"
        "any" -> "Cualquier persona"
        else -> value
    }

    fun furniture(value: String): String = when (value) {
        "bed" -> "Cama"
        "desk" -> "Escritorio"
        "chair" -> "Silla"
        "closet" -> "Clóset"
        "dresser" -> "Cómoda"
        "tv" -> "Televisión"
        "fridge" -> "Refrigerador"
        "microwave" -> "Microondas"
        else -> value
    }

    fun minContract(value: String?): String? = when (value) {
        "four_months" -> "4 meses"
        "six_months" -> "6 meses"
        "annual" -> "12 meses"
        null, "" -> null
        else -> value
    }

    fun placeType(value: String): String = when (value) {
        "neighborhood" -> "Colonia"
        "city" -> "Ciudad"
        else -> value
    }
}
