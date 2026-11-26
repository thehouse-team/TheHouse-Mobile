package com.thehouse.app.presentation.preferences

import com.thehouse.app.data.remote.dto.preferences.PreferencesContract
import com.thehouse.app.domain.error.DomainException
import com.thehouse.app.domain.error.DomainError
import com.thehouse.app.domain.model.Budget
import com.thehouse.app.domain.model.Preference
import com.thehouse.app.domain.repository.PreferenceRepository
import com.thehouse.app.domain.usecase.GetPreferencesUseCase
import com.thehouse.app.domain.usecase.SavePreferencesUseCase
import com.thehouse.app.presentation.common.BaseViewModel
import com.thehouse.app.presentation.common.UiEffect
import com.thehouse.app.presentation.common.UiEvent
import com.thehouse.app.presentation.common.toSafeUiMessage
import com.thehouse.app.preferences.flow.PreferencesFlowLogger
import java.util.Locale
import kotlinx.coroutines.CancellationException

/**
 * Estado editable de preferencias.
 *
 * Los campos reflejan exclusivamente el modelo real [Preference]. D14 añade:
 * - `requiresAuth`: sesión caducada (401), sin mostrar error genérico.
 * - `missingFields`: campos requeridos por Smart Match; `university` se gestiona
 *   en Perfil/Configuración inicial, nunca aquí.
 */
data class PreferencesUiState(
    val isLoading: Boolean = true,
    val loadError: String? = null,
    val requiresAuth: Boolean = false,
    val isSaving: Boolean = false,
    val saveError: String? = null,
    val message: String? = null,
    val missingFields: List<String> = emptyList(),
    val budgetMin: String = "",
    val budgetMax: String = "",
    val budgetCurrency: String = "",
    val maxDistanceKm: String = "",
    val essentialServices: String = "",
    val livingWith: String = "",
    val placeType: String = "",
    val pets: String = "",
    val transport: String = "",
    val propertyTypes: String = "",
    val budgetMinError: String? = null,
    val budgetMaxError: String? = null,
    val maxDistanceError: String? = null,
) {
    val hasValidationErrors: Boolean
        get() = budgetMinError != null || budgetMaxError != null || maxDistanceError != null

    /** Campos gestionables por Preferences que siguen incompletos. */
    val missingHere: List<String>
        get() = missingFields.filter { it != "university" }

    val missingUniversity: Boolean
        get() = missingFields.contains("university")
}

sealed interface PreferencesEvent : UiEvent {
    data object Load : PreferencesEvent
    data object Retry : PreferencesEvent
    data object Save : PreferencesEvent
    data class MissingFieldsChanged(val fields: List<String>) : PreferencesEvent
    data class BudgetMinChanged(val value: String) : PreferencesEvent
    data class BudgetMaxChanged(val value: String) : PreferencesEvent
    data class BudgetCurrencyChanged(val value: String) : PreferencesEvent
    data class MaxDistanceKmChanged(val value: String) : PreferencesEvent
    data class EssentialServicesChanged(val value: String) : PreferencesEvent
    data class LivingWithChanged(val value: String) : PreferencesEvent
    data class PlaceTypeChanged(val value: String) : PreferencesEvent
    data class PetsChanged(val value: String) : PreferencesEvent
    data class TransportChanged(val value: String) : PreferencesEvent
    data class PropertyTypesChanged(val value: String) : PreferencesEvent
    data object ClearMessages : PreferencesEvent
}

sealed interface PreferencesEffect : UiEffect

class PreferencesViewModel(
    private val preferenceRepository: PreferenceRepository,
    private val getPreferences: GetPreferencesUseCase,
    private val savePreferences: SavePreferencesUseCase,
) : BaseViewModel<PreferencesUiState, PreferencesEvent, PreferencesEffect>(
    PreferencesUiState(),
) {

    override fun onEvent(event: PreferencesEvent) {
        when (event) {
            PreferencesEvent.Load,
            PreferencesEvent.Retry -> load()

            PreferencesEvent.Save -> save()

            is PreferencesEvent.MissingFieldsChanged -> updateState {
                it.copy(missingFields = event.fields.distinct(), message = null, saveError = null)
            }

            is PreferencesEvent.BudgetMinChanged -> updateState {
                it.copy(budgetMin = event.value, budgetMinError = null, saveError = null, message = null)
            }

            is PreferencesEvent.BudgetMaxChanged -> updateState {
                it.copy(budgetMax = event.value, budgetMaxError = null, saveError = null, message = null)
            }

            is PreferencesEvent.BudgetCurrencyChanged -> updateState {
                it.copy(budgetCurrency = event.value, saveError = null, message = null)
            }

            is PreferencesEvent.MaxDistanceKmChanged -> updateState {
                it.copy(maxDistanceKm = event.value, maxDistanceError = null, saveError = null, message = null)
            }

            is PreferencesEvent.EssentialServicesChanged -> updateState {
                it.copy(essentialServices = event.value, saveError = null, message = null)
            }

            is PreferencesEvent.LivingWithChanged -> updateState {
                it.copy(livingWith = event.value, saveError = null, message = null)
            }

            is PreferencesEvent.PlaceTypeChanged -> updateState {
                it.copy(placeType = event.value, saveError = null, message = null)
            }

            is PreferencesEvent.PetsChanged -> updateState {
                it.copy(pets = event.value, saveError = null, message = null)
            }

            is PreferencesEvent.TransportChanged -> updateState {
                it.copy(transport = event.value, saveError = null, message = null)
            }

            is PreferencesEvent.PropertyTypesChanged -> updateState {
                it.copy(propertyTypes = event.value, saveError = null, message = null)
            }

            PreferencesEvent.ClearMessages -> updateState {
                it.copy(message = null, saveError = null)
            }
        }
    }

    private fun load() {
        PreferencesFlowLogger.enterScreen()
        PreferencesFlowLogger.loadStart()
        updateState {
            it.copy(
                isLoading = true,
                loadError = null,
                requiresAuth = false,
                saveError = null,
                message = null,
            )
        }
        PreferencesFlowLogger.state("Loading")
        launch {
            try {
                preferenceRepository.loadPreferences()
                val preference = getPreferences()
                updateState { applyPreference(it.copy(isLoading = false, loadError = null), preference) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (throwable: Throwable) {
                val unauthorized = isUnauthorized(throwable)
                updateState {
                    it.copy(
                        isLoading = false,
                        requiresAuth = unauthorized,
                        loadError = if (unauthorized) null else safeMessage(throwable),
                    )
                }
                PreferencesFlowLogger.state(if (unauthorized) "RequiresAuth" else "Error")
                PreferencesFlowLogger.exception(throwable, "load_ui")
            }
        }
    }

    private fun save() {
        val current = uiState.value
        if (current.isSaving || current.isLoading || current.requiresAuth) return

        val min = parseInteger(current.budgetMin)
        val max = parseInteger(current.budgetMax)
        val distance = parseDouble(current.maxDistanceKm)

        val budgetMinError = integerError(current.budgetMin, min)
        val budgetMaxError = integerError(current.budgetMax, max)
        val maxDistanceError = numberError(current.maxDistanceKm, distance)

        val rangeError = if (
            budgetMinError == null && budgetMaxError == null && min != null && max != null &&
            min > max
        ) "El mínimo no puede ser mayor que el máximo." else null
        val currencyError = validateCurrency(current.budgetCurrency)

        if (
            budgetMinError != null || budgetMaxError != null || maxDistanceError != null ||
            rangeError != null || currencyError != null
        ) {
            updateState {
                it.copy(
                    budgetMinError = budgetMinError,
                    budgetMaxError = budgetMaxError ?: rangeError,
                    maxDistanceError = maxDistanceError,
                    saveError = listOfNotNull(
                        budgetMinError, budgetMaxError, maxDistanceError, rangeError, currencyError,
                    ).firstOrNull(),
                    message = null,
                )
            }
            return
        }

        val preference = buildPreference(current, min?.toDouble(), max?.toDouble(), distance)
        updateState { it.copy(isSaving = true, saveError = null, message = null) }
        launch {
            try {
                savePreferences(preference)
                val saved = getPreferences() ?: preference
                updateState {
                    applyPreference(
                        it.copy(
                            isSaving = false,
                            saveError = null,
                            message = "Preferencias guardadas.",
                        ),
                        saved,
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (throwable: Throwable) {
                updateState {
                    it.copy(
                        isSaving = false,
                        requiresAuth = isUnauthorized(throwable),
                        saveError = if (isUnauthorized(throwable)) null else safeMessage(throwable),
                    )
                }
                PreferencesFlowLogger.exception(throwable, "save_ui")
            }
        }
    }

    private fun isUnauthorized(throwable: Throwable): Boolean =
        (throwable as? DomainException)?.error == DomainError.Unauthorized

    /**
     * Los estados contractuales de Preferences tienen mensaje propio; el resto
     * usa el mapper central seguro. Nunca se muestra el mensaje genérico para
     * un estado conocido (400/422 → validación).
     */
    private fun safeMessage(throwable: Throwable): String {
        val error = (throwable as? DomainException)?.error
        return when (error) {
            DomainError.Conflict -> "Revisa los datos ingresados. Algunos valores no son válidos."
            else -> throwable.toSafeUiMessage()
        }
    }

    private fun applyPreference(state: PreferencesUiState, preference: Preference?): PreferencesUiState {
        val budget = preference?.budget
        return state.copy(
            budgetMin = formatNumber(budget?.min),
            budgetMax = formatNumber(budget?.max),
            budgetCurrency = budget?.currency ?: preference?.currency ?: PreferencesContract.CURRENCY,
            maxDistanceKm = formatNumber(preference?.maxDistanceKm),
            essentialServices = preference?.essentialServices.orEmpty().joinToString(", "),
            livingWith = preference?.livingWith.orEmpty(),
            placeType = preference?.placeType.orEmpty(),
            pets = preference?.pets.orEmpty(),
            transport = preference?.transport.orEmpty(),
            propertyTypes = preference?.propertyTypes.orEmpty().joinToString(", "),
            budgetMinError = null,
            budgetMaxError = null,
            maxDistanceError = null,
        )
    }

    private fun buildPreference(
        state: PreferencesUiState,
        min: Double?,
        max: Double?,
        distance: Double?,
    ): Preference {
        val budgetCurrency = state.budgetCurrency.trim().takeIf { it.isNotBlank() }
            ?: PreferencesContract.CURRENCY
        val budget = if (min != null || max != null) {
            Budget(min = min, max = max, currency = budgetCurrency)
        } else {
            null
        }
        return Preference(
            budget = budget,
            maxDistanceKm = distance,
            essentialServices = parseList(state.essentialServices),
            livingWith = state.livingWith.trim().takeIf { it.isNotBlank() },
            placeType = state.placeType.trim().takeIf { it.isNotBlank() },
            pets = state.pets.trim().takeIf { it.isNotBlank() },
            transport = state.transport.trim().takeIf { it.isNotBlank() },
            propertyTypes = parseList(state.propertyTypes),
            currency = budgetCurrency,
        )
    }

    /** El backend usa int? en MXN: un decimal se rechaza, nunca se redondea. */
    private fun parseInteger(value: String): Int? {
        val trimmed = value.trim()
        if (trimmed.isBlank()) return null
        val parsed = trimmed.replace(',', '.').toDoubleOrNull() ?: return null
        if (parsed % 1.0 != 0.0) return null
        return parsed.toInt()
    }

    private fun integerError(raw: String, parsed: Int?): String? {
        if (raw.trim().isBlank()) return null
        if (parsed == null) return "Ingresa un número entero válido."
        if (parsed !in PreferencesContract.BUDGET_MIN..PreferencesContract.BUDGET_MAX) {
            return "El presupuesto debe estar entre ${PreferencesContract.BUDGET_MIN} y ${PreferencesContract.BUDGET_MAX}."
        }
        return null
    }

    private fun validateCurrency(raw: String): String? {
        val value = raw.trim().takeIf { it.isNotBlank() } ?: return null
        return if (value.equals(PreferencesContract.CURRENCY, ignoreCase = true)) null
        else "La moneda debe ser ${PreferencesContract.CURRENCY}."
    }

    private fun parseDouble(value: String): Double? {
        val trimmed = value.trim()
        if (trimmed.isBlank()) return null
        return trimmed.replace(',', '.').toDoubleOrNull()
    }

    private fun numberError(raw: String, parsed: Double?): String? {
        if (raw.trim().isBlank()) return null
        if (parsed == null) return "Ingresa un número válido."
        if (parsed !in PreferencesContract.DISTANCE_MIN..PreferencesContract.DISTANCE_MAX) {
            return "La distancia debe estar entre ${PreferencesContract.DISTANCE_MIN} y ${PreferencesContract.DISTANCE_MAX} km."
        }
        return null
    }

    private fun parseList(value: String): List<String>? {
        val items = value.split(',', '\n')
            .map { it.trim() }
            .filter { it.isNotBlank() }
        return items.takeIf { it.isNotEmpty() }
    }

    private fun formatNumber(value: Double?): String {
        if (value == null) return ""
        return if (value % 1.0 == 0.0) {
            value.toLong().toString()
        } else {
            String.format(Locale.US, "%s", value)
        }
    }
}
