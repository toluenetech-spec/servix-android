package ng.name.servix.app.ui.bookings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import ng.name.servix.app.ui.components.ServixBadge
import ng.name.servix.app.ui.theme.Charcoal
import ng.name.servix.app.ui.theme.Coral
import ng.name.servix.app.ui.theme.DangerRed
import ng.name.servix.app.ui.theme.DeepForest
import ng.name.servix.app.ui.theme.ForestGreen
import ng.name.servix.app.ui.theme.MutedGray
import ng.name.servix.app.ui.theme.SoftSand
import ng.name.servix.app.ui.theme.WarmIvory

object BookingStatusMeta {
    val ACTIVE_STATUSES = setOf(
        "pending_payment",
        "requested",
        "accepted",
        "in_progress",
        "delivered",
        "disputed",
    )

    val PAST_STATUSES = setOf(
        "completed",
        "cancelled",
        "declined",
        "refunded",
    )

    /**
     * Exact human labels specified in Stage 3:
     * pending_payment "Awaiting payment", requested "Requested", accepted "Accepted",
     * in_progress "In progress", delivered "Delivered", completed "Completed",
     * cancelled "Cancelled", declined "Declined", disputed "Disputed", refunded "Refunded".
     */
    fun labelFor(status: String): String = when (status) {
        "pending_payment" -> "Awaiting payment"
        "requested" -> "Requested"
        "accepted" -> "Accepted"
        "in_progress" -> "In progress"
        "delivered" -> "Delivered"
        "completed" -> "Completed"
        "cancelled" -> "Cancelled"
        "declined" -> "Declined"
        "disputed" -> "Disputed"
        "refunded" -> "Refunded"
        else -> status
    }

    fun colorsFor(status: String): Pair<Color, Color> = when (status) {
        "pending_payment" -> Coral.copy(alpha = 0.16f) to Coral
        "requested" -> SoftSand to Charcoal
        "accepted" -> DeepForest.copy(alpha = 0.12f) to DeepForest
        "in_progress" -> ForestGreen.copy(alpha = 0.14f) to ForestGreen
        "delivered" -> Coral.copy(alpha = 0.16f) to Coral
        "completed" -> DeepForest to WarmIvory
        "cancelled" -> SoftSand to MutedGray
        "declined" -> SoftSand to MutedGray
        "disputed" -> DangerRed.copy(alpha = 0.14f) to DangerRed
        "refunded" -> SoftSand to Charcoal
        else -> SoftSand to Charcoal
    }
}

@Composable
fun BookingStatusChip(
    status: String,
    modifier: Modifier = Modifier,
) {
    val label = BookingStatusMeta.labelFor(status)
    val (bg, fg) = BookingStatusMeta.colorsFor(status)
    ServixBadge(
        text = label,
        containerColor = bg,
        contentColor = fg,
        modifier = modifier,
    )
}
