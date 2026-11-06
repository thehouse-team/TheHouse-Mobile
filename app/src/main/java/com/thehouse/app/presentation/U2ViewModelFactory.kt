package com.thehouse.app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.thehouse.app.di.AppContainer
import com.thehouse.app.presentation.auth.LoginViewModel
import com.thehouse.app.presentation.auth.RegisterViewModel
import com.thehouse.app.presentation.explore.ExploreViewModel
import com.thehouse.app.presentation.home.HomeViewModel
import com.thehouse.app.presentation.onboarding.OnboardingViewModel
import com.thehouse.app.presentation.root.RootViewModel

/**
 * Fábrica manual de ViewModels (DI, sin frameworks).
 *
 * U2 + D4 + D5 (catálogo real); de forma explícita.
 */
@Suppress("UNCHECKED_CAST")
class U2ViewModelFactory(
    private val container: AppContainer,
    private val userKey: String? = null,
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(RootViewModel::class.java) ->
            RootViewModel(container) as T

        modelClass.isAssignableFrom(LoginViewModel::class.java) ->
            LoginViewModel(container.login) as T

        modelClass.isAssignableFrom(RegisterViewModel::class.java) ->
            RegisterViewModel(container.register) as T

        modelClass.isAssignableFrom(OnboardingViewModel::class.java) ->
            OnboardingViewModel(container.onboardingRepository, userKey) as T

        modelClass.isAssignableFrom(HomeViewModel::class.java) ->
            HomeViewModel(
                logoutUseCase = container.logout,
                sessionState = container.sessionStore.state,
                getUniversities = container.getUniversities,
                getProperties = container.getProperties,
                selectedRepository = container.selectedUniversityRepository,
                selectUniversity = container.selectUniversity,
            ) as T

        modelClass.isAssignableFrom(ExploreViewModel::class.java) ->
            ExploreViewModel(
                getProperties = container.getProperties,
                searchProperties = container.searchProperties,
            ) as T

        else -> throw IllegalArgumentException("ViewModel no soportado: ${modelClass.canonicalName}")
    }
}
