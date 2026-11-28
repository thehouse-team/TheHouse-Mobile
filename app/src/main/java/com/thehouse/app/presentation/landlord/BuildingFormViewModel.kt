package com.thehouse.app.presentation.landlord

import com.thehouse.app.domain.error.LandlordErrorCode
import com.thehouse.app.domain.error.LandlordException
import com.thehouse.app.domain.location.CoordinateParser
import com.thehouse.app.domain.location.DeviceLocationProvider
import com.thehouse.app.domain.location.LocationResult
import com.thehouse.app.domain.model.AllowedTenants
import com.thehouse.app.domain.model.Building
import com.thehouse.app.domain.model.BuildingAddress
import com.thehouse.app.domain.model.BuildingLocation
import com.thehouse.app.domain.model.BuildingPatch
import com.thehouse.app.domain.model.BuildingType
import com.thehouse.app.domain.model.Service
import com.thehouse.app.domain.model.University
import com.thehouse.app.domain.repository.NearbyUniversity
import com.thehouse.app.domain.usecase.CreateBuildingUseCase
import com.thehouse.app.domain.usecase.GetLandlordBuildingUseCase
import com.thehouse.app.domain.usecase.GetUniversitiesUseCase
import com.thehouse.app.domain.usecase.SearchNearbyUniversitiesUseCase
import com.thehouse.app.domain.usecase.UpdateBuildingUseCase
import com.thehouse.app.presentation.common.BaseViewModel
import com.thehouse.app.presentation.common.UiEvent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

/**
 * Única fuente de validación del formulario de edificio.
 *
 * Cada campo expone su propio error y [pendingFields] es la lista ordenada de
 * campos pendientes. `isValid` se deriva de esa lista: no hay condiciones
 * duplicadas entre el estado y los mensajes visibles.
 *
 * Reglas espejo del Backend (`BuildingService.CreateAsync` / `Address` /
 * `Location`): name 2..120 tras trim; street 1..120; number 1..20;
 * neighborhood 1..120; city 1..80; state 1..60; postalCode 5 dígitos tras trim;
 * lat 14..33; lng -118.5..-86; nearestUniversityId presente; rules <= 2000.
 */
object BuildingFormValidation {

    const val FIELD_NAME = "name"
    const val FIELD_STREET = "street"
    const val FIELD_NUMBER = "number"
    const val FIELD_NEIGHBORHOOD = "neighborhood"
    const val FIELD_CITY = "city"
    const val FIELD_STATE = "state"
    const val FIELD_POSTAL_CODE = "postalCode"
    const val FIELD_LAT = "lat"
    const val FIELD_LNG = "lng"
    const val FIELD_UNIVERSITY = "university"
    const val FIELD_RULES = "rules"

    /** Nombres visibles en español para el resumen compacto. */
    private val labels = mapOf(
        FIELD_NAME to "Nombre",
        FIELD_STREET to "Calle",
        FIELD_NUMBER to "Número",
        FIELD_NEIGHBORHOOD to "Colonia",
        FIELD_CITY to "Ciudad",
        FIELD_STATE to "Estado",
        FIELD_POSTAL_CODE to "C.P.",
        FIELD_LAT to "Latitud",
        FIELD_LNG to "Longitud",
        FIELD_UNIVERSITY to "Universidad cercana",
        FIELD_RULES to "Reglas",
    )

    fun label(field: String): String = labels[field] ?: field

    fun errors(
        name: String,
        street: String,
        number: String,
        neighborhood: String,
        city: String,
        state: String,
        postalCode: String,
        lat: String,
        lng: String,
        universityId: String,
        rules: String,
    ): Map<String, String> = buildMap {
        fun check(field: String, condition: Boolean, message: String) {
            if (!condition) put(field, message)
        }
        val trimmedName = name.trim()
        check(
            FIELD_NAME,
            trimmedName.length in 2..120,
            "Nombre de 2 a 120 caracteres.",
        )
        check(
            FIELD_STREET,
            street.trim().length in 1..120,
            "La calle es obligatoria (máx. 120 caracteres).",
        )
        check(
            FIELD_NUMBER,
            number.trim().length in 1..20,
            "El número es obligatorio (máx. 20 caracteres).",
        )
        check(
            FIELD_NEIGHBORHOOD,
            neighborhood.trim().length in 1..120,
            "La colonia es obligatoria (máx. 120 caracteres).",
        )
        check(
            FIELD_CITY,
            city.trim().length in 1..80,
            "La ciudad es obligatoria (máx. 80 caracteres).",
        )
        check(
            FIELD_STATE,
            state.trim().length in 1..60,
            "El estado es obligatorio (máx. 60 caracteres).",
        )
        check(
            FIELD_POSTAL_CODE,
            postalCode.trim().matches(Regex("^\\d{5}$")),
            "C.P. de 5 dígitos.",
        )
        check(
            FIELD_LAT,
            CoordinateParser.latitudeOrNull(lat) != null,
            if (lat.isBlank()) "Escribe la latitud (14 a 33)." else "Latitud inválida. Usa números entre 14 y 33.",
        )
        check(
            FIELD_LNG,
            CoordinateParser.longitudeOrNull(lng) != null,
            if (lng.isBlank()) "Escribe la longitud (-118.5 a -86)."
            else "Longitud inválida. Usa números entre -118.5 y -86.",
        )
        check(FIELD_UNIVERSITY, universityId.isNotBlank(), "Selecciona la universidad más cercana.")
        check(FIELD_RULES, rules.length <= 2000, "Las reglas no pueden superar 2000 caracteres.")
    }
}

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
    /** Texto original: nunca se reemplaza mientras el usuario escribe. */
    val lat: String = "",
    val lng: String = "",
    val latTouched: Boolean = false,
    val lngTouched: Boolean = false,
    val latBlurred: Boolean = false,
    val lngBlurred: Boolean = false,
    val submitAttempted: Boolean = false,
    val universityId: String = "",
    val universities: List<University> = emptyList(),
    val nearbyUniversities: List<NearbyUniversity> = emptyList(),
    val nearbySearching: Boolean = false,
    val nearbyError: String? = null,
    val isNearbyResult: Boolean = false,
    val isLocating: Boolean = false,
    val locationMessage: String? = null,
    val approximateLocation: Boolean = false,
    val allowedTenants: AllowedTenants = AllowedTenants.ANY,
    val selectedServices: Set<Service> = emptySet(),
    val rules: String = "",
    val version: Int = 0,
    val status: com.thehouse.app.domain.model.BuildingStatus? = null,
    val versionConflict: Boolean = false,
    val errorCode: LandlordErrorCode? = null,
    val errorMessage: String? = null,
    /** Campos pendientes tras pulsar Submit; se muestran como resumen compacto. */
    val pendingFields: List<String> = emptyList(),
    /** Primer campo inválido, para llevar el foco cuando sea viable. */
    val firstInvalidField: String? = null,
) {
    /** Fuente única: todos los errores de campo salen del mismo mapa ordenado. */
    private val fieldErrors: Map<String, String> by lazy {
        BuildingFormValidation.errors(
            name = name,
            street = street,
            number = number,
            neighborhood = neighborhood,
            city = city,
            state = state,
            postalCode = postalCode,
            lat = lat,
            lng = lng,
            universityId = universityId,
            rules = rules,
        )
    }

    val nameError: String? get() = fieldErrors[BuildingFormValidation.FIELD_NAME]
    val streetError: String? get() = fieldErrors[BuildingFormValidation.FIELD_STREET]
    val numberError: String? get() = fieldErrors[BuildingFormValidation.FIELD_NUMBER]
    val neighborhoodError: String? get() = fieldErrors[BuildingFormValidation.FIELD_NEIGHBORHOOD]
    val cityError: String? get() = fieldErrors[BuildingFormValidation.FIELD_CITY]
    val stateError: String? get() = fieldErrors[BuildingFormValidation.FIELD_STATE]
    val postalCodeError: String? get() = fieldErrors[BuildingFormValidation.FIELD_POSTAL_CODE]
    val rulesError: String? get() = fieldErrors[BuildingFormValidation.FIELD_RULES]
    val universityError: String? get() = fieldErrors[BuildingFormValidation.FIELD_UNIVERSITY]

    /** Solo se muestra error después de interacción con el campo, pérdida de foco o envío. */
    val showLatError: Boolean get() = latBlurred || submitAttempted
    val showLngError: Boolean get() = lngBlurred || submitAttempted
    val latError: String? get() = fieldErrors[BuildingFormValidation.FIELD_LAT]?.takeIf { showLatError }
    val lngError: String? get() = fieldErrors[BuildingFormValidation.FIELD_LNG]?.takeIf { showLngError }
    val showNumberError: Boolean get() = submitAttempted
    val showNeighborhoodError: Boolean get() = submitAttempted

    /** Derivado de la misma fuente que los mensajes: sin condiciones duplicadas. */
    val validationErrors: Map<String, String> get() = fieldErrors
    val isValid: Boolean get() = fieldErrors.isEmpty()
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
    data class LatFocusLost(val value: String) : BuildingFormEvent
    data class LngFocusLost(val value: String) : BuildingFormEvent
    data class UniversityChanged(val value: String) : BuildingFormEvent
    data class TenantsChanged(val value: AllowedTenants) : BuildingFormEvent
    data class ServiceToggled(val value: Service) : BuildingFormEvent
    data class RulesChanged(val value: String) : BuildingFormEvent
    data object UseCurrentLocationClicked : BuildingFormEvent
    data class LocationPermissionResult(val fineGranted: Boolean, val coarseGranted: Boolean) : BuildingFormEvent
    data object RetryNearbySearch : BuildingFormEvent
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
    private val searchNearbyUniversities: SearchNearbyUniversitiesUseCase,
    private val deviceLocationProvider: DeviceLocationProvider,
) : BaseViewModel<BuildingFormUiState, BuildingFormEvent, BuildingFormEffect>(BuildingFormUiState()) {

    companion object {
        private const val NEARBY_DEBOUNCE_MS = 500L
    }

    private var nearbyJob: Job? = null
    private var pendingLocationRequest = false
    private var manualUniversitySelected = false

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
            is BuildingFormEvent.LatChanged -> updateState {
                it.copy(lat = event.value, latTouched = true)
            }.also { scheduleNearbySearch() }
            is BuildingFormEvent.LngChanged -> updateState {
                it.copy(lng = event.value, lngTouched = true)
            }.also { scheduleNearbySearch() }
            is BuildingFormEvent.LatFocusLost -> updateState {
                it.copy(lat = event.value, latTouched = true, latBlurred = true)
            }
            is BuildingFormEvent.LngFocusLost -> updateState {
                it.copy(lng = event.value, lngTouched = true, lngBlurred = true)
            }
            is BuildingFormEvent.UniversityChanged -> {
                manualUniversitySelected = true
                updateState { it.copy(universityId = event.value) }
            }
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
            BuildingFormEvent.UseCurrentLocationClicked -> requestLocation()
            is BuildingFormEvent.LocationPermissionResult -> {
                if (pendingLocationRequest) {
                    pendingLocationRequest = false
                    if (event.fineGranted || event.coarseGranted) {
                        readLocation(event.fineGranted, event.coarseGranted)
                    } else {
                        updateState {
                            it.copy(
                                isLocating = false,
                                locationMessage = "Sin permiso de ubicación. Puedes escribir las coordenadas manualmente.",
                            )
                        }
                    }
                }
            }
            BuildingFormEvent.RetryNearbySearch -> scheduleNearbySearch(immediate = true)
            BuildingFormEvent.Submit -> submit()
        }
    }

    private fun load() {
        updateState { it.copy(isLoading = true, versionConflict = false, errorCode = null, errorMessage = null) }
        launch {
            getUniversities().fold(
                onSuccess = { universitiesPage ->
                    val catalogIds = universitiesPage.items.map { it.id }.toSet()
                    updateState { current ->
                        // Solo preseleccionar si no hay selección manual/válida. HTTP 200
                        // no garantiza que el estado ya tuviera un ID correcto.
                        val existing = current.universityId
                        val resolved = when {
                            existing.isNotBlank() && existing in catalogIds -> existing
                            existing.isNotBlank() && manualUniversitySelected -> existing
                            else -> universitiesPage.items.firstOrNull()?.id.orEmpty()
                        }
                        current.copy(
                            universities = universitiesPage.items,
                            universityId = resolved,
                        )
                    }
                    if (uiState.value.universityId.isNotBlank()) {
                        BuildingFormFlowLogger.universityPreselected("catalog")
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
                updateState { it.copy(isLoading = false) }
            }
        }
    }

    private fun applyBuilding(building: Building) {
        manualUniversitySelected = true
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

    private fun requestLocation() {
        pendingLocationRequest = true
        updateState { it.copy(isLocating = false, locationMessage = null) }
    }

    private fun readLocation(fineGranted: Boolean, coarseGranted: Boolean) {
        updateState { it.copy(isLocating = true, locationMessage = "Obteniendo ubicación…") }
        launch {
            val result = try {
                deviceLocationProvider.currentLocation(fineGranted, coarseGranted)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (unexpected: Exception) {
                LocationResult.Unavailable
            }
            when (result) {
                is LocationResult.Success -> {
                    val latValue = result.location.lat
                    val lngValue = result.location.lng
                    val approximate = !fineGranted && (result.location.accuracyMeters ?: 0f) > 100f
                    updateState {
                        it.copy(
                            isLocating = false,
                            lat = latValue.toString(),
                            lng = lngValue.toString(),
                            latTouched = true,
                            lngTouched = true,
                            approximateLocation = approximate,
                            locationMessage = if (approximate) {
                                "Ubicación aproximada. Confirma que corresponda al inmueble y ajústala si hace falta."
                            } else {
                                "Confirma que las coordenadas correspondan al inmueble, no solo a tu ubicación."
                            },
                        )
                    }
                    searchNearby(latValue, lngValue)
                }
                LocationResult.PermissionDenied, LocationResult.PermanentlyDenied -> updateState {
                    it.copy(
                        isLocating = false,
                        locationMessage = "Permiso no concedido. Puedes escribir las coordenadas manualmente o activarlo desde Ajustes.",
                    )
                }
                LocationResult.ServicesDisabled -> updateState {
                    it.copy(
                        isLocating = false,
                        locationMessage = "Los servicios de ubicación están desactivados. Actívalos o escribe las coordenadas.",
                    )
                }
                LocationResult.Timeout -> updateState {
                    it.copy(
                        isLocating = false,
                        locationMessage = "No se pudo obtener la ubicación a tiempo. Puedes reintentar o escribirla manualmente.",
                    )
                }
                LocationResult.Cancelled -> updateState { it.copy(isLocating = false) }
                LocationResult.Unavailable -> updateState {
                    it.copy(
                        isLocating = false,
                        locationMessage = "Ubicación no disponible. Escribe las coordenadas manualmente.",
                    )
                }
            }
        }
    }

    /** El texto se conserva; la búsqueda solo se dispara con coordenadas válidas. */
    private fun scheduleNearbySearch(immediate: Boolean = false) {
        nearbyJob?.cancel()
        nearbyJob = launch {
            if (!immediate) delay(NEARBY_DEBOUNCE_MS)
            val current = uiState.value
            val lat = CoordinateParser.latitudeOrNull(current.lat)
            val lng = CoordinateParser.longitudeOrNull(current.lng)
            if (lat != null && lng != null) performNearbySearch(lat, lng)
        }
    }

    /** Búsqueda inmediata (GPS): cancela cualquier búsqueda pendiente. */
    private fun searchNearby(lat: Double, lng: Double) {
        nearbyJob?.cancel()
        nearbyJob = launch { performNearbySearch(lat, lng) }
    }

    private suspend fun performNearbySearch(lat: Double, lng: Double) {
        updateState { it.copy(nearbySearching = true, nearbyError = null) }
        searchNearbyUniversities(lat, lng).fold(
            onSuccess = { items ->
                updateState { current ->
                    val stillValid = CoordinateParser.latitudeOrNull(current.lat) == lat &&
                        CoordinateParser.longitudeOrNull(current.lng) == lng
                    if (!stillValid) return@updateState current.copy(nearbySearching = false)
                    val currentId = current.universityId
                    val keepManual = manualUniversitySelected && currentId.isNotBlank() &&
                        items.any { it.id == currentId }
                    // Una búsqueda con resultados vacíos NO debe borrar una selección
                    // válida: antes `firstOrNull()?.id.orEmpty()` la dejaba en blanco.
                    val resolved = when {
                        keepManual -> currentId
                        currentId.isNotBlank() && items.isEmpty() -> currentId
                        else -> items.firstOrNull()?.id ?: currentId
                    }
                    if (resolved == currentId && currentId.isNotBlank()) {
                        BuildingFormFlowLogger.universitySelectionPreserved()
                    }
                    current.copy(
                        nearbySearching = false,
                        nearbyError = null,
                        nearbyUniversities = items,
                        isNearbyResult = items.isNotEmpty(),
                        universityId = resolved,
                    )
                }
            },
            onFailure = {
                updateState {
                    it.copy(
                        nearbySearching = false,
                        nearbyError = "No se pudieron buscar universidades cercanas. Revisa las coordenadas o usa el catálogo.",
                    )
                }
            },
        )
    }

    /** PATCH parcial con version: solo campos editables; el Backend es autoridad. */
    private fun submit() {
        if (uiState.value.isSaving || uiState.value.isLoading) return
        updateState { it.copy(submitAttempted = true) }
        val current = uiState.value
        if (!current.isValid) {
            val pending = current.validationErrors.keys.toList()
            updateState {
                it.copy(
                    pendingFields = pending,
                    firstInvalidField = pending.firstOrNull(),
                )
            }
            // Diagnóstico seguro: solo nombres de campos, nunca valores del formulario.
            BuildingFormFlowLogger.validationFailed(pending)
            return
        }
        updateState {
            it.copy(
                isSaving = true,
                errorCode = null,
                errorMessage = null,
                versionConflict = false,
                pendingFields = emptyList(),
                firstInvalidField = null,
            )
        }
        launch {
            val latValue = CoordinateParser.latitudeOrNull(current.lat)!!
            val lngValue = CoordinateParser.longitudeOrNull(current.lng)!!
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
                        location = BuildingLocation(lat = latValue, lng = lngValue),
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
                    location = BuildingLocation(lat = latValue, lng = lngValue),
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
        if (throwable is CancellationException) throw throwable
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
