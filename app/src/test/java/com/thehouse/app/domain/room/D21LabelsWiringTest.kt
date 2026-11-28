package com.thehouse.app.domain.room

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** D21 etiquetas: comprueba que las pantallas en alcance usan el catálogo. */
class D21LabelsWiringTest {

    private fun source(path: String) = java.io.File("src/main/java/com/thehouse/app/$path").readText()

    @Test
    fun `RoomCard traduce servicios visibles`() {
        val s = source("presentation/components/RoomCard.kt")
        assertTrue(s.contains("RoomSearchCatalog.label(RoomSearchCatalog.Kind.SERVICE, it)"))
    }

    @Test
    fun `PropertyCard traduce servicios visibles`() {
        val s = source("presentation/components/PropertyCard.kt")
        assertTrue(s.contains("RoomSearchCatalog.Kind.SERVICE"))
    }

    @Test
    fun `Explore traduce chips de servicios`() {
        val s = source("presentation/explore/ExploreScreen.kt")
        assertTrue(s.contains("RoomSearchCatalog.label(RoomSearchCatalog.Kind.SERVICE, code)"))
        assertFalse(s.contains("code.replaceFirstChar"))
    }

    @Test
    fun `SmartMatch traduce servicios y no usa readableCode`() {
        val s = source("presentation/smartmatch/SmartMatchRecommendationCard.kt")
        assertTrue(s.contains("RoomSearchCatalog.label(RoomSearchCatalog.Kind.SERVICE, it)"))
        assertFalse(s.contains("readableCode"))
    }

    @Test
    fun `RoomDetail traduce servicios mobiliario y contrato`() {
        val s = source("presentation/roomdetail/RoomDetailScreen.kt")
        assertTrue(s.contains("RoomSearchCatalog.Kind.SERVICE"))
        assertTrue(s.contains("RoomSearchCatalog.Kind.FURNITURE"))
        assertTrue(s.contains("RoomSearchCatalog.Kind.MIN_CONTRACT"))
    }

    @Test
    fun `Favoritos mantiene catalogo para tipo y bano`() {
        val s = source("presentation/favorites/FavoritesScreen.kt")
        assertTrue(s.contains("RoomSearchCatalog.Kind.ROOM_TYPE"))
        assertTrue(s.contains("RoomSearchCatalog.Kind.BATHROOM"))
    }
}
