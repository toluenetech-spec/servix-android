package ng.name.servix.app.chat

import ng.name.servix.app.data.model.ChatMessageDto
import ng.name.servix.app.data.realtime.ChatEvent
import ng.name.servix.app.data.realtime.ChatEventParser
import ng.name.servix.app.ui.chat.ChatMerge
import ng.name.servix.app.ui.chat.ChatRow
import ng.name.servix.app.ui.chat.ChatTicks
import ng.name.servix.app.ui.chat.ChatTimeline
import ng.name.servix.app.ui.chat.TickState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Release 1.1 chat logic. Pure functions only — no Android, no network (Android Testing Skills:
 * "assert the render contract, not the framework").
 */
class ChatLogicTest {

    private val me = "user-me"
    private val them = "user-them"

    private fun msg(
        id: String = "m1",
        body: String = "hello",
        senderId: String = them,
        createdAt: String = "2026-11-07T09:00:00.000Z",
        clientId: String? = null,
        kind: String = "text",
    ) = ChatMessageDto(
        id = id,
        body = body,
        senderId = senderId,
        createdAt = createdAt,
        clientId = clientId,
        kind = kind,
    )

    // ---------------------------------------------------------------- parser

    @Test
    fun `parser ignores SSE comments and blank lines`() {
        assertNull(ChatEventParser.parse(": ping"))
        assertNull(ChatEventParser.parse(""))
        assertNull(ChatEventParser.parse("   "))
    }

    @Test
    fun `parser reads the type out of a single data frame`() {
        val event = ChatEventParser.parse("""data: {"type":"typing","threadId":"t1","userId":"u2","typing":true}""")
        assertTrue(event is ChatEvent.Typing)
        event as ChatEvent.Typing
        assertEquals("t1", event.threadId)
        assertEquals("u2", event.userId)
        assertTrue(event.typing)
    }

    @Test
    fun `parser keeps a message payload intact`() {
        val event = ChatEventParser.parse(
            """data: {"type":"message","threadId":"t1","message":{"id":"m9","body":"hi there","senderId":"u2","createdAt":"2026-11-07T09:00:00.000Z","clientId":"c9","kind":"text"}}"""
        )
        assertTrue(event is ChatEvent.Message)
        event as ChatEvent.Message
        assertEquals("t1", event.threadId)
        assertEquals("m9", event.message.id)
        assertEquals("hi there", event.message.body)
    }

    @Test
    fun `unknown types do not crash the stream`() {
        val event = ChatEventParser.parse("""data: {"type":"reaction","threadId":"t1"}""")
        assertTrue(event is ChatEvent.Unknown)
        assertEquals("reaction", (event as ChatEvent.Unknown).type)
    }

    @Test
    fun `malformed json is dropped rather than thrown`() {
        assertNull(ChatEventParser.parse("""data: {"type":"message","""))

        assertNull(ChatEventParser.parse("data: not-json"))
    }

    // ---------------------------------------------------------------- ticks

    @Test
    fun `my message shows read once the other side read it`() {
        val message = msg(createdAt = "2026-11-07T09:00:00.000Z")
        assertEquals(
            TickState.READ,
            ChatTicks.state(message, me, otherReadAt = "2026-11-07T09:00:05.000Z", otherDeliveredAt = null),
        )
    }

    @Test
    fun `delivered without read is one tick further than sent`() {
        val message = msg(createdAt = "2026-11-07T09:00:00.000Z")
        assertEquals(
            TickState.DELIVERED,
            ChatTicks.state(message, me, otherReadAt = null, otherDeliveredAt = "2026-11-07T09:00:05.000Z"),
        )
    }

    @Test
    fun `an older read stamp does not mark a newer message as read`() {
        val message = msg(createdAt = "2026-11-07T09:10:00.000Z")
        assertEquals(
            TickState.SENT,
            ChatTicks.state(message, me, otherReadAt = "2026-11-07T09:00:00.000Z", otherDeliveredAt = null),
        )
    }

    @Test
    fun `their message never carries a tick`() {
        val message = msg(senderId = them)
        assertNull(ChatTicks.state(message, me, otherReadAt = null, otherDeliveredAt = null))
    }

    @Test
    fun `an optimistic row is pending`() {
        val message = msg(id = "", senderId = me, clientId = "c1")
        assertEquals(TickState.PENDING, ChatTicks.state(message, me, otherReadAt = null, otherDeliveredAt = null))
    }

    // ---------------------------------------------------------------- merge

    @Test
    fun `server row replaces the optimistic row with the same clientId`() {
        val optimistic = msg(id = "", body = "hello", senderId = me, clientId = "c1")
        val server = msg(id = "m-real", body = "hello", senderId = me, clientId = "c1")
        val merged = ChatMerge.upsert(listOf(optimistic), server)
        assertEquals(1, merged.size)
        assertEquals("m-real", merged.first().id)
    }

    @Test
    fun `an update for a known id replaces in place and keeps order`() {
        val first = msg(id = "a", createdAt = "2026-11-07T09:00:00.000Z")
        val second = msg(id = "b", createdAt = "2026-11-07T09:01:00.000Z")
        val edited = second.copy(body = "edited", deletedAt = null)
        val merged = ChatMerge.upsert(listOf(first, second), edited)
        assertEquals(listOf("a", "b"), merged.map { it.id })
        assertEquals("edited", merged[1].body)
    }

    @Test
    fun `a brand new message is appended once even if delivered twice`() {
        val base = listOf(msg(id = "a"))
        val incoming = msg(id = "b", createdAt = "2026-11-07T09:05:00.000Z")
        val once = ChatMerge.upsert(base, incoming)
        val twice = ChatMerge.upsert(once, incoming)
        assertEquals(2, once.size)
        assertEquals(2, twice.size)
    }

    @Test
    fun `upsertAll merges a page without duplicating history`() {
        val page = listOf(msg(id = "a"), msg(id = "b"))
        val merged = ChatMerge.upsertAll(listOf(msg(id = "a")), page)
        assertEquals(listOf("a", "b"), merged.map { it.id })
    }
}
