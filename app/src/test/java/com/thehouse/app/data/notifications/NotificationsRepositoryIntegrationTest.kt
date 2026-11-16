package com.thehouse.app.data.notifications

import com.thehouse.app.data.notifications.repository.NotificationsRepositoryImpl
import com.thehouse.app.data.remote.api.NotificationsApi
import com.thehouse.app.data.serialization.JsonProvider
import com.thehouse.app.domain.error.DomainError
import com.thehouse.app.domain.error.NotificationException
import com.thehouse.app.domain.model.NotificationCategory
import com.thehouse.app.domain.model.NotificationTargetKind
import com.thehouse.app.domain.model.UnreadCountFetch
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class NotificationsRepositoryIntegrationTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: NotificationsRepositoryImpl

    private val listJson = """
        {
          "items": [
            {
              "id": "n-1",
              "type": "request_update",
              "category": "requests",
              "title": "Solicitud actualizada",
              "body": "Tu solicitud cambió de estado",
              "target": {"kind": "request", "id": "req-1"},
              "count": 3,
              "read": false,
              "createdAt": "2026-11-12T12:00:00Z",
              "updatedAt": "2026-11-12T12:05:00Z"
            },
            {
              "id": "n-2",
              "type": "chat_message",
              "category": "requests",
              "title": "Nuevos mensajes",
              "body": "Tienes mensajes nuevos",
              "target": {"kind": "conversation", "id": "conv-1"},
              "count": 2,
              "read": true,
              "createdAt": "2026-11-12T11:00:00Z",
              "updatedAt": "2026-11-12T11:05:00Z"
            }
          ],
          "page": 1,
          "pageSize": 20,
          "totalItems": 2,
          "totalPages": 1
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
        repository = NotificationsRepositoryImpl(
            api = retrofit.create(NotificationsApi::class.java),
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
    fun `list maps items filters and pagination`() = runTest {
        server.enqueue(jsonResponse(200, listJson))

        val page = repository.getNotifications(
            unread = true,
            category = NotificationCategory.REQUESTS,
            page = 2,
            pageSize = 10,
        ).getOrThrow()

        assertEquals(2, page.items.size)
        assertEquals(1, page.page)
        assertEquals(20, page.pageSize)
        assertEquals(2, page.totalItems)
        assertEquals(1, page.totalPages)

        val first = page.items.first()
        assertEquals("n-1", first.id)
        assertEquals(NotificationCategory.REQUESTS, first.category)
        assertEquals(NotificationTargetKind.REQUEST, first.target?.kind)
        assertEquals("req-1", first.target?.id)
        assertEquals(3, first.count) // count agrupado
        assertEquals(false, first.read)

        val recorded = server.takeRequest()
        assertEquals("/api/v1/users/me/notifications", recorded.url.encodedPath)
        assertEquals("true", recorded.url.queryParameter("unread"))
        assertEquals("requests", recorded.url.queryParameter("category"))
        assertEquals("2", recorded.url.queryParameter("page"))
        assertEquals("10", recorded.url.queryParameter("pageSize"))
    }

    @Test
    fun `unread count 200 returns count and etag`() = runTest {
        server.enqueue(
            MockResponse.Builder()
                .code(200)
                .addHeader("Content-Type", "application/json")
                .addHeader("ETag", "\"etag-1\"")
                .body("""{"unread":5,"requests":2,"visits":1,"system":2}""")
                .build(),
        )

        val fetch = repository.getUnreadCount(etag = null).getOrThrow() as UnreadCountFetch.Modified
        assertEquals(5, fetch.count.unread)
        assertEquals(2, fetch.count.requests)
        assertEquals(1, fetch.count.visits)
        assertEquals(2, fetch.count.system)
        assertEquals("\"etag-1\"", fetch.etag)

        val recorded = server.takeRequest()
        assertEquals("/api/v1/users/me/notifications/unread-count", recorded.url.encodedPath)
        assertNull(recorded.headers["If-None-Match"])
    }

    @Test
    fun `unread count sends If-None-Match`() = runTest {
        server.enqueue(MockResponse.Builder().code(304).build())
        repository.getUnreadCount(etag = "\"etag-1\"").getOrThrow()

        val recorded = server.takeRequest()
        assertEquals("\"etag-1\"", recorded.headers["If-None-Match"])
    }

    @Test
    fun `unread count 304 returns NotModified`() = runTest {
        server.enqueue(MockResponse.Builder().code(304).build())
        val fetch = repository.getUnreadCount(etag = "\"etag-1\"").getOrThrow()
        assertTrue(fetch is UnreadCountFetch.NotModified)
    }

    @Test
    fun `mark read 204 succeeds`() = runTest {
        server.enqueue(MockResponse.Builder().code(204).build())
        val result = repository.markRead("n-1")
        assertTrue(result.isSuccess)
        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/api/v1/users/me/notifications/n-1/read", recorded.url.encodedPath)
    }

    @Test
    fun `mark read 404 maps to not found`() = runTest {
        server.enqueue(jsonResponse(404, """{"status":404}"""))
        val failure = repository.markRead("n-missing").exceptionOrNull() as NotificationException
        assertEquals(DomainError.NotFound, failure.domainError)
    }

    @Test
    fun `mark all read without category sends empty optional body`() = runTest {
        server.enqueue(MockResponse.Builder().code(204).build())
        val result = repository.markAllRead(null)
        assertTrue(result.isSuccess)
        val recorded = server.takeRequest()
        assertEquals("/api/v1/users/me/notifications/read-all", recorded.url.encodedPath)
        val body = recorded.body!!.utf8()
        // body opcional: se envía objeto con category null permitido por contrato
        assertTrue(body.contains("{") || body.isBlank())
    }

    @Test
    fun `mark all read with category sends category body`() = runTest {
        server.enqueue(MockResponse.Builder().code(204).build())
        val result = repository.markAllRead(NotificationCategory.VISITS)
        assertTrue(result.isSuccess)
        val recorded = server.takeRequest()
        assertEquals("/api/v1/users/me/notifications/read-all", recorded.url.encodedPath)
        val body = recorded.body!!.utf8()
        assertTrue(body.contains("visits"))
    }
}
