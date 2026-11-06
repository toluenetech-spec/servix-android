package ng.name.servix.app.ui.account

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.net.Inet4Address
import java.net.NetworkInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ng.name.servix.app.location.LocationButton
import ng.name.servix.app.BuildConfig
import ng.name.servix.app.data.model.AccountDeviceDto
import ng.name.servix.app.data.repository.AccountRepository
import ng.name.servix.app.ui.components.ServixCard
import ng.name.servix.app.ui.theme.Charcoal
import ng.name.servix.app.ui.theme.MutedGray

@Composable
fun DeviceCard(repository: AccountRepository, userId: String?, city: String?) {
    val context = LocalContext.current
    var device by remember(userId) { mutableStateOf<AccountDeviceDto?>(null) }
    var localIp by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(userId) {
        device = if (userId == null) null else repository.getDevice().getOrNull()
        localIp = withContext(Dispatchers.IO) {
            runCatching {
                NetworkInterface.getNetworkInterfaces().toList()
                    .filter { it.isUp && !it.isLoopback }
                    .flatMap { it.inetAddresses.toList() }
                    .firstOrNull { it is Inet4Address && !it.isLoopbackAddress && !it.isLinkLocalAddress }
                    ?.hostAddress
            }.getOrNull()
        }
    }
    val model = "${Build.MANUFACTURER} ${Build.MODEL}"
    val deviceName = remember { runCatching { Settings.Global.getString(context.contentResolver, "device_name") }.getOrNull()?.takeIf { it.isNotBlank() } ?: model }
    val network = runCatching {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = manager.getNetworkCapabilities(manager.activeNetwork)
        when {
            caps == null -> "Offline"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile"
            else -> "Other"
        }
    }.getOrDefault("Unknown")
    ServixCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("This device", style = MaterialTheme.typography.titleMedium, color = Charcoal)
            listOf(
                "Device name" to deviceName,
                "Model" to model,
                "Android version" to Build.VERSION.RELEASE,
                "App version" to "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                "Network" to network,
                "Local IP" to (localIp ?: "Unavailable"),
                "Public IP" to (device?.ip ?: "Unavailable"),
                "Device as seen by Servix" to (device?.device ?: "Unavailable"),
                "Approximate location" to (city ?: "Not enabled"),
            ).forEach { (label, value) ->
                Text("$label: $value", style = MaterialTheme.typography.bodySmall, color = Charcoal)
            }
            LocationButton()
            Text("Servix uses your device name and address only to alert you about sign-ins from new devices.", style = MaterialTheme.typography.bodySmall, color = MutedGray)
        }
    }
}
