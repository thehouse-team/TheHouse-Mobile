package com.thehouse.app.data.cache

import com.thehouse.app.domain.model.Budget
import com.thehouse.app.domain.model.Preference
import com.thehouse.app.domain.model.University
import com.thehouse.app.domain.model.UserProfile
import kotlinx.serialization.Serializable

@Serializable
data class UniversitySnapshot(
    val id: String,
    val code: String,
    val name: String,
    val city: String,
    val country: String = "",
    val createdAt: String = "",
) {
    fun toDomain() = University(id = id, code = code, name = name, city = city, country = country, createdAt = createdAt)
    companion object {
        fun from(u: University) = UniversitySnapshot(u.id, u.code, u.name, u.city, u.country, u.createdAt)
    }
}

@Serializable
data class ProfileSnapshot(
    val name: String? = null,
    val university: UniversitySnapshot? = null,
    val quickSetupCompleted: Boolean = false,
) {
    fun toDomain() = UserProfile(
        name = name,
        university = university?.toDomain(),
        quickSetupCompleted = quickSetupCompleted,
    )
    companion object {
        fun from(p: UserProfile) = ProfileSnapshot(
            name = p.name,
            university = p.university?.let { UniversitySnapshot.from(it) },
            quickSetupCompleted = p.quickSetupCompleted,
        )
    }
}

@Serializable
data class BudgetSnapshot(val min: Double? = null, val max: Double? = null, val currency: String? = null) {
    fun toDomain() = Budget(min = min, max = max, currency = currency)
    companion object { fun from(b: Budget) = BudgetSnapshot(b.min, b.max, b.currency) }
}

@Serializable
data class PreferencesSnapshot(
    val budget: BudgetSnapshot? = null,
    val maxDistanceKm: Double? = null,
    val essentialServices: List<String>? = null,
    val livingWith: String? = null,
    val placeType: String? = null,
    val pets: String? = null,
    val transport: String? = null,
    val propertyTypes: List<String>? = null,
    val currency: String? = null,
) {
    fun toDomain() = Preference(
        budget = budget?.toDomain(),
        maxDistanceKm = maxDistanceKm,
        essentialServices = essentialServices,
        livingWith = livingWith,
        placeType = placeType,
        pets = pets,
        transport = transport,
        propertyTypes = propertyTypes,
        currency = currency,
    )
    companion object {
        fun from(p: Preference) = PreferencesSnapshot(
            budget = p.budget?.let { BudgetSnapshot.from(it) },
            maxDistanceKm = p.maxDistanceKm,
            essentialServices = p.essentialServices,
            livingWith = p.livingWith,
            placeType = p.placeType,
            pets = p.pets,
            transport = p.transport,
            propertyTypes = p.propertyTypes,
            currency = p.currency,
        )
    }
}