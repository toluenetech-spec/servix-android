package ng.name.servix.app.ui.home

import ng.name.servix.app.data.model.BookingDto
import ng.name.servix.app.data.model.OwnServiceDto
import ng.name.servix.app.data.model.ProfessionalAnalyticsTotalsDto

data class ProfessionalHomeState(
    val userId: String? = null,
    val loading: Boolean = true,
    val jobs: List<BookingDto> = emptyList(),
    val totals: ProfessionalAnalyticsTotalsDto? = null,
    val services: List<OwnServiceDto> = emptyList(),
    val jobsError: String? = null,
    val analyticsError: String? = null,
    val servicesError: String? = null,
    val actionError: String? = null,
    val busyBookingId: String? = null,
)

/** Only expose state transitions accepted by the existing server state machine. */
fun professionalActionFor(status: String): Pair<String, String>? = when (status) {
    "requested" -> "accept" to "Accept"
    "accepted" -> "start" to "Start work"
    "in_progress" -> "deliver" to "Mark delivered"
    else -> null
}

fun professionalActionAllowed(status: String, action: String): Boolean =
    professionalActionFor(status)?.first == action || (status == "requested" && action == "decline")
