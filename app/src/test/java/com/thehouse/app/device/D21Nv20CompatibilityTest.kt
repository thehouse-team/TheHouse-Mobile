package com.thehouse.app.device

import com.thehouse.app.data.notifications.mapper.toDomain
import com.thehouse.app.data.remote.dto.notifications.NotificationItemDto
import com.thehouse.app.data.remote.dto.notifications.NotificationTargetDto
import com.thehouse.app.domain.model.AppNotification
import com.thehouse.app.domain.model.NotificationTargetKind
import com.thehouse.app.domain.push.NotificationDestination
import com.thehouse.app.domain.push.NotificationDestinationResolver
import com.thehouse.app.domain.push.NotificationTargetResolver
import com.thehouse.app.domain.push.PushPayloadParser
import com.thehouse.app.presentation.notifications.notificationIcon
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * D21 adenda — NV-20 Notifications v1.3.0 (PR #103 Backend, §9.1).
 * Fixtures derivados de los literales contractuales, no de etiquetas españolas.
 */
class D21Nv20CompatibilityTest {

    private fun notification(
        type: String,
        kind: String?,
        id: String?,
        title: String = "Título del Backend",
        body: String = "Cuerpo del Backend",
    ): AppNotification = NotificationItemDto(
        id = "notif-1",
        type = type,
        category = "system",
        title = title,
        body = body,
        target = if (kind == null) null else NotificationTargetDto(kind = kind, id = id),
        count = 1,
        read = false,
    ).toDomain()

    private val newTypes = listOf(
        "agreement_proposed", "agreement_cancelled", "review_received",
        "review_hidden", "favorite_unavailable", "report_resolved",
    )

    @Test
    fun all_six_new_types_deserialize_into_generic_notifications() {
        newTypes.forEach { type ->
            val n = notification(type, "none", "informativo")
            assertEquals(type, n.type)
            assertNotNull(n.title)
            assertTrue(n.body.isNotBlank())
        }
    }

    @Test
    fun all_six_new_types_have_a_coherent_icon_and_unknown_is_generic() {
        newTypes.forEach { assertNotNull(notificationIcon(it)) }
        assertNotNull(notificationIcon("tipo_que_no_existe"))
    }

    @Test
    fun targetKind_room_with_valid_id_goes_to_room_detail() {
        val destination = NotificationDestinationResolver.from(
            "room", "6aef1234-1111-2222-3333-444455556666",
        )
        assertTrue(destination is NotificationDestination.Room)
        assertEquals(
            "6aef1234-1111-2222-3333-444455556666",
            (destination as NotificationDestination.Room).id,
        )
    }

    @Test
    fun targetKind_room_with_invalid_id_falls_back_to_notification_center() {
        listOf(null, "", "../admin", "http://evil.test/x", "id con espacios").forEach { id ->
            val destination = NotificationDestinationResolver.from("room", id)
            assertEquals(NotificationDestination.NotificationCenter, destination)
        }
    }

    @Test
    fun targetKind_none_opens_center_without_requiring_targetId() {
        assertEquals(
            NotificationDestination.NotificationCenter,
            NotificationDestinationResolver.from("none", null),
        )
        assertEquals(
            NotificationTargetKind.NONE,
            notification("report_resolved", "none", null).target?.kind,
        )
    }

    @Test
    fun unknown_targetKind_and_unknown_type_fall_back_to_center() {
        assertEquals(
            NotificationDestination.NotificationCenter,
            NotificationDestinationResolver.from("kind_futuro", "abc123"),
        )
        val unknownType = notification("tipo_futuro", "room", "abc123")
        assertEquals("tipo_futuro", unknownType.type)
        assertNotNull(unknownType.title)
    }

    @Test
    fun mixed_inbox_keeps_known_and_unknown_items_without_losing_valid_data() {
        val items = listOf(
            notification("review_received", "room", "room-123"),
            notification("tipo_futuro", "kind_futuro", "abc123"),
            notification("request_new", "request", "req-1", title = "Solicitud real"),
        )
        assertEquals(3, items.size)
        assertEquals("Solicitud real", items[2].title)
        assertEquals(
            NotificationDestination.Room("room-123"),
            NotificationDestinationResolver.fromTarget(items[0].target),
        )
        assertEquals(
            NotificationDestination.NotificationCenter,
            NotificationDestinationResolver.fromTarget(items[1].target),
        )
    }

    @Test
    fun previous_types_remain_supported() {
        val cases = listOf(
            "request" to NotificationDestination.Request("req-1"),
            "conversation" to NotificationDestination.Conversation("conv-1"),
            "visit" to NotificationDestination.Visit("visit-1"),
            "agreement" to NotificationDestination.Agreement,
            "rental" to NotificationDestination.Rental,
            "building" to NotificationDestination.Building("b-1"),
        )
        cases.forEach { (kind, expected) ->
            val id = when (expected) {
                is NotificationDestination.Conversation -> "conv-1"
                is NotificationDestination.Visit -> "visit-1"
                is NotificationDestination.Building -> "b-1"
                else -> "req-1"
            }
            assertEquals(expected, NotificationDestinationResolver.from(kind, id))
        }
    }

    @Test
    fun push_payload_none_and_room_are_centralized() {
        val none = NotificationTargetResolver.resolve("none", null)
        assertEquals(NotificationTargetKind.NONE, none?.kind)
        assertEquals(
            NotificationDestination.NotificationCenter,
            NotificationDestinationResolver.fromTarget(none),
        )

        val room = NotificationTargetResolver.resolve("room", "room-123")
        assertEquals(
            NotificationDestination.Room("room-123"),
            NotificationDestinationResolver.fromTarget(room),
        )

        val invalid = NotificationTargetResolver.resolve("room", "../admin")
        assertEquals(null, invalid)

        val parsed = PushPayloadParser.parse(
            mapOf(
                "notificationId" to "n-1",
                "title" to "Favorito no disponible",
                "body" to "Cuarto ya no disponible",
                "category" to "system",
                "targetKind" to "room",
                "targetId" to "room-123",
            ),
        )
        assertTrue(parsed is com.thehouse.app.domain.push.PushPayloadParseResult.Valid)
        assertEquals(
            NotificationDestination.Room("room-123"),
            NotificationDestinationResolver.fromTarget(
                (parsed as com.thehouse.app.domain.push.PushPayloadParseResult.Valid).payload.target,
            ),
        )
    }

    @Test
    fun push_payload_unknown_type_and_kind_do_not_crash() {
        val parsed = PushPayloadParser.parse(
            mapOf(
                "notificationId" to "n-2",
                "title" to "Notificación futura",
                "body" to "Contenido seguro",
                "category" to "system",
                "targetKind" to "kind_futuro",
                "targetId" to "abc123",
            ),
        )
        assertTrue(parsed is com.thehouse.app.domain.push.PushPayloadParseResult.Valid)
        assertEquals(
            NotificationDestination.NotificationCenter,
            NotificationDestinationResolver.fromTarget(
                (parsed as com.thehouse.app.domain.push.PushPayloadParseResult.Valid).payload.target,
            ),
        )
    }
}
