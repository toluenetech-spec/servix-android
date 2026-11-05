package ng.name.servix.app.data.network

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

/**
 * Persistent OkHttp CookieJar storing the httpOnly `servix_refresh` cookie across app restarts.
 */
class PersistentCookieJar(context: Context) : CookieJar {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val prefs: SharedPreferences = createCookiePrefs(context.applicationContext)
    private val lock = Any()

    @Serializable
    private data class StoredCookie(
        val name: String,
        val value: String,
        val expiresAt: Long,
        val domain: String,
        val path: String,
        val secure: Boolean,
        val httpOnly: Boolean,
        val hostOnly: Boolean,
    ) {
        fun toOkHttpCookie(): Cookie {
            val builder = Cookie.Builder()
                .name(name)
                .value(value)
                .expiresAt(expiresAt)
                .path(path)
            if (hostOnly) builder.hostOnlyDomain(domain) else builder.domain(domain)
            if (secure) builder.secure()
            if (httpOnly) builder.httpOnly()
            return builder.build()
        }

        companion object {
            fun from(cookie: Cookie): StoredCookie = StoredCookie(
                name = cookie.name,
                value = cookie.value,
                expiresAt = cookie.expiresAt,
                domain = cookie.domain,
                path = cookie.path,
                secure = cookie.secure,
                httpOnly = cookie.httpOnly,
                hostOnly = cookie.hostOnly,
            )
        }
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        synchronized(lock) {
            val current = loadAllLocked().toMutableMap()
            val now = System.currentTimeMillis()
            for (cookie in cookies) {
                val key = cookieKey(cookie)
                if (cookie.expiresAt <= now || cookie.value.isEmpty()) {
                    current.remove(key)
                } else {
                    current[key] = StoredCookie.from(cookie)
                }
            }
            persistLocked(current)
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        synchronized(lock) {
            val current = loadAllLocked().toMutableMap()
            val now = System.currentTimeMillis()
            val valid = mutableListOf<Cookie>()
            var mutated = false

            val iterator = current.entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                val cookie = entry.value.toOkHttpCookie()
                if (cookie.expiresAt <= now) {
                    iterator.remove()
                    mutated = true
                } else if (cookie.matches(url)) {
                    valid.add(cookie)
                }
            }

            if (mutated) {
                persistLocked(current)
            }
            return valid
        }
    }

    fun hasRefreshCookie(): Boolean {
        synchronized(lock) {
            val now = System.currentTimeMillis()
            return loadAllLocked().values.any { it.name == "servix_refresh" && it.expiresAt > now && it.value.isNotEmpty() }
        }
    }

    fun clear() {
        synchronized(lock) {
            prefs.edit().clear().apply()
        }
    }

    private fun cookieKey(cookie: Cookie): String =
        "${cookie.domain}|${cookie.path}|${cookie.name}"

    private fun loadAllLocked(): Map<String, StoredCookie> {
        val raw = prefs.getString(KEY_COOKIES_JSON, null) ?: return emptyMap()
        return try {
            json.decodeFromString<Map<String, StoredCookie>>(raw)
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun persistLocked(map: Map<String, StoredCookie>) {
        prefs.edit().putString(KEY_COOKIES_JSON, json.encodeToString(map)).apply()
    }

    private fun createCookiePrefs(appContext: Context): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(appContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                appContext,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        } catch (_: Exception) {
            appContext.getSharedPreferences("${PREFS_NAME}_fallback", Context.MODE_PRIVATE)
        }
    }

    companion object {
        private const val PREFS_NAME = "servix_cookie_jar"
        private const val KEY_COOKIES_JSON = "cookies_json"
    }
}
