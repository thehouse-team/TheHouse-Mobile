package com.thehouse.app.presentation.demo

/**
 * Datos de demostración aislados del prototipo (D4, NV-05).
 *
 * Fuente: `00-onboarding.html` + `07-inicio-estudiante.html` y landing
 * (El Llano $3,200 / 8 min / 95%; Laureles $2,800 / 12 min / 91%).
 *
 * - NO son respuesta del Backend (sin endpoint, sin Retrofit).
 * - El backend confirmado no entrega properties (NV-27 sigue BLOCKED parcial
 *   para properties/universities); por eso se aísla aquí como fixture.
 * - Widgets que los muestran etiquetan visualmente "Demo del prototipo".
 */
data class PrototypeProperty(
    val title: String,
    val priceMxn: Int,
    val distanceMinutes: Int,
    val distanceMeters: Int? = null,
    val matchPercent: Int,
    val verified: Boolean,
    val services: List<String> = emptyList(),
    val isDemo: Boolean = true,
)

object PrototypeProperties {

    val featured: List<PrototypeProperty> = listOf(
        PrototypeProperty(
            title = "Estudio Universitario El Llano",
            priceMxn = 3200,
            distanceMinutes = 8,
            distanceMeters = 650,
            matchPercent = 95,
            verified = true,
            services = listOf("WiFi 100MB", "Amueblado", "Agua/Luz incl."),
        ),
        PrototypeProperty(
            title = "Residencia Universitaria Los Laureles",
            priceMxn = 2800,
            distanceMinutes = 12,
            distanceMeters = null,
            matchPercent = 91,
            verified = true,
            services = listOf("Habitación privada", "Baño propio", "Estudio tranquilo"),
        ),
    )

    val featuredProperty: PrototypeProperty = featured.first()
}
