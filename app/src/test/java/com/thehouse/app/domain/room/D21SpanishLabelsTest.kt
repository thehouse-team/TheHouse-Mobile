package com.thehouse.app.domain.room

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * D21 etiquetas: catálogos de presentación del Backend.
 * Solo textos visibles; los códigos serializados no se modifican.
 */
class D21SpanishLabelsTest {

    @Test
    fun `tipos de cuarto y bano en espanol`() {
        assertEquals("Individual", RoomSearchCatalog.label(RoomSearchCatalog.Kind.ROOM_TYPE, "single"))
        assertEquals("Doble", RoomSearchCatalog.label(RoomSearchCatalog.Kind.ROOM_TYPE, "double"))
        assertEquals("Compartido o Compartida", RoomSearchCatalog.label(RoomSearchCatalog.Kind.ROOM_TYPE, "shared"))
        assertEquals("Privado", RoomSearchCatalog.label(RoomSearchCatalog.Kind.BATHROOM, "private"))
        assertEquals("Compartido o Compartida", RoomSearchCatalog.label(RoomSearchCatalog.Kind.BATHROOM, "shared"))
    }

    @Test
    fun `tipos de edificio e inquilinos en espanol`() {
        assertEquals("Casa con habitaciones", RoomSearchCatalog.label(RoomSearchCatalog.Kind.BUILDING_TYPE, "house_rooms"))
        assertEquals("Cuartos en edificio", RoomSearchCatalog.label(RoomSearchCatalog.Kind.BUILDING_TYPE, "rooms_building"))
        assertEquals("Departamento completo", RoomSearchCatalog.label(RoomSearchCatalog.Kind.BUILDING_TYPE, "full_apartment"))
        assertEquals("Solo mujeres", RoomSearchCatalog.label(RoomSearchCatalog.Kind.ALLOWED_TENANTS, "women"))
        assertEquals("Solo hombres", RoomSearchCatalog.label(RoomSearchCatalog.Kind.ALLOWED_TENANTS, "men"))
        assertEquals("Cualquiera", RoomSearchCatalog.label(RoomSearchCatalog.Kind.ALLOWED_TENANTS, "any"))
    }

    @Test
    fun `los once servicios del backend se traducen`() {
        val esperados = mapOf(
            "wifi" to "Wi-Fi",
            "water" to "Agua",
            "electricity" to "Electricidad",
            "gas" to "Gas",
            "hot_water" to "Agua caliente",
            "laundry" to "Lavandería",
            "kitchen" to "Cocina",
            "furnished" to "Amueblado",
            "parking" to "Estacionamiento",
            "security" to "Seguridad",
            "cleaning" to "Limpieza",
        )
        esperados.forEach { (code, label) ->
            assertEquals(code, label, RoomSearchCatalog.label(RoomSearchCatalog.Kind.SERVICE, code))
        }
    }

    @Test
    fun `mobiliario y contrato minimo del backend se traducen`() {
        val muebles = mapOf(
            "bed" to "Cama", "desk" to "Escritorio", "chair" to "Silla",
            "closet" to "Clóset", "dresser" to "Cómoda", "tv" to "TV",
            "fridge" to "Refrigerador", "microwave" to "Microondas",
        )
        muebles.forEach { (code, label) ->
            assertEquals(code, label, RoomSearchCatalog.label(RoomSearchCatalog.Kind.FURNITURE, code))
        }
        assertEquals("Cuatro meses", RoomSearchCatalog.label(RoomSearchCatalog.Kind.MIN_CONTRACT, "four_months"))
        assertEquals("Seis meses", RoomSearchCatalog.label(RoomSearchCatalog.Kind.MIN_CONTRACT, "six_months"))
        assertEquals("Anual", RoomSearchCatalog.label(RoomSearchCatalog.Kind.MIN_CONTRACT, "annual"))
    }

    @Test
    fun `valor desconocido conserva el codigo original sin inventar traduccion`() {
        assertEquals("nuevo_servicio", RoomSearchCatalog.label(RoomSearchCatalog.Kind.SERVICE, "nuevo_servicio"))
        assertEquals("futuro", RoomSearchCatalog.label(RoomSearchCatalog.Kind.ROOM_TYPE, "futuro"))
        assertEquals("otro", RoomSearchCatalog.label(RoomSearchCatalog.Kind.FURNITURE, "otro"))
        assertEquals("mensual", RoomSearchCatalog.label(RoomSearchCatalog.Kind.MIN_CONTRACT, "mensual"))
    }

    @Test
    fun `las listas contractuales siguen siendo los codigos del backend`() {
        assertEquals(listOf("single", "double", "shared"), RoomSearchCatalog.ROOM_TYPES)
        assertEquals(listOf("rooms_building", "house_rooms", "full_apartment"), RoomSearchCatalog.BUILDING_TYPES)
        assertEquals(listOf("private", "shared"), RoomSearchCatalog.BATHROOMS)
        assertEquals(listOf("women", "men", "any"), RoomSearchCatalog.ALLOWED_TENANTS)
    }
}
