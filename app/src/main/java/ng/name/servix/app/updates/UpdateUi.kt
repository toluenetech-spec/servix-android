package ng.name.servix.app.updates

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import ng.name.servix.app.BuildConfig
import ng.name.servix.app.ui.components.ServixCard
import ng.name.servix.app.ui.components.ServixPrimaryButton
import ng.name.servix.app.ui.components.ServixSecondaryButton
import ng.name.servix.app.util.CustomTabLauncher

class UpdateUiState(private val context: Context, private val scope: CoroutineScope) {
    private val updates = Updates(context)
    var available by mutableStateOf<AvailableUpdate?>(null); private set
    var showDialog by mutableStateOf(false); private set
    var downloading by mutableStateOf(false); private set
    var progress by mutableStateOf<Int?>(null); private set
    var error by mutableStateOf<String?>(null); private set
    private var file: File? = null
    private var waitingForInstallPermission = false
    val enabled get() = BuildConfig.SELF_UPDATE_CHANNEL == "github"
    fun check(force: Boolean = false) {
        if (!enabled) return
        scope.launch {
            val previousVersion = available?.version
            val result = updates.check(force)
            if (result?.version != previousVersion) file = null
            available = result
            available?.let { if (updates.shouldShow(it.version)) showDialog = true }
        }
    }
    fun onResume() {
        if (!waitingForInstallPermission) return
        waitingForInstallPermission = false
        val apk = file ?: return
        if (context.packageManager.canRequestPackageInstalls()) {
            try { updates.install(apk); error = null }
            catch (e: Exception) { Log.w("ServixUpdate", "Could not open installer", e); error = "Unable to open the installer. Open the download page instead." }
        } else {
            error = "Allow installs from Servix, then tap Update again."
        }
    }
    fun dismiss() { showDialog = false }
    fun openPage() { available?.let { CustomTabLauncher.launchUrl(context, it.release.html_url) } }
    fun update() {
        val selected = available ?: return
        if (downloading) return
        scope.launch {
            try {
                error = null
                val existing = file?.takeIf { it.exists() }
                val apk = existing ?: run {
                    downloading = true
                    updates.download(selected) { value -> scope.launch { progress = value } }
                }
                file = apk
                downloading = false
                waitingForInstallPermission = !context.packageManager.canRequestPackageInstalls()
                updates.install(apk)
                if (waitingForInstallPermission) error = "Allow installs from Servix to continue."
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                downloading = false
                Log.w("ServixUpdate", "Download or installation failed", e)
                error = "Unable to install the update. Open the download page instead."
            }
        }
    }
}

@Composable
fun rememberUpdateUiState(): UpdateUiState {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state = remember(context, scope) { UpdateUiState(context, scope) }
    LaunchedEffect(state) { state.check() }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner, state) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) state.onResume() }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    if (state.showDialog) {
        AlertDialog(
            onDismissRequest = state::dismiss,
            title = { Text("Update available — Servix v${state.available?.version}") },
            text = { Text("A new test build is available.") },
            confirmButton = { TextButton(onClick = { state.dismiss(); state.update() }) { Text("Update") } },
            dismissButton = { TextButton(onClick = state::dismiss) { Text("Later") } },
        )
    }
    return state
}

@Composable
fun UpdateCard(state: UpdateUiState) {
    val update = state.available ?: return
    ServixCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Update available — Servix v${update.version}", style = MaterialTheme.typography.titleMedium)
            if (state.downloading) {
                val value = state.progress
                if (value == null) LinearProgressIndicator() else {
                    LinearProgressIndicator(progress = { value / 100f })
                    Text("Downloading: $value%")
                }
            }
            state.error?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            ServixPrimaryButton(text = if (state.downloading) "Downloading…" else "Update", enabled = !state.downloading, onClick = state::update, testTag = "update_download")
            ServixSecondaryButton(text = "Open download page", onClick = state::openPage, testTag = "update_page")
        }
    }
}
