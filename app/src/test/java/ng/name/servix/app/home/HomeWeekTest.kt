package ng.name.servix.app.home

import ng.name.servix.app.data.model.BookingDto
import ng.name.servix.app.ui.home.HomeWeek
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

/**
 * The professional Home strip must only ever label a number as what it is (owner review, Stage 1):
 * jobs whose scheduled date is in this week, and jobs completed in it.
 */
class HomeWeekTest {

    /** Saturday 7 November 2026, 16:00 Lagos — the week runs Mon 2 Nov 00:00 → Mon 9 Nov 00:00. */
    private val now = Instant.parse("2026-11-07T15:00:00.000Z")

    private fun booking(
        id: String = "b1",
        status: String = "accepted",
        scheduledAt: String = "2026-11-07T10:00:00.000Z",
        completedAt: String? = null,
    ) = BookingDto(
        id = id,
        status = status,
        scheduledAt = scheduledAt,
        completedAt = completedAt,
    )

    @Test
    fun `week bounds start on Monday in Lagos`() {
        val (start, end) = HomeWeek.bounds(now)
        // Lagos is UTC+1: Monday 00:00 local = Sunday 23:00 UTC.
        assertEquals(Instant.parse("2026-11-01T23:00:00.000Z"), start)
        assertEquals(Instant.parse("2026-11-08T23:00:00.000Z"), end)
    }

    @Test
    fun `jobs scheduled inside the week are counted`() {
        val bookings = listOf(
            booking("a", scheduledAt = "2026-11-04T09:00:00.000Z"),
            booking("b", scheduledAt = "2026-11-07T09:00:00.000Z"),
        )
        assertEquals(2, HomeWeek.jobsScheduled(bookings, now))
    }

    @Test
    fun `last week and next week are excluded`() {
        val bookings = listOf(
            booking("a", scheduledAt = "2026-10-28T09:00:00.000Z"),
            booking("b", scheduledAt = "2026-11-11T09:00:00.000Z"),
        )
        assertEquals(0, HomeWeek.jobsScheduled(bookings, now))
    }

    @Test
    fun `a cancelled job is not scheduled work`() {
        val bookings = listOf(booking("a", status = "cancelled"))
        assertEquals(0, HomeWeek.jobsScheduled(bookings, now))
    }

    @Test
    fun `a completed job is counted as completed and not as scheduled`() {
        val bookings = listOf(
            booking("a", status = "completed", scheduledAt = "2026-11-06T09:00:00.000Z", completedAt = "2026-11-06T12:00:00.000Z"),
        )
        assertEquals(0, HomeWeek.jobsScheduled(bookings, now))
        assertEquals(1, HomeWeek.jobsCompleted(bookings, now))
    }

    @Test
    fun `completed jobs from an earlier week are excluded`() {
        val bookings = listOf(
            booking("a", status = "completed", completedAt = "2026-10-30T12:00:00.000Z"),
        )
        assertEquals(0, HomeWeek.jobsCompleted(bookings, now))
    }

    @Test
    fun `a bare date is accepted`() {
        val bookings = listOf(booking("a", scheduledAt = "2026-11-05"))
        assertEquals(1, HomeWeek.jobsScheduled(bookings, now))
    }

    @Test
    fun `an unparseable date is ignored rather than guessed`() {
        val bookings = listOf(booking("a", scheduledAt = "next tuesday"))
        assertEquals(0, HomeWeek.jobsScheduled(bookings, now))
    }
}
