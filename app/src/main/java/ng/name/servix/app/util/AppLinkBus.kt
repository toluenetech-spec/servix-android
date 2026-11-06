package ng.name.servix.app.util

import android.net.Uri
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Where an in-app deep link should land: a booking, or the notifications list. */
data class AppLinkTarget(val bookingId: String? = null, val notifications: Boolean = false)

/**
 * Notification taps open MainActivity with `servix://booking/<id>` or `servix://notifications`;
 * the shell consumes the target and navigates.
 */
object AppLinkBus {
    private val _links = MutableSharedFlow<AppLinkTarget>(extraBufferCapacity = 1)
    val links = _links.asSharedFlow()

    fun publish(uri: Uri) {
        if (uri.scheme != "servix") return
        when (uri.host) {
            "booking" -> {
                val id = uri.pathSegments.firstOrNull().orEmpty()
                if (id.isNotBlank()) _links.tryEmit(AppLinkTarget(bookingId = id))
            }
            "notifications" -> _links.tryEmit(AppLinkTarget(notifications = true))
        }
    }
}
