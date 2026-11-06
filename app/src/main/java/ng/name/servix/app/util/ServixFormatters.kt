package ng.name.servix.app.util

import ng.name.servix.app.BuildConfig
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object ServixFormatters {
    private val numberFormatter: NumberFormat
        get() = NumberFormat.getIntegerInstance(Locale.US)

    private val shortDateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.UK)
    private val dayMonthFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.UK)
    private val weekdayDayMonthFormatter = DateTimeFormatter.ofPattern("EEE d MMM", Locale.UK)
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.UK)

    /**
     * Formats integer Naira amounts as ₦ with thousands separators (e.g. ₦120,000).
     * Mirrors reference/web/src/lib/format.js formatPrice().
     */
    fun formatNaira(amount: Long?): String {
        val safe = amount ?: 0L
        return "₦${numberFormatter.format(safe)}"
    }

    /**
     * Resolves catalogue image URLs.
     * Uploaded media from the backend is an absolute URL (https://api.servix.name.ng/media/...).
     * Catalogue seed assets are root-relative paths (/images/professionals/..., /images/services/...)
     * served from https://www.servix.name.ng.
     */
    fun resolveServixMediaUrl(rawUrl: String?): String? {
        val trimmed = rawUrl?.trim().orEmpty()
        if (trimmed.isEmpty()) return null
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed
        }
        val base = BuildConfig.WEB_BASE_URL.trimEnd('/')
        return if (trimmed.startsWith("/")) {
            "$base$trimmed"
        } else {
            "$base/$trimmed"
        }
    }

    fun extractInitials(name: String?): String {
        val clean = name?.trim().orEmpty()
        if (clean.isEmpty()) return "?"
        val parts = clean.split(Regex("\\s+")).filter { it.isNotBlank() }
        return when {
            parts.size >= 2 -> "${parts.first().first().uppercaseChar()}${parts.last().first().uppercaseChar()}"
            else -> parts[0].take(1).uppercase()
        }
    }

    fun formatDate(raw: String?): String {
        val value = raw?.trim().orEmpty()
        if (value.isEmpty()) return ""
        return try {
            if (value.length == 10 && value[4] == '-') {
                LocalDate.parse(value).format(shortDateFormatter)
            } else {
                Instant.parse(value)
                    .atZone(ZoneId.of("Africa/Lagos"))
                    .toLocalDate()
                    .format(shortDateFormatter)
            }
        } catch (_: Exception) {
            value.take(10)
        }
    }

    fun formatShortDayMonth(rawIso: String?): String {
        val value = rawIso?.trim().orEmpty()
        if (value.isEmpty()) return ""
        return try {
            Instant.parse(value)
                .atZone(ZoneId.of("Africa/Lagos"))
                .toLocalDate()
                .format(dayMonthFormatter)
        } catch (_: Exception) {
            value.take(10)
        }
    }

    fun formatSlotDayHeader(rawIso: String): String {
        return try {
            Instant.parse(rawIso)
                .atZone(ZoneId.of("Africa/Lagos"))
                .format(weekdayDayMonthFormatter)
        } catch (_: Exception) {
            rawIso.take(10)
        }
    }

    fun formatSlotTime(rawIso: String): String {
        return try {
            Instant.parse(rawIso)
                .atZone(ZoneId.of("Africa/Lagos"))
                .format(timeFormatter)
        } catch (_: Exception) {
            rawIso.substringAfter("T").take(5)
        }
    }

    private val fullDateFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.UK)

    fun formatDateTimeMedium(rawIso: String?): String {
        val value = rawIso?.trim().orEmpty()
        if (value.isEmpty()) return ""
        return try {
            val zdt = Instant.parse(value).atZone(ZoneId.of("Africa/Lagos"))
            "${zdt.format(weekdayDayMonthFormatter)} · ${zdt.format(timeFormatter)}"
        } catch (_: Exception) {
            value
        }
    }

    fun formatDateTimeFull(rawIso: String?): String {
        val value = rawIso?.trim().orEmpty()
        if (value.isEmpty()) return ""
        return try {
            val zdt = Instant.parse(value).atZone(ZoneId.of("Africa/Lagos"))
            "${zdt.format(fullDateFormatter)} at ${zdt.format(timeFormatter)} (WAT)"
        } catch (_: Exception) {
            value
        }
    }
}
