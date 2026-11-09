package ng.name.servix.app.ui.home

import ng.name.servix.app.data.model.BookingDto
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/**
 * The two numbers on the professional Home strip, computed from booking data the app already has.
 *
 * Both labels are literally what the numbers are: jobs whose `scheduledAt` falls inside the current
 * week, and jobs whose `completedAt` falls inside it. Nothing here is a payout figure — the payouts
 * API is not part of this release, and a Naira amount summed from completed bookings is not
 * "awaiting payout".
 *
 * Week = Monday 00:00 to the next Monday 00:00 in Africa/Lagos, the app's display zone.
 */
object HomeWeek {
    val ZONE: ZoneId = ZoneId.of("Africa/Lagos")

    /** Statuses that mean the job is no longer happening, so it is not "scheduled". */
    private val deadStatuses = setOf("cancelled", "declined", "completed")

    fun jobsScheduled(bookings: List<BookingDto>, now: Instant = Instant.now()): Int {
        val (start, end) = bounds(now)
        return bookings.count { booking ->
            val at = instantOrNull(booking.scheduledAt)
            at != null && !at.isBefore(start) && at.isBefore(end) && booking.status !in deadStatuses
        }
    }

    fun jobsCompleted(bookings: List<BookingDto>, now: Instant = Instant.now()): Int {
        val (start, end) = bounds(now)
        return bookings.count { booking ->
            val at = instantOrNull(booking.completedAt)
            at != null && !at.isBefore(start) && at.isBefore(end)
        }
    }

    fun bounds(now: Instant): Pair<Instant, Instant> {
        val today = now.atZone(ZONE).toLocalDate()
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val start = monday.atStartOfDay(ZONE).toInstant()
        val end = monday.plusWeeks(1).atStartOfDay(ZONE).toInstant()
        return start to end
    }

    /**
     * `scheduledAt` is a date-time in the API, but older rows carry a bare date; accept both rather
     * than silently counting them as zero.
     */
    internal fun instantOrNull(raw: String?): Instant? {
        val value = raw?.trim().orEmpty()
        if (value.isEmpty()) return null
        runCatching { return Instant.parse(value) }
        runCatching { return LocalDateTime.parse(value).atZone(ZONE).toInstant() }
        runCatching { return LocalDate.parse(value).atStartOfDay(ZONE).toInstant() }
        return null
    }
}
