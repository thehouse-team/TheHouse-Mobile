package com.thehouse.app.presentation.preferences

import com.thehouse.app.domain.model.Budget
import com.thehouse.app.domain.model.Preference
import com.thehouse.app.domain.repository.PreferenceRepository
import com.thehouse.app.domain.usecase.GetPreferencesUseCase
import com.thehouse.app.domain.usecase.SavePreferencesUseCase
import com.thehouse.app.presentation.common.BaseViewModel
import com.thehouse.app.presentation.common.UiEffect
import com.thehouse.app.presentation.common.UiEvent
import com.thehouse.app.presentation.common.toSafeUiMessage
import java.util.Locale
import kotlinx.coroutines.CancellationException

/**
 * Estado editable de preferencias.
 *
 * Los campos reflejan exclusivamente el modelo real [Preference]. No se inventan
 * opciones ni contratos adicionales.
 */
data class PreferencesUiState(
    val isLoading: Boolean = true,
    val loadError: String? = null,
    val isSaving: Boolean = false,
    val saveError: String? = null,
    val message: String? = null,
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
}

sealed interface PreferencesEvent : UiEvent {
    data object Load : PreferencesEvent
    data object Retry : PreferencesEvent
    data object Save : PreferencesEvent
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
        updateState { it.copy(isLoading = true, loadError = null, saveError = null, message = null) }
        launch {
            try {
                preferenceRepository.loadPreferences()
                val preference = getPreferences()
                updateState { applyPreference(it.copy(isLoading = false, loadError = null), preference) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (throwable: Throwable) {
                updateState { it.copy(isLoading = false, loadError = throwable.toSafeUiMessage()) }
            }
        }
    }

    private fun save() {
        val current = uiState.value
        if (current.isSaving || current.isLoading) return

        val min = parseDouble(current.budgetMin)
        val max = parseDouble(current.budgetMax)
        val distance = parseDouble(current.maxDistanceKm)

        val budgetMinError = numberError(current.budgetMin, min)
        val budgetMaxError = numberError(current.budgetMax, max)
        val maxDistanceError = numberError(current.maxDistanceKm, distance)

        if (budgetMinError != null || budgetMaxError != null || maxDistanceError != null) {
            updateState {
                it.copy(
                    budgetMinError = budgetMinError,
                    budgetMaxError = budgetMaxError,
                    maxDistanceError = maxDistanceError,
                    saveError = "Revisa los campos marcados.",
                    message = null,
                )
            }
            return
        }

        val preference = buildPreference(current, min, max, distance)
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
                        saveError = throwable.toSafeUiMessage(),
                    )
                }
            }
        }
    }

    private fun applyPreference(state: PreferencesUiState, preference: Preference?): PreferencesUiState {
        val budget = preference?.budget
        return state.copy(
            budgetMin = formatNumber(budget?.min),
            budgetMax = formatNumber(budget?.max),
            budgetCurrency = budget?.currency ?: preference?.currency.orEmpty(),
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
        val budget = if (min != null || max != null || budgetCurrency != null) {
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

    private fun parseDouble(value: String): Double? {
        val trimmed = value.trim()
        if (trimmed.isBlank()) return null
        return trimmed.replace(',', '.').toDoubleOrNull()
    }

    private fun numberError(raw: String, parsed: Double?): String? {
        if (raw.trim().isBlank()) return null
        return if (parsed == null) "Ingresa un número válido." else null
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
