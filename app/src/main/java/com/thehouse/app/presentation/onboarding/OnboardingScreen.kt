package com.thehouse.app.presentation.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddHome
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thehouse.app.R
import com.thehouse.app.presentation.U2ViewModelFactory
import com.thehouse.app.presentation.components.BrandMark
import com.thehouse.app.presentation.components.TheHousePrimaryButton
import com.thehouse.app.ui.theme.Heights
import com.thehouse.app.ui.theme.IconSizes
import com.thehouse.app.ui.theme.Spacing
import kotlinx.coroutines.launch

/** Texto e icono del prototipo 00-onboarding (sin inventar contenido). */
private data class OnboardingPage(
    val title: String,
    val body: String,
    val icon: ImageVector,
)

private val onboardingPages = listOf(
    OnboardingPage(
        title = "Busca tu universidad",
        body = "Ve las rentas de alrededor, cuánto cuestan y qué tan cerca están.",
        icon = Icons.Rounded.Search,
    ),
    OnboardingPage(
        title = "Encuentra la que encaja contigo",
        body = "Te mostramos primero las que van contigo. Chatea y agenda visitas sin dar tu número.",
        icon = Icons.Rounded.Home,
    ),
    OnboardingPage(
        title = "¿Rentas cuartos? Publícalos aquí",
        body = "Con la misma cuenta publicas tus cuartos y recibes solicitudes.",
        icon = Icons.Rounded.AddHome,
    ),
)

/**
 * Onboarding del prototipo (D4, 00-onboarding.html).
 *
 * - Cabecera de marca + "Omitir", tres diapositivas ilustradas, dots y
 *   botón Siguiente/Comenzar e "Inicia sesión".
 * - Flujo D4: Omitir → Guest → Home; Terminar → Guest/Home; Inicia sesión → Login.
 *   (El llamador decide la ruta; el ViewModel solo marca completado y emite efecto.)
 */
@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier,
    factory: U2ViewModelFactory,
) {
    val viewModel: OnboardingViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsState()
    val pagerState = rememberPagerState(initialPage = state.page, pageCount = { state.totalPages })
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                OnboardingEffect.NavigateToWelcome -> onFinished()
            }
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(Spacing.xl),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.sm),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BrandMark()
                TextButton(
                    onClick = { viewModel.onEvent(OnboardingEvent.SkipClicked) },
                    modifier = Modifier.height(Heights.button),
                ) {
                    Text(
                        stringResource(R.string.onboarding_skip),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) { index ->
                val page = onboardingPages.getOrElse(index) { onboardingPages.first() }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = Spacing.sm)
                        .semantics { contentDescription = "Diapositiva ${index + 1} de ${state.totalPages}" },
                    verticalArrangement = Arrangement.spacedBy(Spacing.xxl, Alignment.CenterVertically),
                ) {
                    OnboardingHero(page = page)
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                        Text(
                            text = page.title,
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            text = page.body,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.lg),
            ) {
                // Indicador de progreso del prototipo: punto activo alargado.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Página ${state.page + 1} de ${state.totalPages}" },
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm, Alignment.CenterHorizontally),
                ) {
                    repeat(state.totalPages) { index ->
                        val selected = index == state.page
                        Box(
                            modifier = Modifier
                                .then(
                                    if (selected) {
                                        Modifier
                                            .width(28.dp)
                                            .height(8.dp)
                                    } else {
                                        Modifier
                                            .width(8.dp)
                                            .height(8.dp)
                                    },
                                )
                                .clip(CircleShape)
                                .background(
                                    if (selected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outlineVariant
                                    },
                                ),
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    if (state.page > 0) {
                        TextButton(
                            onClick = {
                                scope.launch { pagerState.animateScrollToPage(state.page - 1) }
                                viewModel.onEvent(OnboardingEvent.PageChanged(state.page - 1))
                            },
                            modifier = Modifier.height(Heights.buttonLarge),
                        ) {
                            Text(stringResource(R.string.onboarding_back))
                        }
                    }
                    TheHousePrimaryButton(
                        text = if (state.page == state.totalPages - 1) {
                            stringResource(R.string.onboarding_start)
                        } else {
                            stringResource(R.string.onboarding_next)
                        },
                        onClick = {
                            val next = state.page + 1
                            if (next >= state.totalPages) {
                                viewModel.onEvent(OnboardingEvent.SkipClicked)
                            } else {
                                scope.launch { pagerState.animateScrollToPage(next) }
                                viewModel.onEvent(OnboardingEvent.PageChanged(next))
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
                TextButton(
                    onClick = onLoginClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Heights.button),
                ) {
                    Text(
                        text = "¿Ya tienes cuenta? Inicia sesión",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingHero(page: OnboardingPage) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.secondaryContainer,
                    ),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        // Formas decorativas sobrias, sin assets externos.
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(Spacing.lg)
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.08f)),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(Spacing.xxl)
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.10f)),
        )
        Icon(
            imageVector = page.icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(72.dp),
        )
    }
}
