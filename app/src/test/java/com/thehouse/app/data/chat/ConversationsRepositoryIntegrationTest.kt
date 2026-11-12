package com.thehouse.app.data.chat

import com.thehouse.app.data.chat.repository.ConversationsRepositoryImpl
import com.thehouse.app.data.remote.api.ChatApi
import com.thehouse.app.data.serialization.JsonProvider
import com.thehouse.app.domain.error.ChatErrorCode
import com.thehouse.app.domain.error.ChatException
import com.thehouse.app.domain.error.DomainError
import com.thehouse.app.domain.model.ConversationStatus
import com.thehouse.app.domain.model.MessagesFetch
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class ConversationsRepositoryIntegrationTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: ConversationsRepositoryImpl

    private val conversationsJson = """
        {
          "items": [
            {
              "id": "conv-1",
              "requestId": "req-1",
              "room": {"id": "room-1", "title": "Cuarto"},
              "counterpart": {"name": "Arrendador", "role": "landlord"},
              "status": "open",
              "lastMessageAt": "2026-11-12T12:00:00+00:00",
              "unreadCount": 2
            }
          ],
          "page": 1,
          "pageSize": 20,
          "totalItems": 1,
          "totalPages": 1
        }
    """.trimIndent()

    private val messagesJson = """
        {
          "items": [
            {"id": "m-1", "senderRole": "student", "text": "Hola", "createdAt": "2026-11-12T12:00:00+00:00", "mine": true},
            {"id": "m-2", "senderRole": "landlord", "text": "Hola, ¿en qué te ayudo?", "createdAt": "2026-11-12T12:01:00+00:00", "mine": false}
          ],
          "hasMore": false,
          "pollAfterSeconds": 10
        }
    """.trimIndent()

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()
        val json = JsonProvider.create()
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        repository = ConversationsRepositoryImpl(
            api = retrofit.create(ChatApi::class.java),
            json = json,
        )
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun jsonResponse(code: Int, raw: String): MockResponse =
        MockResponse.Builder()
            .code(code)
            .addHeader("Content-Type", "application/json")
            .body(raw)
            .build()

    @Test
    fun `conversations list maps items`() = runTest {
        server.enqueue(jsonResponse(200, conversationsJson))
        val conversations = repository.getConversations(1, 20).getOrThrow()
        assertEquals(1, conversations.size)
        val conversation = conversations.first()
        assertEquals("conv-1", conversation.id)
        assertEquals("req-1", conversation.requestId)
        assertEquals("Arrendador", conversation.counterpart.name)
        assertEquals(ConversationStatus.OPEN, conversation.status)
        assertEquals(2, conversation.unreadCount)
    }

    @Test
    fun `messages 200 returns page with etag`() = runTest {
        server.enqueue(
            MockResponse.Builder()
                .code(200)
                .addHeader("Content-Type", "application/json")
                .addHeader("ETag", "\"v1\"")
                .body(messagesJson)
                .build(),
        )
        val fetch = repository.getMessages("conv-1", after = null, etag = null).getOrThrow()
        val modified = fetch as MessagesFetch.Modified
        assertEquals(2, modified.page.items.size)
        assertTrue(modified.page.items.first().mine)
        assertFalse(modified.page.items.last().mine)
        assertEquals(10L, modified.page.pollAfterSeconds)
        assertEquals("\"v1\"", modified.etag)
        val recorded = server.takeRequest()
        assertEquals("/api/v1/conversations/conv-1/messages", recorded.url.encodedPath)
        assertEquals("50", recorded.url.queryParameter("limit"))
        assertNull(recorded.url.queryParameter("after"))
        assertNull(recorded.headers["If-None-Match"])
    }

    @Test
    fun `poll sends after and If-None-Match`() = runTest {
        server.enqueue(jsonResponse(304, ""))
        repository.getMessages("conv-1", after = "m-2", etag = "\"v1\"").getOrThrow()
        val recorded = server.takeRequest()
        assertEquals("m-2", recorded.url.queryParameter("after"))
        assertEquals("\"v1\"", recorded.headers["If-None-Match"])
    }

    @Test
    fun `304 returns NotModified`() = runTest {
        server.enqueue(MockResponse.Builder().code(304).build())
        val fetch = repository.getMessages("conv-1", after = "m-2", etag = "\"v1\"").getOrThrow()
        assertTrue(fetch is MessagesFetch.NotModified)
    }

    @Test
    fun `send 201 returns message not replayed`() = runTest {
        server.enqueue(
            jsonResponse(
                201,
                """{"id":"m-3","senderRole":"student","text":"¿Cuánto es el depósito?","createdAt":"2026-11-12T12:02:00+00:00","mine":true}""",
            ),
        )
        val sent = repository.sendMessage("conv-1", "¿Cuánto es el depósito?", "uuid-1").getOrThrow()
        assertFalse(sent.replayed)
        assertEquals("m-3", sent.message.id)
        val recorded = server.takeRequest()
        val body = recorded.body!!.utf8()
        assertTrue(body.contains("clientMessageId"))
        assertTrue(body.contains("uuid-1"))
        assertEquals("POST", recorded.method)
    }

    @Test
    fun `send 200 replay marks replayed`() = runTest {
        server.enqueue(
            jsonResponse(
                200,
                """{"id":"m-3","senderRole":"student","text":"Hola","createdAt":"2026-11-12T12:02:00+00:00","mine":true}""",
            ),
        )
        val sent = repository.sendMessage("conv-1", "Hola", "uuid-1").getOrThrow()
        assertTrue(sent.replayed)
        assertEquals("m-3", sent.message.id)
    }

    @Test
    fun `send 409 conversation_closed exposes code`() = runTest {
        server.enqueue(jsonResponse(409, """{"status":409,"code":"conversation_closed"}"""))
        val failure = repository.sendMessage("conv-1", "Hola", "uuid-1")
            .exceptionOrNull() as ChatException
        assertEquals(DomainError.Conflict, failure.domainError)
        assertEquals(ChatErrorCode.CONVERSATION_CLOSED, failure.code)
    }

    @Test
    fun `read 204 returns success`() = runTest {
        server.enqueue(MockResponse.Builder().code(204).build())
        val result = repository.markRead("conv-1", "m-2")
        assertTrue(result.isSuccess)
        val recorded = server.takeRequest()
        assertEquals("/api/v1/conversations/conv-1/read", recorded.url.encodedPath)
        assertTrue(recorded.body!!.utf8().contains("m-2"))
    }

    @Test
    fun `network failure maps to network error`() = runTest {
        server.close()
        val failure = repository.getConversations(1, 20).exceptionOrNull() as ChatException
        assertEquals(DomainError.Network, failure.domainError)
    }
}
