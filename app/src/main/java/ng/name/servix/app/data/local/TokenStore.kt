package ng.name.servix.app.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ng.name.servix.app.data.model.UserDto

class TokenStore(context: Context) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val prefs: SharedPreferences = createSecurePrefs(context.applicationContext)

    @Volatile
    private var memoryAccessToken: String? = prefs.getString(KEY_ACCESS_TOKEN, null)

    fun getAccessToken(): String? = memoryAccessToken

    fun setAccessToken(token: String?) {
        memoryAccessToken = token
        prefs.edit().apply {
            if (token.isNullOrBlank()) remove(KEY_ACCESS_TOKEN)
            else putString(KEY_ACCESS_TOKEN, token)
        }.apply()
    }

    fun getCachedUser(): UserDto? {
        val raw = prefs.getString(KEY_CACHED_USER, null) ?: return null
        return try {
            json.decodeFromString<UserDto>(raw)
        } catch (_: Exception) {
            null
        }
    }

    fun setCachedUser(user: UserDto?) {
        prefs.edit().apply {
            if (user == null) remove(KEY_CACHED_USER)
            else putString(KEY_CACHED_USER, json.encodeToString(user))
        }.apply()
    }

    fun clear() {
        memoryAccessToken = null
        prefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_CACHED_USER)
            .apply()
    }

    private fun createSecurePrefs(appContext: Context): SharedPreferences {
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
            // Fallback only if hardware keystore fails in a headless/test container
            appContext.getSharedPreferences("${PREFS_NAME}_fallback", Context.MODE_PRIVATE)
        }
    }

    companion object {
        private const val PREFS_NAME = "servix_encrypted_session"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_CACHED_USER = "cached_user"
    }
}
