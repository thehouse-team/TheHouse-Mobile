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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thehouse.app.R
import com.thehouse.app.presentation.U2ViewModelFactory
import kotlinx.coroutines.launch

/** Textos de las 3 pantallas de introducción (primer ingreso). */
private val onboardingPages = listOf(
    "Encuentra casas y habitaciones cerca de tu universidad.",
    "Filtra por universidad, precio y distancia al momento de buscar.",
    "Visita, haz match y firma tu próximo hogar en un solo lugar.",
)

/**
 * Onboarding de primer ingreso (3 pantallas + botón `Omitir`), MVVM + UDF.
 * Referencia visual: `00-onboarding.html` (fidelidad pendiente humana).
 */
@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
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
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(
                    onClick = { viewModel.onEvent(OnboardingEvent.SkipClicked) },
                    modifier = Modifier
                        .height(44.dp),
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Paso ${index + 1} de ${state.totalPages}",
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Text(
                        text = onboardingPages.getOrElse(index) { "" },
                        style = MaterialTheme.typography.headlineSmall,
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
            ) {
                if (state.page > 0) {
                    TextButton(
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(state.page - 1) }
                            viewModel.onEvent(OnboardingEvent.PageChanged(state.page - 1))
                        },
                        modifier = Modifier
                            .height(44.dp),
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
                    modifier = Modifier
                        .height(44.dp),
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
        }
    }
}
