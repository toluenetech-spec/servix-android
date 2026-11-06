package ng.name.servix.app.util

import android.net.Uri
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import ng.name.servix.app.data.model.OAuthReturn

/**
 * The Custom Tab returns on `ng.name.servix.app://oauth/callback` (NATIVE_CLIENTS.md:52-57).
 * MainActivity publishes the parsed return here; the app shell hands the ticket to AuthViewModel.
 */
object OAuthReturnBus {
    const val SCHEME = "ng.name.servix.app"
    const val HOST = "oauth"
    const val PATH = "/callback"

    /** GET /auth/<provider>/native/start?client=ng.name.servix.app (NATIVE_CLIENTS.md:52-53). */
    fun nativeStartUrl(provider: String): String =
        "https://api.servix.name.ng/api/v1/auth/$provider/native/start?client=ng.name.servix.app"

    private val _returns = MutableSharedFlow<OAuthReturn>(extraBufferCapacity = 1)
    val returns = _returns.asSharedFlow()

    fun publish(uri: Uri) {
        if (uri.scheme != SCHEME || uri.host != HOST || uri.path != PATH) return
        _returns.tryEmit(
            OAuthReturn(
                provider = uri.getQueryParameter("provider"),
                ticket = uri.getQueryParameter("ticket"),
                error = uri.getQueryParameter("error"),
            )
        )
    }
}
