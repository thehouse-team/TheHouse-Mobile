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

    fun createRequest(roomId: String): ViewModelProvider.Factory = viewModelFactory {
        initializer {
            com.thehouse.app.presentation.requests.CreateRequestViewModel(
                roomId = roomId,
                createRequest = container.createRequest,
            )
        }
    }

    fun requestDetail(requestId: String): ViewModelProvider.Factory = viewModelFactory {
        initializer {
            com.thehouse.app.presentation.requests.RequestDetailViewModel(
                requestId = requestId,
                getRequest = container.getRequest,
                cancelRequest = container.cancelRequest,
            )
        }
    }

    fun landlordRequestDetail(requestId: String): ViewModelProvider.Factory = viewModelFactory {
        initializer {
            com.thehouse.app.presentation.landlord.LandlordRequestDetailViewModel(
                requestId = requestId,
                getRequest = container.getRequest,
                acceptRequest = container.acceptRequest,
                rejectRequest = container.rejectRequest,
            )
        }
    }

    fun conversation(conversationId: String, initialClosed: Boolean): ViewModelProvider.Factory =
        viewModelFactory {
            initializer {
                com.thehouse.app.presentation.chat.ConversationViewModel(
                    conversationId = conversationId,
                    getMessages = container.getMessages,
                    sendMessage = container.sendMessage,
                    markRead = container.markConversationRead,
                )
            }
        }

    fun landlordProfile(): ViewModelProvider.Factory = viewModelFactory {
        initializer {
            com.thehouse.app.presentation.landlord.LandlordProfileViewModel(
                activateLandlord = container.activateLandlord,
            )
        }
    }

    fun buildingForm(buildingId: String?): ViewModelProvider.Factory = viewModelFactory {
        initializer {
            com.thehouse.app.presentation.landlord.BuildingFormViewModel(
                buildingId = buildingId,
                createBuilding = container.createBuilding,
                updateBuilding = container.updateBuilding,
                getBuilding = container.getLandlordBuilding,
                getUniversities = container.getUniversities,
            )
        }
    }

    fun buildingDetail(buildingId: String): ViewModelProvider.Factory = viewModelFactory {
        initializer {
            com.thehouse.app.presentation.landlord.BuildingDetailViewModel(
                buildingId = buildingId,
                getBuilding = container.getLandlordBuilding,
                getRooms = container.getLandlordRooms,
                getPublication = container.getPublicationStatus,
                publishBuilding = container.publishBuilding,
                deleteBuilding = container.deleteBuilding,
                pauseRoom = container.pauseRoom,
                resumeRoom = container.resumeRoom,
                getVerification = container.getBuildingVerification,
            )
        }
    }

    fun roomForm(buildingId: String, roomId: String?): ViewModelProvider.Factory = viewModelFactory {
        initializer {
            com.thehouse.app.presentation.landlord.RoomFormViewModel(
                buildingId = buildingId,
                roomId = roomId,
                generateRooms = container.generateRooms,
                updateRoom = container.updateRoom,
                deleteRoom = container.deleteRoom,
                getRooms = container.getLandlordRooms,
            )
        }
    }

    fun report(targetType: String, targetId: String): ViewModelProvider.Factory = viewModelFactory {
        initializer {
            val type = com.thehouse.app.domain.model.ReportTargetType.entries
                .firstOrNull { it.api == targetType }
                ?: com.thehouse.app.domain.model.ReportTargetType.ROOM
            com.thehouse.app.presentation.reports.ReportViewModel(
                targetType = type,
                targetId = targetId,
                createReport = container.createReport,
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
                getProperties = container.getProperties,
                searchProperties = container.searchProperties,
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

        modelClass.isAssignableFrom(com.thehouse.app.presentation.requests.RequestsViewModel::class.java) ->
            com.thehouse.app.presentation.requests.RequestsViewModel(
                getRequests = container.getUserRequests,
                cancelRequest = container.cancelRequest,
            ) as T

        modelClass.isAssignableFrom(com.thehouse.app.presentation.landlord.LandlordRequestsViewModel::class.java) ->
            com.thehouse.app.presentation.landlord.LandlordRequestsViewModel(
                getLandlordRequests = container.getLandlordRequests,
                acceptRequest = container.acceptRequest,
                rejectRequest = container.rejectRequest,
            ) as T

        modelClass.isAssignableFrom(com.thehouse.app.presentation.chat.ConversationsViewModel::class.java) ->
            com.thehouse.app.presentation.chat.ConversationsViewModel(
                getConversations = container.getConversations,
            ) as T

        modelClass.isAssignableFrom(com.thehouse.app.presentation.landlord.LandlordDashboardViewModel::class.java) ->
            com.thehouse.app.presentation.landlord.LandlordDashboardViewModel(
                getProfile = container.getLandlordProfile,
                getBuildings = container.getLandlordBuildings,
            ) as T

        else -> throw IllegalArgumentException("ViewModel no soportado: ${modelClass.canonicalName}")
    }
}
