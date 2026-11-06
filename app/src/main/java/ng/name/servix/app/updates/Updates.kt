package ng.name.servix.app.updates

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.File
import java.net.URI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import ng.name.servix.app.BuildConfig
import ng.name.servix.app.data.network.ApiErrorParser
import ng.name.servix.app.data.network.servixUserAgent
import okhttp3.OkHttpClient
import okhttp3.Request

private val Context.updateStore by preferencesDataStore(name = "updates")
private val checked = longPreferencesKey("last_check")
private val cached = stringPreferencesKey("release_json")
private val shown = stringPreferencesKey("dialog_version")

@Serializable
data class ReleaseAsset(val name: String, val browser_download_url: String, val size: Long = 0)
@Serializable
data class Release(val tag_name: String, val name: String, val html_url: String, val body: String? = null, val assets: List<ReleaseAsset> = emptyList())
data class AvailableUpdate(val version: String, val release: Release, val asset: ReleaseAsset)

private fun parseVersion(value: String): List<Long>? {
    val parts = value.split('.')
    return if (parts.size == 3) parts.map { it.toLongOrNull() ?: return null } else null
}

/** New releases are named "Servix v0.6.2 (build 21)" on tag "v0.6.2-b21"; older ones said "… (v0.3.2)". */
private fun versionOf(release: Release): String? =
    Regex("v(\\d+\\.\\d+\\.\\d+)").find(release.name)?.groupValues?.get(1)
        ?: Regex("v?(\\d+\\.\\d+\\.\\d+)").find(release.tag_name)?.groupValues?.get(1)

private fun isNewer(candidate: List<Long>, current: List<Long>): Boolean =
    (0..2).firstOrNull { candidate[it] != current[it] }?.let { candidate[it] > current[it] } ?: false

/**
 * GitHub lists releases by tag text, so "test-build-7" sorts above "test-build-20" and the first
 * release is not necessarily the newest. Scan the first ten and keep the highest semantic version
 * that actually carries an .apk, then apply the trusted-URL checks.
 */
fun selectUpdate(releases: List<Release>, current: String): AvailableUpdate? {
    val currentParts = parseVersion(current) ?: return null
    val best = releases.take(10).mapNotNull { release ->
        val asset = release.assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) } ?: return@mapNotNull null
        val version = versionOf(release) ?: return@mapNotNull null
        val parts = parseVersion(version) ?: return@mapNotNull null
        if (!isNewer(parts, currentParts)) return@mapNotNull null
        Triple(version, parts, release to asset)
    }.maxWithOrNull(compareBy({ it.second[0] }, { it.second[1] }, { it.second[2] })) ?: return null

    val version = best.first
    val (release, asset) = best.third
    val uri = runCatching { URI(asset.browser_download_url) }.getOrNull() ?: return null
    val page = runCatching { URI(release.html_url) }.getOrNull() ?: return null
    val repository = "/toluenetech-spec/servix-android/releases/"
    if (uri.scheme != "https" || uri.host != "github.com" ||
        !uri.path.startsWith(repository + "download/") || page.scheme != "https" ||
        page.host != "github.com" || !page.path.startsWith(repository)) return null
    return AvailableUpdate(version, release, asset)
}

class Updates(private val context: Context) {
    private val client = OkHttpClient()
    suspend fun check(force: Boolean = false): AvailableUpdate? {
        if (BuildConfig.SELF_UPDATE_CHANNEL != "github") return null
        return try {
            val prefs = context.updateStore.data.first()
            val now = System.currentTimeMillis()
            val last = prefs[checked] ?: 0
            val throttled = !force && now >= last && now - last < 6 * 60 * 60 * 1000L
            if (throttled && prefs[cached] == null) return null
            val raw = if (throttled) prefs[cached] else null
            if (raw == null) context.updateStore.edit { it[checked] = now; it.remove(cached) }
            val json = raw ?: withContext(Dispatchers.IO) {
                val request = Request.Builder().url("https://api.github.com/repos/toluenetech-spec/servix-android/releases?per_page=10")
                    .header("Accept", "application/vnd.github+json").header("User-Agent", servixUserAgent).build()
                client.newCall(request).execute().use { response ->
                    check(response.isSuccessful) { "GitHub HTTP ${response.code}" }
                    response.body?.string() ?: error("Empty GitHub response")
                }
            }.also { result -> context.updateStore.edit { it[checked] = now; it[cached] = result } }
            selectUpdate(ApiErrorParser.json.decodeFromString<List<Release>>(json), BuildConfig.VERSION_NAME)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.w("ServixUpdate", "Update check failed", e)
            null
        }
    }
    suspend fun shouldShow(version: String): Boolean {
        val prefs = context.updateStore.data.first()
        if (prefs[shown] == version) return false
        context.updateStore.edit { it[shown] = version }
        return true
    }
    suspend fun download(update: AvailableUpdate, progress: (Int?) -> Unit): File = withContext(Dispatchers.IO) {
        val directory = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: error("Download directory unavailable")
        val file = File(directory, "servix-update-${update.version}.apk")
        if (file.exists()) file.delete()
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(Uri.parse(update.asset.browser_download_url))
            .addRequestHeader("User-Agent", servixUserAgent)
            .setTitle("Servix v${update.version}")
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, file.name)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        val id = manager.enqueue(request)
        try {
            while (true) {
                val cursor = manager.query(DownloadManager.Query().setFilterById(id)) ?: error("Download unavailable")
                val status = cursor.use {
                    if (!it.moveToFirst()) error("Download unavailable")
                    val status = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                    val bytes = it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                    val total = it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                    progress(if (total > 0) ((bytes * 100 / total).toInt()).coerceIn(0, 100) else null)
                    status
                }
                if (status == DownloadManager.STATUS_SUCCESSFUL) break
                if (status == DownloadManager.STATUS_FAILED) error("Download failed")
                delay(500)
            }
            file
        } catch (e: Exception) {
            manager.remove(id)
            throw e
        }
    }
    fun install(file: File) {
        if (!context.packageManager.canRequestPackageInstalls()) {
            context.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", file)
        context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
