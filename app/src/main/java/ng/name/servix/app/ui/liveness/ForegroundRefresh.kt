package ng.name.servix.app.ui.liveness

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.currentCoroutineContext

/** Runs immediately on resume, then periodically only while this lifecycle is RESUMED. */
suspend fun runForegroundRefresh(lifecycle: Lifecycle, intervalMs: Long = 25_000, refresh: () -> Unit) {
    lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
        while (currentCoroutineContext().isActive) {
            refresh()
            delay(intervalMs)
        }
    }
}

@Composable
fun ForegroundRefresh(key: Any?, refresh: () -> Unit) {
    val owner = LocalLifecycleOwner.current
    val latest by rememberUpdatedState(refresh)
    LaunchedEffect(owner, key) { runForegroundRefresh(owner.lifecycle) { latest() } }
}
