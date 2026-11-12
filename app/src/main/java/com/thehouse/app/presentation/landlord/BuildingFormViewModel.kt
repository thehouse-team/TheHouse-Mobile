package com.thehouse.app.presentation.landlord

import com.thehouse.app.domain.error.LandlordErrorCode
import com.thehouse.app.domain.error.LandlordException
import com.thehouse.app.domain.model.AllowedTenants
import com.thehouse.app.domain.model.Building
import com.thehouse.app.domain.model.BuildingAddress
import com.thehouse.app.domain.model.BuildingLocation
import com.thehouse.app.domain.model.BuildingPatch
import com.thehouse.app.domain.model.BuildingType
import com.thehouse.app.domain.model.Service
import com.thehouse.app.domain.usecase.CreateBuildingUseCase
import com.thehouse.app.domain.usecase.GetLandlordBuildingUseCase
import com.thehouse.app.domain.usecase.GetUniversitiesUseCase
import com.thehouse.app.domain.usecase.UpdateBuildingUseCase
import com.thehouse.app.presentation.common.BaseViewModel
import com.thehouse.app.presentation.common.UiEvent

/** Formulario de edificio: crear (draft) o editar (PATCH parcial con version). */
data class BuildingFormUiState(
    val isEdit: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val name: String = "",
    val type: BuildingType = BuildingType.ROOMS_BUILDING,
    val street: String = "",
    val number: String = "",
    val neighborhood: String = "",
    val city: String = "",
    val state: String = "",
    val postalCode: String = "",
    val lat: String = "",
    val lng: String = "",
    val universityId: String = "",
    val universities: List<com.thehouse.app.domain.model.University> = emptyList(),
    val allowedTenants: AllowedTenants = AllowedTenants.ANY,
    val selectedServices: Set<Service> = emptySet(),
    val rules: String = "",
    val version: Int = 0,
    val status: com.thehouse.app.domain.model.BuildingStatus? = null,
    val versionConflict: Boolean = false,
    val errorCode: LandlordErrorCode? = null,
    val errorMessage: String? = null,
) {
    val nameError: String? = if (name.length in 2..120) null else "Nombre de 2 a 120 caracteres."
    val streetError: String? = if (street.isNotBlank()) null else "La calle es obligatoria."
    val cityError: String? = if (city.isNotBlank()) null else "La ciudad es obligatoria."
    val stateError: String? = if (state.isNotBlank()) null else "El estado es obligatorio."
    val postalCodeError: String? = if (postalCode.matches(Regex("^\\d{5}$"))) null else "C.P. de 5 dígitos."
    val latError: String? = lat.toDoubleOrNull()?.let { if (it in -90.0..90.0) null else "Latitud inválida." } ?: "Latitud inválida."
    val lngError: String? = lng.toDoubleOrNull()?.let { if (it in -180.0..180.0) null else "Longitud inválida." } ?: "Longitud inválida."
    val universityError: String? = if (universityId.isNotBlank()) null else "Selecciona la universidad más cercana."
    val isValid: Boolean = nameError == null && streetError == null && cityError == null &&
        stateError == null && postalCodeError == null && latError == null && lngError == null &&
        universityError == null
}

sealed interface BuildingFormEvent : UiEvent {
    data object Load : BuildingFormEvent
    data class NameChanged(val value: String) : BuildingFormEvent
    data class TypeChanged(val value: BuildingType) : BuildingFormEvent
    data class StreetChanged(val value: String) : BuildingFormEvent
    data class NumberChanged(val value: String) : BuildingFormEvent
    data class NeighborhoodChanged(val value: String) : BuildingFormEvent
    data class CityChanged(val value: String) : BuildingFormEvent
    data class StateChanged(val value: String) : BuildingFormEvent
    data class PostalCodeChanged(val value: String) : BuildingFormEvent
    data class LatChanged(val value: String) : BuildingFormEvent
    data class LngChanged(val value: String) : BuildingFormEvent
    data class UniversityChanged(val value: String) : BuildingFormEvent
    data class TenantsChanged(val value: AllowedTenants) : BuildingFormEvent
    data class ServiceToggled(val value: Service) : BuildingFormEvent
    data class RulesChanged(val value: String) : BuildingFormEvent
    data object Submit : BuildingFormEvent
    data object Reload : BuildingFormEvent
}

sealed interface BuildingFormEffect : com.thehouse.app.presentation.common.UiEffect {
    data class Saved(val buildingId: String) : BuildingFormEffect
}

class BuildingFormViewModel(
    private val buildingId: String?,
    private val createBuilding: CreateBuildingUseCase,
    private val updateBuilding: UpdateBuildingUseCase,
    private val getBuilding: GetLandlordBuildingUseCase,
    private val getUniversities: GetUniversitiesUseCase,
) : BaseViewModel<BuildingFormUiState, BuildingFormEvent, BuildingFormEffect>(BuildingFormUiState()) {

    init {
        onEvent(BuildingFormEvent.Load)
    }

    override fun onEvent(event: BuildingFormEvent) {
        when (event) {
            BuildingFormEvent.Load, BuildingFormEvent.Reload -> load()
            is BuildingFormEvent.NameChanged -> updateState { it.copy(name = event.value) }
            is BuildingFormEvent.TypeChanged -> updateState { it.copy(type = event.value) }
            is BuildingFormEvent.StreetChanged -> updateState { it.copy(street = event.value) }
            is BuildingFormEvent.NumberChanged -> updateState { it.copy(number = event.value) }
            is BuildingFormEvent.NeighborhoodChanged -> updateState { it.copy(neighborhood = event.value) }
            is BuildingFormEvent.CityChanged -> updateState { it.copy(city = event.value) }
            is BuildingFormEvent.StateChanged -> updateState { it.copy(state = event.value) }
            is BuildingFormEvent.PostalCodeChanged -> updateState { it.copy(postalCode = event.value) }
            is BuildingFormEvent.LatChanged -> updateState { it.copy(lat = event.value) }
            is BuildingFormEvent.LngChanged -> updateState { it.copy(lng = event.value) }
            is BuildingFormEvent.UniversityChanged -> updateState { it.copy(universityId = event.value) }
            is BuildingFormEvent.TenantsChanged -> updateState { it.copy(allowedTenants = event.value) }
            is BuildingFormEvent.ServiceToggled -> updateState {
                val selected = if (event.value in it.selectedServices) {
                    it.selectedServices - event.value
                } else {
                    it.selectedServices + event.value
                }
                it.copy(selectedServices = selected)
            }
            is BuildingFormEvent.RulesChanged -> updateState { it.copy(rules = event.value.take(2000)) }
            BuildingFormEvent.Submit -> submit()
        }
    }

    private fun load() {
        updateState { it.copy(isLoading = true, versionConflict = false, errorCode = null, errorMessage = null) }
        launch {
            getUniversities().fold(
                onSuccess = { universitiesPage ->
                    updateState { current ->
                        current.copy(
                            universities = universitiesPage.items,
                            universityId = current.universityId.ifBlank {
                                universitiesPage.items.firstOrNull()?.id.orEmpty()
                            },
                        )
                    }
                },
                onFailure = { throwable -> handleFailure(throwable) },
            )
            if (buildingId != null) {
                getBuilding(buildingId).fold(
                    onSuccess = { building -> applyBuilding(building) },
                    onFailure = { throwable -> handleFailure(throwable) },
                )
            } else {
                updateState { it.copy(isLoading = false, isEdit = false) }
            }
        }
    }

    private fun applyBuilding(building: Building) {
        updateState { current ->
            current.copy(
                isEdit = true,
                isLoading = false,
                version = building.version,
                status = building.status,
                name = building.name,
                type = building.type,
                street = building.address.street,
                number = building.address.number,
                neighborhood = building.address.neighborhood,
                city = building.address.city,
                state = building.address.state,
                postalCode = building.address.postalCode,
                lat = building.location.lat.toString(),
                lng = building.location.lng.toString(),
                universityId = building.nearestUniversity.id,
                allowedTenants = building.allowedTenants,
                selectedServices = building.services.toSet(),
                rules = building.rules.orEmpty(),
            )
        }
    }

    /** PATCH parcial con version: solo campos editables; el Backend es autoridad. */
    private fun submit() {
        val current = uiState.value
        if (current.isSaving || !current.isValid || current.isLoading) return
        updateState { it.copy(isSaving = true, errorCode = null, errorMessage = null, versionConflict = false) }
        launch {
            val result = if (current.isEdit && buildingId != null) {
                updateBuilding(
                    id = buildingId,
                    version = current.version,
                    patch = BuildingPatch(
                        type = current.type,
                        name = current.name.trim(),
                        address = BuildingAddress(
                            street = current.street.trim(),
                            number = current.number.trim(),
                            neighborhood = current.neighborhood.trim(),
                            city = current.city.trim(),
                            state = current.state.trim(),
                            postalCode = current.postalCode.trim(),
                        ),
                        location = BuildingLocation(
                            lat = current.lat.toDouble(),
                            lng = current.lng.toDouble(),
                        ),
                        nearestUniversityId = current.universityId,
                        allowedTenants = current.allowedTenants,
                        services = current.selectedServices.toList(),
                        rules = current.rules.takeIf { it.isNotBlank() },
                    ),
                )
            } else {
                createBuilding(
                    type = current.type,
                    name = current.name.trim(),
                    address = BuildingAddress(
                        street = current.street.trim(),
                        number = current.number.trim(),
                        neighborhood = current.neighborhood.trim(),
                        city = current.city.trim(),
                        state = current.state.trim(),
                        postalCode = current.postalCode.trim(),
                    ),
                    location = BuildingLocation(
                        lat = current.lat.toDouble(),
                        lng = current.lng.toDouble(),
                    ),
                    nearestUniversityId = current.universityId,
                    allowedTenants = current.allowedTenants,
                    services = current.selectedServices.toList(),
                    rules = current.rules.takeIf { it.isNotBlank() },
                )
            }
            result.fold(
                onSuccess = { building ->
                    updateState { it.copy(isSaving = false, version = building.version) }
                    sendEffect(BuildingFormEffect.Saved(building.id))
                },
                onFailure = { throwable ->
                    handleFailure(throwable)
                    updateState { it.copy(isSaving = false) }
                },
            )
        }
    }

    private fun handleFailure(throwable: Throwable) {
        val exception = throwable as? LandlordException
        updateState {
            it.copy(
                isLoading = false,
                versionConflict = exception?.code == LandlordErrorCode.VERSION_CONFLICT,
                errorCode = exception?.code,
                errorMessage = LandlordErrorMessages.map(exception),
            )
        }
    }
}
