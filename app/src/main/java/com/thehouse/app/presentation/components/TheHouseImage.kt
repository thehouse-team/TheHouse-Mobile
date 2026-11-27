package com.thehouse.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Roofing
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import com.thehouse.app.BuildConfig
import com.thehouse.app.data.image.ImageUrlResolver
import com.thehouse.app.perf.Perf

/**
 * D21 hotfix: imagen contractual con placeholder TheHouse.
 *
 * - Resuelve URLs relativas del Backend contra [BuildConfig.API_BASE_URL].
 * - Muestra siempre el placeholder branded cuando no hay URL, mientras carga
 *   y si la carga falla: nunca un contenedor blanco vacío.
 * - Expone el estado (`loading`/`success`/`error`) por semántica para tests y
 *   accesibilidad, y registra la duración solo al estado terminal.
 */
@Composable
fun TheHouseImage(
    url: String?,
    title: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    perfOperation: String = "image.load.generic",
) {
    val resolved = remember(url) { ImageUrlResolver.resolve(url, BuildConfig.API_BASE_URL) }
    var stateLabel by remember(resolved) { mutableStateOf(if (resolved == null) "empty" else "loading") }
    val start = remember(resolved) { System.nanoTime() }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (resolved != null) {
            AsyncImage(
                model = resolved,
                contentDescription = "Foto de $title",
                contentScale = contentScale,
                onState = { state ->
                    val next = when (state) {
                        is AsyncImagePainter.State.Success -> "success"
                        is AsyncImagePainter.State.Error -> "error"
                        is AsyncImagePainter.State.Loading -> "loading"
                        else -> "empty"
                    }
                    if (next != stateLabel) stateLabel = next
                    if (next == "success" || next == "error") {
                        Perf.since(perfOperation, start)
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
        // El placeholder permanece hasta Success y vuelve a aparecer en Error.
        if (stateLabel != "success") {
            TheHouseImagePlaceholder(
                title = title,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** Placeholder original TheHouse, reutilizado en todas las tarjetas y galería. */
@Composable
fun TheHouseImagePlaceholder(
    title: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.background(
            Brush.linearGradient(
                listOf(
                    MaterialTheme.colorScheme.primaryContainer,
                    MaterialTheme.colorScheme.secondaryContainer,
                ),
            ),
        ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.Roofing,
            contentDescription = "Sin foto disponible para $title",
            tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
            modifier = Modifier.semantics { contentDescription = "image.placeholder" },
        )
    }
}

/** Unidad de altura compartida para que ninguna tarjeta reserve espacio sin contenido. */
internal val ImageHeight = 168.dp
