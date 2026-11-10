package com.thehouse.app.presentation.explore

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.thehouse.app.domain.model.GeoPoint
import com.thehouse.app.domain.model.MapBounds
import com.thehouse.app.domain.model.RoomMarker
import com.thehouse.app.presentation.components.formatDistance
import kotlinx.coroutines.delay

/**
 * Visualización de mapa de referencia con marcadores REALES de /rooms/map.
 *
 * No usa SDK de mapas ni tiles externos: el proyecto no tiene API key y
 * M-MOB-017/COST-001 siguen pendientes. Las coordenadas son las aproximadas
 * del contrato (nunca la dirección real). Pan/zoom generan un nuevo bbox;
 * el ViewModel valida el máximo de 0.3° y aplica debounce.
 */
@Composable
fun RoomMapView(
    markers: List<RoomMarker>,
    bounds: MapBounds,
    onViewportChanged: (MapBounds) -> Unit,
    onMarkerClick: (RoomMarker) -> Unit,
    modifier: Modifier = Modifier,
) {
    var viewport by remember(bounds) { mutableStateOf(bounds) }
    var userMoved by remember(bounds) { mutableStateOf(false) }

    LaunchedEffect(viewport, userMoved) {
        if (!userMoved) return@LaunchedEffect
        delay(500)
        onViewportChanged(viewport)
        userMoved = false
    }

    val background = MaterialTheme.colorScheme.surfaceVariant
    val grid = MaterialTheme.colorScheme.outlineVariant
    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(background),
    ) {
        // La capa de gestos vive detrás de los marcadores para que un tap
        // sobre un marcador no sea consumido por pan/zoom.
        Box(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val current = viewport
                        val latSpan = (current.latitudeSpan / zoom).coerceIn(0.002, 0.6)
                        val lngSpan = (current.longitudeSpan / zoom).coerceIn(0.002, 0.6)
                        val latShift = pan.y / size.height * current.latitudeSpan
                        val lngShift = -pan.x / size.width * current.longitudeSpan
                        viewport = MapBounds.around(
                            center = GeoPoint(
                                latitude = (current.center.latitude + latShift)
                                    .coerceIn(-90.0, 90.0),
                                longitude = (current.center.longitude + lngShift)
                                    .coerceIn(-180.0, 180.0),
                            ),
                            latitudeSpan = latSpan,
                            longitudeSpan = lngSpan,
                        )
                        userMoved = true
                    }
                },
        )
        Canvas(Modifier.fillMaxSize()) {
            drawRect(color = background)
            val columns = 6
            val rows = 6
            repeat(columns - 1) { index ->
                val x = size.width * (index + 1) / columns
                drawLine(grid, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
            }
            repeat(rows - 1) { index ->
                val y = size.height * (index + 1) / rows
                drawLine(grid, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
            }
        }

        BoxWithConstraints(Modifier.fillMaxSize()) {
            val width = maxWidth
            val height = maxHeight
            markers.forEach { marker ->
                val x = ((marker.approxLocation.longitude - viewport.minLongitude) /
                    viewport.longitudeSpan).toFloat()
                val y = ((viewport.maxLatitude - marker.approxLocation.latitude) /
                    viewport.latitudeSpan).toFloat()
                if (x in 0f..1f && y in 0f..1f) {
                    Surface(
                        onClick = { onMarkerClick(marker) },
                        modifier = Modifier
                            .offset(
                                x = (width - 56.dp) * x.coerceIn(0.04f, 0.96f),
                                y = (height - 56.dp) * y.coerceIn(0.04f, 0.96f),
                            )
                            .semantics {
                                contentDescription =
                                    "Marcador a ${formatDistance(marker.distanceKm)} kilómetros"
                            },
                        shape = CircleShape,
                        color = primary,
                        shadowElevation = 3.dp,
                    ) {
                        Text(
                            text = "$${marker.price}",
                            color = onPrimary,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp),
        ) {
            Text(
                text = "Mapa de referencia",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Arrastra o pellizca · marcadores aproximados",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
