package ng.name.servix.app.data.repository

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import ng.name.servix.app.data.model.ServixApiException

/** Retry an idempotent read once if a 401 raced session publication. Never retry writes here. */
suspend fun <T> retrySessionRead(isCurrentSession: () -> Boolean, request: suspend () -> Result<T>): Result<T> {
    val first = request()
    currentCoroutineContext().ensureActive()
    if ((first.exceptionOrNull() as? ServixApiException)?.status != 401 || !isCurrentSession()) return first
    delay(150)
    if (!isCurrentSession()) return first
    return request().also { currentCoroutineContext().ensureActive() }
}

fun signedInError(error: Throwable?, fallback: String): String? = when {
    error == null -> null
    (error as? ServixApiException)?.status == 401 -> fallback
    else -> error.message ?: fallback
}
