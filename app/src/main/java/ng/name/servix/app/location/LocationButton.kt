package ng.name.servix.app.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlin.coroutines.resume
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import ng.name.servix.app.ui.components.ServixSecondaryButton

private val Context.cityStore by preferencesDataStore(name = "approximate_city")
private val cityKey = stringPreferencesKey("city")
fun storedCity(context: Context) = context.applicationContext.cityStore.data.map { it[cityKey] }

/** One user-initiated coarse fix. No coordinate is persisted or passed to any app networking code. */
@Suppress("DEPRECATION", "MissingPermission")
private suspend fun acquireFix(context: Context): Location? = withTimeoutOrNull(20_000) {
    suspendCancellableCoroutine { continuation ->
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }
        if (Build.VERSION.SDK_INT >= 30) {
            val cancellation = CancellationSignal()
            continuation.invokeOnCancellation { cancellation.cancel() }
            manager.getCurrentLocation(LocationManager.NETWORK_PROVIDER, cancellation, context.mainExecutor) { location ->
                if (continuation.isActive) continuation.resume(location)
            }
        } else {
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    manager.removeUpdates(this)
                    if (continuation.isActive) continuation.resume(location)
                }
                override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) = Unit
                override fun onProviderEnabled(provider: String) = Unit
                override fun onProviderDisabled(provider: String) {
                    manager.removeUpdates(this)
                    if (continuation.isActive) continuation.resume(null)
                }
            }
            continuation.invokeOnCancellation { manager.removeUpdates(listener) }
            manager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, listener, Looper.getMainLooper())
        }
    }
}

@Composable
fun LocationButton(onCityChosen: (String) -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var rationale by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    fun findCity() {
        if (busy) return
        busy = true
        message = null
        scope.launch {
            try {
                // Only the city name survives this expression; coordinates are never stored.
                val city = acquireFix(context)?.let { nearestCity(it.latitude, it.longitude)?.name ?: "" }
                when (city) {
                    null -> message = "Location unavailable. Please try again or enter a city."
                    "" -> {
                        context.applicationContext.cityStore.edit { it.remove(cityKey) }
                        message = "Your location isn't covered yet"
                    }
                    else -> {
                        context.applicationContext.cityStore.edit { it[cityKey] = city }
                        onCityChosen(city)
                        message = "Near $city"
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                message = "Location unavailable. Please try again or enter a city."
            } finally {
                busy = false
            }
        }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) findCity() else message = "Location permission was not granted. You can enter a city instead."
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        ServixSecondaryButton(text = if (busy) "Finding location…" else "Use my location", enabled = !busy, onClick = { rationale = true }, testTag = "use_my_location")
        message?.let { Text(it) }
    }
    if (rationale) {
        AlertDialog(
            onDismissRequest = { rationale = false },
            title = { Text("Use approximate location") },
            text = { Text("Servix uses your approximate location to show professionals near you. It stays on your phone.") },
            confirmButton = { TextButton(onClick = {
                rationale = false
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) findCity()
                else permission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
            }) { Text("Continue") } },
            dismissButton = { TextButton(onClick = { rationale = false }) { Text("Not now") } },
        )
    }
}
