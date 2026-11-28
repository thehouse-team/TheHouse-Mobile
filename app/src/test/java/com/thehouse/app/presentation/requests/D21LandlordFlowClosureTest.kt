package com.thehouse.app.presentation.requests

import com.thehouse.app.data.remote.api.RequestsApi
import com.thehouse.app.data.requests.repository.RequestsRepositoryImpl
import com.thehouse.app.data.serialization.JsonProvider
import com.thehouse.app.domain.error.DomainError
import com.thehouse.app.domain.error.RequestErrorCode
import com.thehouse.app.domain.error.RequestException
import com.thehouse.app.domain.model.PagedResult
import com.thehouse.app.domain.model.QuickQuestion
import com.thehouse.app.domain.model.RequestLandlord
import com.thehouse.app.domain.model.RequestRoom
import com.thehouse.app.domain.model.RequestStatus
import com.thehouse.app.domain.model.RoomRequest
import com.thehouse.app.domain.repository.RequestsRepository
import com.thehouse.app.domain.usecase.CancelRequestUseCase
import com.thehouse.app.domain.usecase.CreateRequestUseCase
import com.thehouse.app.domain.usecase.GetRequestUseCase
import com.thehouse.app.testutil.TestMainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

private val closureRequest = RoomRequest(
    id = "req-1",
    status = RequestStatus.PENDING,
    room = RequestRoom("room-1", "b-1", "Cuarto", "Tulancingo"),
    quickQuestions = listOf(QuickQuestion.AVAILABLE_NOW, QuickQuestion.PETS_ALLOWED),
    message = "Hola",
    student = null,
    landlord = RequestLandlord("Arrendador"),
    visit = null,
    conversationId = "conv-real-1",
    rejection = null,
    roomAvailable = true,
    createdAt = null,
    updatedAt = null,
)

private class ClosureRepository(
    private val result: Result<RoomRequest> = Result.success(closureRequest),
) : RequestsRepository {
    var roomId: String? = null
    var questions: List<QuickQuestion> = emptyList()
    var message: String? = null
    var calls: Int = 0

    override suspend fun createRequest(
        roomId: String,
        quickQuestions: List<QuickQuestion>,
        message: String?,
    ): Result<RoomRequest> {
        calls++
        this.roomId = roomId
        this.questions = quickQuestions
        this.message = message
        return result
    }

    override suspend fun getRequests(status: RequestStatus?, page: Int, pageSize: Int) =
        Result.success(PagedResult(listOf(closureRequest), 1, 20, 1, 1))

    override suspend fun getRequest(id: String) = Result.success(closureRequest)
    override suspend fun cancelRequest(id: String) = Result.success(closureRequest)
    override suspend fun getLandlordRequests(
        status: RequestStatus?,
        roomId: String?,
        page: Int,
        pageSize: Int,
    ) = Result.success(PagedResult(listOf(closureRequest), 1, 20, 1, 1))

    override suspend fun getLandlordRequest(id: String) = Result.success(closureRequest)
    override suspend fun acceptRequest(id: String) = Result.success(closureRequest)
    override suspend fun rejectRequest(id: String, reason: String, note: String?) =
        Result.success(closureRequest)
}

@OptIn(ExperimentalCoroutinesApi::class)
class D21LandlordFlowClosureTest {

    @get:Rule
    val mainDispatcherRule = TestMainDispatcherRule(StandardTestDispatcher())

    // --- Etiquetas compartidas: el detalle ya no muestra códigos API ---

    @Test
    fun `quick question labels are Spanish for all seven codes`() {
        assertEquals("¿Disponible ahora?", QuickQuestionLabels.label(QuickQuestion.AVAILABLE_NOW))
        assertEquals("¿Qué servicios están incluidos?", QuickQuestionLabels.label(QuickQuestion.SERVICES_INCLUDED))
        assertEquals("¿Cuánto es el depósito?", QuickQuestionLabels.label(QuickQuestion.DEPOSIT))
        assertEquals("¿Cuál es el contrato mínimo?", QuickQuestionLabels.label(QuickQuestion.MINIMUM_CONTRACT))
        assertEquals("¿Se pueden hacer visitas?", QuickQuestionLabels.label(QuickQuestion.VISIT_POSSIBLE))
        assertEquals("¿Se aceptan mascotas?", QuickQuestionLabels.label(QuickQuestion.PETS_ALLOWED))
        assertEquals("¿Hay roommates?", QuickQuestionLabels.label(QuickQuestion.ROOMMATES))
        assertEquals(7, QuickQuestionLabels.all().size)
    }

    @Test
    fun `composer and detail use the same label catalog`() {
        // El composer itera QuickQuestionLabels.all(); el detalle usa label().
        val codes = QuickQuestionLabels.all().map { it.first.api }
        assertEquals(
            listOf(
                "available_now", "services_included", "deposit", "minimum_contract",
                "visit_possible", "pets_allowed", "roommates",
            ),
            codes,
        )
    }

    // --- Selección y máximo contractual ---

    @Test
    fun `selection toggles on and off without losing message`() = runTest {
        val repository = ClosureRepository()
        val vm = CreateRequestViewModel("room-1", CreateRequestUseCase(repository))
        vm.onEvent(CreateRequestEvent.MessageChanged("Consulta"))
        vm.onEvent(CreateRequestEvent.QuickQuestionToggled(QuickQuestion.DEPOSIT))
        assertTrue(QuickQuestion.DEPOSIT in vm.uiState.value.quickQuestions)
        vm.onEvent(CreateRequestEvent.QuickQuestionToggled(QuickQuestion.DEPOSIT))
        assertFalse(QuickQuestion.DEPOSIT in vm.uiState.value.quickQuestions)
        assertEquals("Consulta", vm.uiState.value.message)
    }

    @Test
    fun `sixth quick question is rejected but deselect remains available`() = runTest {
        val repository = ClosureRepository()
        val vm = CreateRequestViewModel("room-1", CreateRequestUseCase(repository))
        QuickQuestion.entries.take(6).forEach {
            vm.onEvent(CreateRequestEvent.QuickQuestionToggled(it))
        }
        assertEquals(5, vm.uiState.value.quickQuestions.size)
        // Deseleccionar siempre funciona aunque estemos en el máximo.
        vm.onEvent(CreateRequestEvent.QuickQuestionToggled(QuickQuestion.entries.first()))
        assertEquals(4, vm.uiState.value.quickQuestions.size)
    }

    @Test
    fun `successful request sends room id questions message and keeps form on failure`() = runTest {
        val repository = ClosureRepository()
        val vm = CreateRequestViewModel("room-abc", CreateRequestUseCase(repository))
        vm.onEvent(CreateRequestEvent.QuickQuestionToggled(QuickQuestion.AVAILABLE_NOW))
        vm.onEvent(CreateRequestEvent.MessageChanged("Hola arrendador"))
        vm.onEvent(CreateRequestEvent.Send)
        runCurrent()
        assertEquals("room-abc", repository.roomId)
        assertEquals(listOf(QuickQuestion.AVAILABLE_NOW), repository.questions)
        assertEquals("Hola arrendador", repository.message)
        assertFalse(vm.uiState.value.isSending)
    }

    @Test
    fun `moderation failure keeps the form and resets sending`() = runTest {
        val repository = ClosureRepository(
            Result.failure(
                RequestException(DomainError.ValidationError, RequestErrorCode.CONTENT_REJECTED),
            ),
        )
        val vm = CreateRequestViewModel("room-1", CreateRequestUseCase(repository))
        vm.onEvent(CreateRequestEvent.QuickQuestionToggled(QuickQuestion.DEPOSIT))
        vm.onEvent(CreateRequestEvent.MessageChanged("Texto a revisar"))
        vm.onEvent(CreateRequestEvent.Send)
        runCurrent()
        assertEquals(RequestErrorCode.CONTENT_REJECTED, vm.uiState.value.errorCode)
        assertEquals(
            "El mensaje no cumple las reglas de la comunidad. Revísalo y vuelve a intentarlo.",
            vm.uiState.value.errorMessage,
        )
        assertFalse(vm.uiState.value.isSending)
        // El formulario se conserva para corregir y reenviar.
        assertEquals("Texto a revisar", vm.uiState.value.message)
        assertTrue(QuickQuestion.DEPOSIT in vm.uiState.value.quickQuestions)
    }

    @Test
    fun `moderation unavailable maps to friendly retry message`() = runTest {
        val repository = ClosureRepository(
            Result.failure(
                RequestException(DomainError.ServiceUnavailable, RequestErrorCode.MODERATION_UNAVAILABLE, 30),
            ),
        )
        val vm = CreateRequestViewModel("room-1", CreateRequestUseCase(repository))
        vm.onEvent(CreateRequestEvent.QuickQuestionToggled(QuickQuestion.DEPOSIT))
        vm.onEvent(CreateRequestEvent.Send)
        runCurrent()
        assertEquals(
            "No pudimos revisar el mensaje en este momento. Intenta de nuevo en unos segundos.",
            vm.uiState.value.errorMessage,
        )
    }

    // --- Errores de moderación del contrato HTTP real ---

    @Test
    fun `repository maps content_rejected 422 and moderation_unavailable 503`() = runTest {
        val server = MockWebServer()
        server.start()
        val json = JsonProvider.create()
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        val repository = RequestsRepositoryImpl(retrofit.create(RequestsApi::class.java), json)
        try {
            server.enqueue(
                MockResponse.Builder().code(422)
                    .addHeader("Content-Type", "application/json")
                    .body("""{"status":422,"code":"content_rejected","field":"message"}""")
                    .build(),
            )
            val rejected = repository.createRequest("room-1", listOf(QuickQuestion.DEPOSIT), "x")
                .exceptionOrNull() as RequestException
            assertEquals(RequestErrorCode.CONTENT_REJECTED, rejected.code)
            assertEquals(DomainError.ValidationError, rejected.domainError)

            server.enqueue(
                MockResponse.Builder().code(503)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Retry-After", "30")
                    .body("""{"status":503,"code":"moderation_unavailable"}""")
                    .build(),
            )
            val unavailable = repository.createRequest("room-1", listOf(QuickQuestion.DEPOSIT), "x")
                .exceptionOrNull() as RequestException
            assertEquals(RequestErrorCode.MODERATION_UNAVAILABLE, unavailable.code)
            assertEquals(DomainError.ServiceUnavailable, unavailable.domainError)
            assertEquals(30L, unavailable.retryAfterSeconds)
        } finally {
            server.close()
        }
    }

    @Test
    fun `HTTP payload sends real codes and never Spanish labels`() = runTest {
        val server = MockWebServer()
        server.start()
        val json = JsonProvider.create()
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        val repository = RequestsRepositoryImpl(retrofit.create(RequestsApi::class.java), json)
        try {
            server.enqueue(
                MockResponse.Builder().code(201).addHeader("Content-Type", "application/json")
                    .body(
                        """{"id":"req-1","status":"pending","room":{"id":"room-1","buildingId":"b-1","title":"Cuarto","city":"Tulancingo"},"quickQuestions":["available_now"],"message":null,"landlord":{"name":"A"},"conversationId":"conv-1","roomAvailable":true,"createdAt":"2026-01-01T00:00:00Z","updatedAt":"2026-01-01T00:00:00Z"}""",
                    ).build(),
            )
            repository.createRequest(
                "room-1",
                listOf(QuickQuestion.AVAILABLE_NOW, QuickQuestion.PETS_ALLOWED),
                "Hola",
            ).getOrThrow()
            val recorded = server.takeRequest()
            assertEquals("/api/v1/rooms/room-1/requests", recorded.url.encodedPath)
            val body = recorded.body!!.utf8()
            assertTrue(body.contains("available_now"))
            assertTrue(body.contains("pets_allowed"))
            assertTrue(body.contains("Hola"))
            assertFalse(body.contains("¿Disponible ahora?"))
        } finally {
            server.close()
        }
    }

    // --- Navegación a conversación con conversationId real ---

    @Test
    fun `opening conversation emits real id only when backend provided it`() = runTest {
        val repository = ClosureRepository()
        val vm = com.thehouse.app.presentation.requests.RequestDetailViewModel(
            requestId = "req-1",
            getRequest = GetRequestUseCase(repository),
            cancelRequest = CancelRequestUseCase(repository),
        )
        runCurrent()
        var opened: String? = null
        val job = launch {
            vm.effects.collect { effect ->
                if (effect is com.thehouse.app.presentation.requests.RequestDetailEffect.OpenConversation) {
                    opened = effect.conversationId
                }
            }
        }
        vm.onEvent(com.thehouse.app.presentation.requests.RequestDetailEvent.OpenConversation)
        runCurrent()
        assertEquals("conv-real-1", opened)
        job.cancel()
    }
}
