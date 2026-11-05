package ng.name.servix.app.util

import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object ServixFormatters {
    private val nairaLocale = Locale("en", "NG")
    private val lagosZone: ZoneId = ZoneId.of("Africa/Lagos")

    private val mediumDateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.UK)
        .withZone(lagosZone)
    private val fullDateTimeFormatter = DateTimeFormatter.ofPattern("EEE, d MMM yyyy 'at' HH:mm", Locale.UK)
        .withZone(lagosZone)
    private val shortTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.UK)
        .withZone(lagosZone)

    /**
     * Formats an amount in whole Naira with the ₦ symbol and thousands separators (e.g. ₦350,000).
     */
    fun formatNaira(amount: Long): String {
        val numberFormat = NumberFormat.getIntegerInstance(nairaLocale)
        return "₦${numberFormat.format(amount)}"
    }

    fun formatNaira(amount: Double): String = formatNaira(amount.toLong())

    /**
     * Converts a Kobo value (minor units) into whole Naira and formats with ₦.
     */
    fun formatKoboAsNaira(amountKobo: Long): String = formatNaira(amountKobo / 100L)

    /**
     * Parses an ISO-8601 timestamp and formats it in the Africa/Lagos timezone.
     */
    fun formatLagosDateTime(isoString: String?): String {
        if (isoString.isNullOrBlank()) return "—"
        return try {
            val instant = Instant.parse(isoString)
            fullDateTimeFormatter.format(instant)
        } catch (_: Exception) {
            isoString
        }
    }

    fun formatLagosDate(isoString: String?): String {
        if (isoString.isNullOrBlank()) return "—"
        return try {
            val instant = Instant.parse(isoString)
            mediumDateFormatter.format(instant)
        } catch (_: Exception) {
            isoString
        }
    }

    fun formatLagosTime(isoString: String?): String {
        if (isoString.isNullOrBlank()) return "—"
        return try {
            val instant = Instant.parse(isoString)
            shortTimeFormatter.format(instant)
        } catch (_: Exception) {
            isoString
        }
    }
}
