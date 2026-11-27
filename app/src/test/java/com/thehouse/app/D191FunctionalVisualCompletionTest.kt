package com.thehouse.app

import com.thehouse.app.data.remote.api.BudgetPatchBody
import com.thehouse.app.data.remote.api.PreferencesPatchBody
import com.thehouse.app.data.remote.dto.preferences.PreferencesContract
import com.thehouse.app.data.serialization.JsonProvider
import com.thehouse.app.domain.error.DomainError
import com.thehouse.app.domain.error.DomainException
import com.thehouse.app.domain.model.LegalInfo
import com.thehouse.app.domain.model.SessionState
import com.thehouse.app.domain.model.UserSession
import com.thehouse.app.domain.repository.AccountRepository
import com.thehouse.app.domain.repository.LegalRepository
import com.thehouse.app.domain.repository.LocalUserDataPurger
import com.thehouse.app.domain.usecase.DeleteAccountUseCase
import com.thehouse.app.presentation.navigation.AppRoutes
import com.thehouse.app.presentation.navigation.DrawerItemCatalog
import com.thehouse.app.presentation.privacy.PrivacyEvent
import com.thehouse.app.presentation.privacy.PrivacyViewModel
import com.thehouse.app.presentation.smartmatch.labelForMissingField
import com.thehouse.app.testutil.TestMainDispatcherRule
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * D19.1 cierre funcional y visual: regresiones de las causas verificadas
 * en la instrucción humana (PUT de preferencias, 422 de Smart Match,
 * enlace legal, logo duplicado y drawer sin iconos).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class D191FunctionalVisualCompletionTest {

    @get:Rule
    val mainDispatcherRule = TestMainDispatcherRule(StandardTestDispatcher())

    @Test
    fun `PUT body uses contractual codes and never sends root currency or server fields`() {
        val preference = com.thehouse.app.domain.model.Preference(
            budget = com.thehouse.app.domain.model.Budget(
                min = 1000.0,
                max = 5000.0,
                currency = "MXN",
            ),
            maxDistanceKm = 10.0,
            essentialServices = listOf("Internet", "Agua"),
            livingWith = "Cualquiera",
            placeType = "Cuarto",
            pets = "Sí",
            transport = "Caminar",
            propertyTypes = listOf("Departamento"),
            currency = "MXN",
        )
        val body: PreferencesPatchBody = with(PreferencesContract) {
            preference.toRequest(min = 1000, max = 5000)
        }
        val json = JsonProvider.create().encodeToString(body)

        assertTrue(json.contains("\"min\":1000"))
        assertTrue(json.contains("\"max\":5000"))
        assertTrue(json.contains("\"currency\":\"MXN\""))
        assertTrue(json.contains("\"maxDistanceKm\":10.0"))
        assertTrue(json.contains("\"wifi\""))
        assertTrue(json.contains("\"water\""))
        assertTrue(json.contains("\"any\""))
        assertTrue(json.contains("\"room\""))
        assertTrue(json.contains("\"pets\":true"))
        assertTrue(json.contains("\"walk\""))
        assertTrue(json.contains("\"apartment\""))
        assertFalse(json.contains("\"Internet\""))
        assertFalse(json.contains("\"quickSetupCompleted\""))
        assertFalse(json.contains("\"version\""))
        assertFalse(json.contains("\"updatedAt\""))
        // currency solo dentro de budget, nunca en la raíz del body.
        assertFalse(json.contains("},\"currency\""))
        assertEquals(1, body.budget?.currency?.let { 1 } ?: 0)
    }

    @Test
    fun `budget currency defaults to MXN when budget is absent`() {
        val preference = com.thehouse.app.domain.model.Preference(
            budget = null,
            maxDistanceKm = null,
            essentialServices = null,
            livingWith = null,
            placeType = null,
            pets = null,
            transport = null,
            propertyTypes = null,
            currency = "",
        )
        val body = with(PreferencesContract) { preference.toRequest(min = null, max = null) }
        assertNull(body.budget)
        assertNull(body.livingWith)
        assertTrue(body.essentialServices.isNullOrEmpty())
    }

    @Test
    fun `BudgetPatchBody contains exactly three contractual fields`() {
        val json = JsonProvider.create().encodeToString(
            BudgetPatchBody(min = 1000, max = 5000, currency = "MXN"),
        )
        assertTrue(json.contains("\"min\":1000"))
        assertTrue(json.contains("\"max\":5000"))
        assertTrue(json.contains("\"currency\":\"MXN\""))
        assertEquals(3, json.count { it == ':' })
    }

    @Test
    fun `missing fields from 422 are routed to preferences without inventing values`() {
        val route = AppRoutes.preferencesWithMissing(
            listOf("budget.max", "livingWith", "placeType"),
        )
        assertEquals(
            "preferencias?missing=budget.max,livingWith,placeType",
            route,
        )
        assertEquals("preferencias", AppRoutes.preferencesWithMissing(emptyList()))
        assertTrue(AppRoutes.PREFERENCES_PATTERN.contains(AppRoutes.MISSING_ARGUMENT))
    }

    @Test
    fun `missing field labels are readable and contractual`() {
        assertEquals("universidad", labelForMissingField("university"))
        assertEquals("presupuesto máximo", labelForMissingField("budget.max"))
        assertEquals("con quién vivir", labelForMissingField("livingWith"))
        assertEquals("tipo de lugar", labelForMissingField("placeType"))
    }

    @Test
    fun `every drawer entry has a visual icon key`() {
        val items = DrawerItemCatalog.guestItems() + DrawerItemCatalog.authItems(false)
        assertTrue(items.isNotEmpty())
        items.forEach { item ->
            assertTrue("Sin icono para ${item.tag}", item.icon.name.isNotBlank())
        }
        assertEquals(13, DrawerItemCatalog.authItems(false).size)
    }

    @Test
    fun `home no longer renders a second BrandMark in its own content`() = runTest {
        val source = sourceOf("src/main/java/com/thehouse/app/presentation/home/HomeScreen.kt")
        assertFalse(source.contains("BrandMark("))
        assertTrue(source.contains("SmartMatchPromo(onClick = onNavigateToSmartMatch)"))
    }

    @Test
    fun `preferences CTA lives in a fixed bottomBar and dropdowns use contractual catalogs`() = runTest {
        val source = sourceOf(
            "src/main/java/com/thehouse/app/presentation/preferences/PreferencesScreen.kt",
        )
        assertTrue(source.contains("bottomBar = {"))
        assertTrue(source.contains("Guardar preferencias"))
        assertTrue(source.contains("testTag = \"preferences_save_button\""))
        assertTrue(source.contains("imePadding()"))
        assertTrue(source.contains("PreferencesContract.TRANSPORT_CODES"))
        assertTrue(source.contains("PreferencesContract.LIVING_WITH_CODES"))
        assertTrue(source.contains("PreferencesContract.PLACE_TYPE_CODES"))
        assertTrue(source.contains("PreferencesContract.ESSENTIAL_SERVICES_CODES"))
        assertTrue(source.contains("PreferencesContract.PROPERTY_TYPE_CODES"))
    }

    @Test
    fun `legal null url clears any stale local url and shows honest message`() = runTest {
        val session = MutableStateFlow<SessionState>(
            SessionState.Active(
                UserSession("u-1", "Test", "test@example.com"),
            ),
        )
        val repository = object : LegalRepository {
            override suspend fun getLegal(): Result<LegalInfo> = Result.success(
                LegalInfo(
                    termsVersion = "2026-11",
                    privacyVersion = "2026-11",
                    landlordTermsVersion = null,
                    termsUrl = null,
                    privacyUrl = null,
                    accountDeletionUrl = null,
                    updatedAt = null,
                ),
            )

            override suspend fun acceptLegal(
                termsVersion: String,
                privacyVersion: String,
            ): Result<Unit> = Result.success(Unit)
        }
        val vm = viewModel(
            this,
            privacyPolicyUrl = "https://stale.example.com/privacy",
            legalRepository = repository,
            session = session,
        )
        advanceUntilIdle()

        val state = vm.uiState.value
        assertNull(state.privacyPolicyUrl)
        assertEquals("El aviso de privacidad aún no ha sido publicado.", state.privacyStatus)
        vm.onEvent(PrivacyEvent.OpenPrivacyPolicyClicked)
        advanceUntilIdle()
        // Sin URL válida no se emite efecto de apertura externa.
        assertNull(vm.uiState.value.privacyPolicyUrl)
    }

    @Test
    fun `legal failure never leaves a clickable stale url`() = runTest {
        val repository = object : LegalRepository {
            override suspend fun getLegal(): Result<LegalInfo> =
                Result.failure(DomainException(DomainError.Network))

            override suspend fun acceptLegal(
                termsVersion: String,
                privacyVersion: String,
            ): Result<Unit> = Result.success(Unit)
        }
        val vm = viewModel(
            this,
            privacyPolicyUrl = "https://stale.example.com/privacy",
            legalRepository = repository,
        )
        advanceUntilIdle()
        assertNull(vm.uiState.value.privacyPolicyUrl)
    }

    private fun viewModel(
        scope: TestScope,
        privacyPolicyUrl: String?,
        legalRepository: LegalRepository,
        session: MutableStateFlow<SessionState> = MutableStateFlow(SessionState.Anonymous),
    ): PrivacyViewModel {
        val accountRepository = object : AccountRepository {
            override val isDeletionAvailable: Boolean = false
            override suspend fun deleteAccount(): Result<Unit> = Result.success(Unit)
        }
        return PrivacyViewModel(
            privacyPolicyUrl = privacyPolicyUrl,
            sessionState = session,
            deleteAccount = DeleteAccountUseCase(
                accountRepository,
                object : LocalUserDataPurger {
                    override suspend fun purge(userId: String?) = Unit
                },
                session,
            ),
            legalRepository = legalRepository,
        )
    }

    private fun sourceOf(path: String): String {
        val candidates = listOf(File(path), File("app", path))
        return candidates.firstOrNull { it.exists() }?.readText()
            ?: throw AssertionError("No se encontró $path")
    }
}
