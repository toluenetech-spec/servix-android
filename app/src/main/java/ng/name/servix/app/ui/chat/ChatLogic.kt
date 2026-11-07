package ng.name.servix.app.ui.chat

import ng.name.servix.app.data.model.ChatMessageDto
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Delivery state of **my** message — the web's tick rule, verbatim (`api/docs/CHAT.md:57-59`). */
enum class TickState { PENDING, SENT, DELIVERED, READ }

object ChatTicks {
    /**
     * `otherReadAt >= createdAt` → seen (✓✓ blue); else `otherDeliveredAt >= createdAt` → delivered
     * (✓✓ grey); else sent (✓). A message that has not reached the server yet is [TickState.PENDING]
     * (clock) and never shows ticks.
     */
    fun state(
        message: ChatMessageDto,
        myUserId: String,
        otherReadAt: String?,
        otherDeliveredAt: String?,
        pending: Boolean = false,
    ): TickState? {
        if (message.senderId != myUserId) return null
        if (pending || message.id.isBlank()) return TickState.PENDING
        val created = instantOrNull(message.createdAt) ?: return TickState.SENT
        val read = instantOrNull(otherReadAt)
        if (read != null && !read.isBefore(created)) return TickState.READ
        val delivered = instantOrNull(otherDeliveredAt)
        if (delivered != null && !delivered.isBefore(created)) return TickState.DELIVERED
        return TickState.SENT
    }

    fun instantOrNull(iso: String?): Instant? =
        runCatching { Instant.parse(iso) }.getOrNull()
}

/**
 * Message list maintenance: `message_update` carries the full message, so everything is an upsert by
 * `id`; an optimistic row is replaced by the server row that carries the same `clientId`
 * (`senderId + clientId` is the API's idempotency key, community.ts:161).
 */
object ChatMerge {
    fun upsert(current: List<ChatMessageDto>, incoming: ChatMessageDto): List<ChatMessageDto> {
        if (incoming.id.isBlank()) return current
        val byId = current.indexOfFirst { it.id == incoming.id }
        if (byId >= 0) {
            return current.toMutableList().also { it[byId] = incoming }.sortedBy { it.createdAt }
        }
        val byClient = incoming.clientId?.let { cid ->
            current.indexOfFirst { it.id.isBlank() && it.clientId == cid }
        } ?: -1
        if (byClient >= 0) {
            return current.toMutableList().also { it[byClient] = incoming }.sortedBy { it.createdAt }
        }
        return (current + incoming).sortedBy { it.createdAt }
    }

    fun upsertAll(current: List<ChatMessageDto>, incoming: List<ChatMessageDto>): List<ChatMessageDto> =
        incoming.fold(current) { acc, m -> upsert(acc, m) }
}

/** One row of the conversation, after day separators and sender grouping. */
sealed interface ChatRow {
    data class Day(val label: String) : ChatRow
    data class Message(
        val message: ChatMessageDto,
        val mine: Boolean,
        /** First bubble of a run by this sender: the run draws the tail and the time. */
        val firstInRun: Boolean,
        /** Inside a run: the bubble loses its tail and the gap shrinks. */
        val grouped: Boolean,
    ) : ChatRow
}

object ChatTimeline {
    const val GROUP_WINDOW_SECONDS = 180L

    fun build(messages: List<ChatMessageDto>, myUserId: String, today: LocalDate = ChatTime.today()): List<ChatRow> {
        val rows = mutableListOf<ChatRow>()
        var lastDay: LocalDate? = null
        var previousSender: String? = null
        var previousAt: Instant? = null
        messages.forEach { message ->
            val at = ChatTicks.instantOrNull(message.createdAt)
            val day = at?.atZone(ChatTime.ZONE)?.toLocalDate() ?: lastDay
            if (day != null && day != lastDay) {
                rows += ChatRow.Day(ChatTime.dayLabel(day, today))
                lastDay = day
                previousSender = null
                previousAt = null
            }
            val sameSender = message.senderId == previousSender &&
                previousAt != null && at != null &&
                Duration.between(previousAt, at).seconds in 0..GROUP_WINDOW_SECONDS
            rows += ChatRow.Message(
                message = message,
                mine = message.senderId == myUserId,
                firstInRun = !sameSender,
                grouped = sameSender,
            )
            previousSender = message.senderId
            previousAt = at
        }
        return rows
    }
}

object ChatTime {
    val ZONE: ZoneId = ZoneId.of("Africa/Lagos")
    private val clock = DateTimeFormatter.ofPattern("HH:mm", Locale.UK)
    private val dayMonth = DateTimeFormatter.ofPattern("d MMM", Locale.UK)
    private val fullDay = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.UK)

    fun today(): LocalDate = LocalDate.now(ZONE)

    fun clockLabel(iso: String?): String =
        ChatTicks.instantOrNull(iso)?.atZone(ZONE)?.format(clock).orEmpty()

    /** Inbox timestamp: time today, "Yesterday", then `d MMM`. */
    fun inboxLabel(iso: String?, today: LocalDate = today()): String {
        val at = ChatTicks.instantOrNull(iso)?.atZone(ZONE) ?: return ""
        val date = at.toLocalDate()
        return when (date) {
            today -> at.format(clock)
            today.minusDays(1) -> "Yesterday"
            else -> at.format(dayMonth)
        }
    }

    fun dayLabel(date: LocalDate, today: LocalDate = today()): String = when (date) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> date.format(fullDay)
    }

    /** "Online" / "Last seen 5 min ago" / "" — the presence line under the name. */
    fun lastSeenLabel(iso: String?, now: Instant = Instant.now()): String {
        val at = ChatTicks.instantOrNull(iso) ?: return ""
        val seconds = Duration.between(at, now).seconds.coerceAtLeast(0)
        val text = when {
            seconds < 60 -> "just now"
            seconds < 3600 -> "${seconds / 60} min ago"
            seconds < 86_400 -> "${seconds / 3600} h ago"
            else -> "${seconds / 86_400} d ago"
        }
        return "Last seen $text"
    }
}

object ChatText {
    private val url = Regex("""https?://[^\s<>"]+""")

    fun linksIn(text: String): List<String> = url.findAll(text).map { it.value }.toList()

    /**
     * "Share my location" is plain text on the wire: `📍 My location: https://maps.google.com/?q=lat,lng`,
     * which every client renders as a link (`api/docs/CHAT.md:110`).
     */
    fun isLocationShare(text: String): Boolean =
        text.startsWith("📍") && linksIn(text).isNotEmpty()

    fun locationUrl(text: String): String? = linksIn(text).firstOrNull()

    /** Text with links removed, for the human part of a location bubble. */
    fun withoutLinks(text: String): String = text.replace(url, "").trim()
}
