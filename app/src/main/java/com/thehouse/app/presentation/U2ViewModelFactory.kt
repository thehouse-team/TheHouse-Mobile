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
import com.thehouse.app.presentation.preferences.PreferencesViewModel
import com.thehouse.app.presentation.profile.ProfileViewModel
import com.thehouse.app.presentation.quicksetup.QuickSetupViewModel
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
                connectivityObserver = container.connectivityObserver,
            )
        }
    }

    /** D18: detalle real de cuarto (GET /api/v1/rooms/{roomId}). */
    fun roomDetail(roomId: String): ViewModelProvider.Factory = viewModelFactory {
        initializer {
            com.thehouse.app.presentation.roomdetail.RoomDetailViewModel(
                roomId = roomId,
                getRoomDetail = container.getRoomDetail,
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
                    onConversationRead = {
                        // NV-14: leer conversación marca aviso como leído; refrescar unread count.
                        container.getUnreadNotificationsCount(null)
                    },
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
                uploadBuildingPhoto = container.uploadBuildingPhoto,
                deleteBuildingPhoto = container.deleteBuildingPhoto,
                uploadIneDocument = container.uploadIneDocument,
                uploadPropertyProofDocument = container.uploadPropertyProofDocument,
                getBuildingDocuments = container.getBuildingDocuments,
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
                uploadRoomPhoto = container.uploadRoomPhoto,
                deleteRoomPhoto = container.deleteRoomPhoto,
            )
        }
    }

    /** NV-15: privacidad + eliminación de cuenta. */
    fun privacy(): ViewModelProvider.Factory = viewModelFactory {
        initializer {
            com.thehouse.app.presentation.privacy.PrivacyViewModel(
                privacyPolicyUrl = container.config.privacyPolicyUrl,
                sessionState = container.sessionStore.state,
                deleteAccount = container.deleteAccount,
                legalRepository = container.legalRepository,
                legalUpToDateProvider = { container.legalUpToDate.value },
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

    fun createAgreement(requestId: String): ViewModelProvider.Factory = viewModelFactory {
        initializer {
            com.thehouse.app.presentation.landlord.CreateAgreementViewModel(
                requestId = requestId,
                createAgreement = container.createAgreement,
            )
        }
    }

    fun landlordAgreementDetail(agreementId: String): ViewModelProvider.Factory = viewModelFactory {
        initializer {
            com.thehouse.app.presentation.landlord.LandlordAgreementDetailViewModel(
                agreementId = agreementId,
                getAgreement = container.getLandlordAgreement,
                issueCode = container.issueAgreementCode,
                cancelAgreement = container.cancelAgreement,
                endAgreement = container.endAgreement,
            )
        }
    }

    fun rentalCode(): ViewModelProvider.Factory = viewModelFactory {
        initializer {
            com.thehouse.app.presentation.rental.RentalCodeViewModel(
                previewAgreement = container.previewAgreement,
            )
        }
    }

    fun agreementPreview(code: String): ViewModelProvider.Factory = viewModelFactory {
        initializer {
            com.thehouse.app.presentation.rental.AgreementPreviewViewModel(
                code = code,
                previewAgreement = container.previewAgreement,
                acceptAgreement = container.acceptAgreement,
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
                getUnreadNotificationsCount = container.getUnreadNotificationsCount,
                pushRefreshSignals = container.pushRefreshSignals,
            ) as T

        modelClass.isAssignableFrom(ExploreViewModel::class.java) ->
            ExploreViewModel(
                getProperties = container.getProperties,
                searchProperties = container.searchProperties,
                connectivityObserver = container.connectivityObserver,
            ) as T

        modelClass.isAssignableFrom(FavoritesViewModel::class.java) ->
            FavoritesViewModel(
                getFavorites = container.getFavorites,
                removeFavorite = container.removeFavorite,
                favoriteIdsStore = container.favoriteIdsStore,
                sessionState = container.sessionStore.state,
                connectivityObserver = container.connectivityObserver,
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
                getLandlordStatsRooms = container.getLandlordStatsRooms,
            ) as T

        modelClass.isAssignableFrom(com.thehouse.app.presentation.landlord.LandlordAgreementsViewModel::class.java) ->
            com.thehouse.app.presentation.landlord.LandlordAgreementsViewModel(
                getLandlordAgreements = container.getLandlordAgreements,
            ) as T

        modelClass.isAssignableFrom(com.thehouse.app.presentation.rental.MyRentalViewModel::class.java) ->
            com.thehouse.app.presentation.rental.MyRentalViewModel(
                getMyRental = container.getMyRental,
            ) as T

        modelClass.isAssignableFrom(com.thehouse.app.presentation.smartmatch.SmartMatchViewModel::class.java) ->
            com.thehouse.app.presentation.smartmatch.SmartMatchViewModel(
                sessionState = container.sessionStore.state,
                preferenceRepository = container.preferencesRepository,
                smartMatchRepository = container.smartMatchRepository,
                getProperties = container.getProperties,
                runSmartMatch = container.runSmartMatch,
                dispatchers = container.dispatchers,
                connectivityObserver = container.connectivityObserver,
            ) as T

        modelClass.isAssignableFrom(com.thehouse.app.presentation.notifications.NotificationsViewModel::class.java) ->
            com.thehouse.app.presentation.notifications.NotificationsViewModel(
                getNotifications = container.getNotifications,
                getUnreadCount = container.getUnreadNotificationsCount,
                markNotificationRead = container.markNotificationRead,
                markAllNotificationsRead = container.markAllNotificationsRead,
                pushRefreshSignals = container.pushRefreshSignals,
            ) as T

        modelClass.isAssignableFrom(ProfileViewModel::class.java) ->
            ProfileViewModel(
                profileRepository = container.profileRepository,
                getProfile = container.getProfile,
                updateProfile = container.updateProfile,
                selectUserUniversity = container.selectUserUniversity,
                getUniversities = container.getUniversities,
                sessionState = container.sessionStore.state,
            ) as T

        modelClass.isAssignableFrom(PreferencesViewModel::class.java) ->
            PreferencesViewModel(
                preferenceRepository = container.preferencesRepository,
                getPreferences = container.getPreferences,
                savePreferences = container.savePreferences,
            ) as T

        // HOTFIX: LandlordDashboardDestination crea este ViewModel con el factory
        // genérico cuando el usuario aún no tiene perfil de arrendador. Sin este
        // branch, `create()` lanza IllegalArgumentException y la app crashea al
        // abrir "Mis alojamientos" (confirmado por dropbox data_app_crash).
        modelClass.isAssignableFrom(com.thehouse.app.presentation.landlord.LandlordProfileViewModel::class.java) ->
            com.thehouse.app.presentation.landlord.LandlordProfileViewModel(
                activateLandlord = container.activateLandlord,
            ) as T

        modelClass.isAssignableFrom(QuickSetupViewModel::class.java) ->
            QuickSetupViewModel(
                profileRepository = container.profileRepository,
                getProfile = container.getProfile,
                updateProfile = container.updateProfile,
                selectUserUniversity = container.selectUserUniversity,
                getUniversities = container.getUniversities,
            ) as T

        else -> throw IllegalArgumentException("ViewModel no soportado: ${modelClass.canonicalName}")
    }
}
