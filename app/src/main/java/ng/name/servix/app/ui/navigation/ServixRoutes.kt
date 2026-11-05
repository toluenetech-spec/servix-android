package ng.name.servix.app.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface ServixRoute {
    @Serializable
    data object Welcome : ServixRoute

    @Serializable
    data object Login : ServixRoute

    @Serializable
    data object Register : ServixRoute

    @Serializable
    data object VerifyEmail : ServixRoute

    @Serializable
    data object SecurityCheck : ServixRoute

    @Serializable
    data object ForgotPassword : ServixRoute

    @Serializable
    data object Home : ServixRoute

    @Serializable
    data object Search : ServixRoute

    @Serializable
    data class ProfessionalProfile(val slug: String) : ServixRoute

    @Serializable
    data class ServiceDetail(
        val slug: String,
        val rebookedFrom: String? = null,
        val initialNotes: String? = null,
        val openBookingSheet: Boolean = false,
    ) : ServixRoute

    @Serializable
    data object Bookings : ServixRoute

    @Serializable
    data class BookingDetail(
        val id: String,
        val pollingAfterPay: Boolean = false,
    ) : ServixRoute

    @Serializable
    data object ServixAi : ServixRoute

    @Serializable
    data object Account : ServixRoute
}
