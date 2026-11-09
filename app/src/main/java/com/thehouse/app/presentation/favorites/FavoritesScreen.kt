package com.thehouse.app.presentation.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.thehouse.app.domain.model.Favorite
import com.thehouse.app.presentation.common.UiError
import com.thehouse.app.presentation.common.UiErrorMessages

@Composable
fun FavoritesScreen(
    isAuthenticated: Boolean,
    isLoading: Boolean,
    items: List<Favorite>,
    error: UiError?,
    removingId: String?,
    onLoginClick: () -> Unit,
    onExploreClick: () -> Unit,
    onRemove: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Favoritos",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { contentDescription = "Título Favoritos" },
            )
            if (!isAuthenticated) {
                Text(
                    text = "Necesitas iniciar sesión para ver tus favoritos.",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Button(
                    onClick = onLoginClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                ) {
                    Text("Iniciar sesión")
                }
            } else if (isLoading) {
                CircularProgressIndicator()
            } else if (error != null) {
                Text(text = error?.let { UiErrorMessages.map(it) } ?: "", style = MaterialTheme.typography.bodyLarge)
                Button(onClick = onRetry, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                    Text("Reintentar")
                }
            } else if (items.isEmpty()) {
                Text(text = "Aún no tienes favoritos guardados.", style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = "Explora propiedades y guarda las que te interesen.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = onExploreClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                ) {
                    Text("Explorar propiedades")
                }
            } else {
                items.forEach { fav ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = fav.title, style = MaterialTheme.typography.titleMedium)
                            Text(text = fav.city ?: "", style = MaterialTheme.typography.bodySmall)
                        }
                        Button(
                            onClick = { onRemove(fav.propertyId) },
                            enabled = removingId != fav.propertyId,
                        ) {
                            Text(if (removingId == fav.propertyId) "..." else "Quitar")
                        }
                    }
                }
            }
        }
    }
}