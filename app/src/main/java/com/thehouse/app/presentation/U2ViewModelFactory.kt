package com.thehouse.app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.thehouse.app.di.AppContainer
import com.thehouse.app.domain.usecase.GetPropertyByIdUseCase
import com.thehouse.app.presentation.auth.LoginViewModel
import com.thehouse.app.presentation.auth.RegisterViewModel
import com.thehouse.app.presentation.explore.ExploreViewModel
import com.thehouse.app.presentation.favorites.FavoritesViewModel
import com.thehouse.app.presentation.home.HomeViewModel
import com.thehouse.app.presentation.onboarding.OnboardingViewModel
import com.thehouse.app.presentation.root.RootViewModel
import com.thehouse.app.presentation.propertydetail.PropertyDetailViewModel
import com.thehouse.app.presentation.roomdetail.RoomDetailViewModel
import com.thehouse.app.presentation.visits.ScheduleVisitViewModel
import com.thehouse.app.presentation.visits.VisitDetailViewModel
import com.thehouse.app.presentation.visits.VisitsViewModel

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

    fun propertyDetail(propertyId: String): ViewModelProvider.Factory = viewModelFactory {
        initializer {
            PropertyDetailViewModel(
                propertyId = propertyId,
                getPropertyById = GetPropertyByIdUseCase(container.propertyRepository),
                sessionState = container.sessionStore.state,
            )
        }
    }

    fun scheduleVisit(propertyId: String): ViewModelProvider.Factory = viewModelFactory {
        initializer {
            ScheduleVisitViewModel(
                propertyId = propertyId,
                createVisit = container.createVisit,
            )
        }
    }

    fun visitDetail(visitId: String): ViewModelProvider.Factory = viewModelFactory {
        initializer {
            VisitDetailViewModel(
                visitId = visitId,
                getVisit = container.getVisit,
                cancelVisit = container.cancelVisit,
            )
        }
    }

    fun roomDetail(roomId: String): ViewModelProvider.Factory = viewModelFactory {
        initializer {
            RoomDetailViewModel(
                roomId = roomId,
                getRoomById = container.getRoomById,
            )
        }
    }

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
                getUniversities = container.getUniversities,
                searchRooms = container.searchRooms,
                searchPlacesUseCase = container.searchPlaces,
                getRoomMap = container.getRoomMap,
                selectedRepository = container.selectedUniversityRepository,
                selectUniversity = container.selectUniversity,
                sessionState = container.sessionStore.state,
            ) as T

        modelClass.isAssignableFrom(FavoritesViewModel::class.java) ->
            FavoritesViewModel(
                getFavorites = container.getFavorites,
                removeFavorite = container.removeFavorite,
                favoriteIdsStore = container.favoriteIdsStore,
                sessionState = container.sessionStore.state,
            ) as T

        modelClass.isAssignableFrom(VisitsViewModel::class.java) ->
            VisitsViewModel(
                getVisits = container.getVisits,
                cancelVisit = container.cancelVisit,
            ) as T

        else -> throw IllegalArgumentException("ViewModel no soportado: ${modelClass.canonicalName}")
    }
}
