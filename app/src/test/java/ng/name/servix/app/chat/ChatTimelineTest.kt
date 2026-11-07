package ng.name.servix.app.chat

import ng.name.servix.app.data.model.ChatMessageDto
import ng.name.servix.app.ui.chat.ChatRow
import ng.name.servix.app.ui.chat.ChatTimeline
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Day separators and the 3-minute run grouping that decides which bubble draws a tail. */
class ChatTimelineTest {

    private val me = "user-me"
    private val them = "user-them"

    private fun msg(id: String, senderId: String, createdAt: String) = ChatMessageDto(
        id = id,
        body = "b-$id",
        senderId = senderId,
        createdAt = createdAt,
    )

    private val today = LocalDate.of(2026, 11, 7)

    @Test
    fun `two days produce two separators in order`() {
        val rows = ChatTimeline.build(
            listOf(
                msg("a", them, "2026-11-06T20:00:00.000Z"),
                msg("b", me, "2026-11-07T08:00:00.000Z"),
            ),
            myUserId = me,
            today = today,
        )
        assertEquals(4, rows.size)
        assertTrue(rows[0] is ChatRow.Day)
        assertTrue(rows[2] is ChatRow.Day)
    }

    @Test
    fun `messages inside three minutes from one sender group`() {
        val rows = ChatTimeline.build(
            listOf(
                msg("a", them, "2026-11-07T09:00:00.000Z"),
                msg("b", them, "2026-11-07T09:02:00.000Z"),
            ),
            myUserId = me,
            today = today,
        )
        val bubbles = rows.filterIsInstance<ChatRow.Message>()
        assertEquals(2, bubbles.size)
        assertTrue(bubbles[0].firstInRun)
        assertFalse(bubbles[0].grouped)
        assertTrue(bubbles[1].grouped)
        assertFalse(bubbles[1].firstInRun)
    }

    @Test
    fun `a gap over three minutes starts a new run`() {
        val rows = ChatTimeline.build(
            listOf(
                msg("a", them, "2026-11-07T09:00:00.000Z"),
                msg("b", them, "2026-11-07T09:04:00.000Z"),
            ),
            myUserId = me,
            today = today,
        )
        val bubbles = rows.filterIsInstance<ChatRow.Message>()
        assertTrue(bubbles[1].firstInRun)
        assertFalse(bubbles[1].grouped)
    }

    @Test
    fun `a change of sender starts a new run even within three minutes`() {
        val rows = ChatTimeline.build(
            listOf(
                msg("a", them, "2026-11-07T09:00:00.000Z"),
                msg("b", me, "2026-11-07T09:00:30.000Z"),
            ),
            myUserId = me,
            today = today,
        )
        val bubbles = rows.filterIsInstance<ChatRow.Message>()
        assertTrue(bubbles[1].firstInRun)
        assertTrue(bubbles[1].mine)
        assertFalse(bubbles[0].mine)
    }

    @Test
    fun `an empty conversation has no rows`() {
        assertTrue(ChatTimeline.build(emptyList(), me, today).isEmpty())
    }
}
