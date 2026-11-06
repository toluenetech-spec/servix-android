package ng.name.servix.app.notifications

import android.content.Context

/** Remembers the last notification we already surfaced and whether the permission was asked. */
class NotificationPrefs(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("servix_notifications", Context.MODE_PRIVATE)

    var lastSeenId: String?
        get() = prefs.getString(KEY_LAST_SEEN, null)
        set(value) = prefs.edit().putString(KEY_LAST_SEEN, value).apply()

    var permissionAsked: Boolean
        get() = prefs.getBoolean(KEY_ASKED, false)
        set(value) = prefs.edit().putBoolean(KEY_ASKED, value).apply()

    fun clear() = prefs.edit().remove(KEY_LAST_SEEN).apply()

    private companion object {
        const val KEY_LAST_SEEN = "last_seen_id"
        const val KEY_ASKED = "permission_asked"
    }
}
