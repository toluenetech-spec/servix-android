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
     * Resolves an image path the backend hands us into a URL Coil can load.
     *
     *  * absolute `http(s)` URLs are returned unchanged — object storage already hands us those;
     *  * `/media/…` and `/uploads/…` are served by the **API**, so they resolve against
     *    `API_BASE_URL`'s origin plus `/api/v1`, never against the marketing site;
     *  * every other root-relative path is a website asset (`/images/professionals/…`, `/brand/…`)
     *    and resolves against `WEB_BASE_URL`.
     */
    fun resolveServixMediaUrl(rawUrl: String?): String? {
        val trimmed = rawUrl?.trim().orEmpty()
        if (trimmed.isEmpty()) return null
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return trimmed
        val path = if (trimmed.startsWith("/")) trimmed else "/$trimmed"
        val base = if (isApiServedMediaPath(path)) apiMediaBase() else BuildConfig.WEB_BASE_URL.trimEnd('/')
        return "$base$path"
    }

    /** Paths the API serves: uploaded avatars/portfolios and the upload redirect. */
    fun isApiServedMediaPath(path: String): Boolean =
        path == "/media" || path == "/uploads" ||
            path.startsWith("/media/") || path.startsWith("/uploads/")

    /**
     * `API_BASE_URL`'s origin plus `/api/v1`. The origin is taken from the configured URL rather than
     * string-replaced, so a build pointed at a different host (or at `/api/v1/` with or without the
     * trailing slash) still resolves to `<origin>/api/v1`.
     */
    fun apiMediaBase(): String {
        val raw = BuildConfig.API_BASE_URL.trim()
        val fallback = raw.trimEnd('/')
        val uri = runCatching { java.net.URI(raw) }.getOrNull()
        val origin = uri?.takeIf { it.scheme != null && it.authority != null }
            ?.let { "${it.scheme}://${it.authority}" }
        return if (origin != null) "$origin/api/v1" else "$fallback/api/v1"
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
