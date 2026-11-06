package com.thehouse.app.presentation.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thehouse.app.R
import com.thehouse.app.presentation.U2ViewModelFactory
import kotlinx.coroutines.launch

/** Texto del prototipo 00-onboarding (título, cuerpo; sin rediseñar). */
private data class OnboardingPage(val title: String, val body: String)

private val onboardingPages = listOf(
    OnboardingPage(
        title = "Busca tu universidad",
        body = "Ve las rentas de alrededor, cuánto cuestan y qué tan cerca están.",
    ),
    OnboardingPage(
        title = "Encuentra la que encaja contigo",
        body = "Te mostramos primero las que van contigo. Chatea y agenda visitas sin dar tu número.",
    ),
    OnboardingPage(
        title = "¿Rentas cuartos? Publícalos aquí",
        body = "Con la misma cuenta publicas tus cuartos y recibes solicitudes.",
    ),
)

/**
 * Onboarding del prototipo (D4, 00-onboarding.html).
 *
 * - Cabecera "TheHouse" + "Omitir", 3 diapositivas con los títulos/cuerpos
 *   del prototipo, dots, botón Siguiente/Comenzar e "Inicia sesión".
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

    Scaffold(modifier = modifier) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "TheHouse",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.semantics { contentDescription = "Marca TheHouse" },
                )
                TextButton(
                    onClick = { viewModel.onEvent(OnboardingEvent.SkipClicked) },
                    modifier = Modifier.height(48.dp),
                ) {
                    Text(stringResource(R.string.onboarding_skip))
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
                        .padding(vertical = 16.dp)
                        .semantics { contentDescription = "Diapositiva ${index + 1} de ${state.totalPages}" },
                    verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                ) {
                    Text(
                        text = page.title,
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(
                        text = page.body,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                ) {
                    if (state.page > 0) {
                        TextButton(
                            onClick = {
                                scope.launch { pagerState.animateScrollToPage(state.page - 1) }
                                viewModel.onEvent(OnboardingEvent.PageChanged(state.page - 1))
                            },
                            modifier = Modifier.height(48.dp),
                        ) {
                            Text(stringResource(R.string.onboarding_back))
                        }
                    }
                    Button(
                        onClick = {
                            val next = state.page + 1
                            if (next >= state.totalPages) {
                                viewModel.onEvent(OnboardingEvent.SkipClicked)
                            } else {
                                scope.launch { pagerState.animateScrollToPage(next) }
                                viewModel.onEvent(OnboardingEvent.PageChanged(next))
                            }
                        },
                        modifier = Modifier.height(48.dp),
                    ) {
                        Text(
                            if (state.page == state.totalPages - 1) {
                                stringResource(R.string.onboarding_start)
                            } else {
                                stringResource(R.string.onboarding_next)
                            },
                        )
                    }
                }
                TextButton(
                    onClick = onLoginClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                ) {
                    Text("¿Ya tienes cuenta? Inicia sesión")
                }
            }
        }
    }
}
