package ng.name.servix.app.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import ng.name.servix.app.MainActivity
import ng.name.servix.app.R
import ng.name.servix.app.data.model.NotificationDto

/**
 * Polling model (no Firebase): the worker fetches unread items and posts one system notification
 * per new item, grouped under the "Bookings & account" channel. Title/body come from the API.
 */
class ServixNotifier(private val context: Context) {

    fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notifications_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.notifications_channel_description)
        }
        manager.createNotificationChannel(channel)
    }

    fun canPost(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    suspend fun postNew(items: List<NotificationDto>) {
        if (items.isEmpty() || !canPost()) return
        ensureChannel()
        val manager = NotificationManagerCompat.from(context)
        items.forEach { item ->
            val intent = Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                data = item.deepLink()
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val pending = PendingIntent.getActivity(
                context,
                item.id.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(item.title)
                .setContentText(item.body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(item.body))
                .setAutoCancel(true)
                .setGroup(GROUP_KEY)
                .setContentIntent(pending)
                .build()
            runCatching { manager.notify(item.id.hashCode(), notification) }
        }
        val summary = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.notifications_title))
            .setGroup(GROUP_KEY)
            .setGroupSummary(true)
            .build()
        runCatching { manager.notify(GROUP_SUMMARY_ID, summary) }
    }

    private fun NotificationDto.deepLink(): Uri {
        // `link` is the website path for the item (e.g. /bookings/<id>); booking ids deep-link
        // straight to Booking detail, everything else opens the Notifications screen.
        val path = link.orEmpty()
        val bookingId = Regex("/bookings/([^/?#]+)").find(path)?.groupValues?.get(1)
        return if (!bookingId.isNullOrBlank()) {
            Uri.parse("servix://booking/$bookingId")
        } else {
            Uri.parse("servix://notifications")
        }
    }

    companion object {
        const val CHANNEL_ID = "bookings_account"
        const val GROUP_KEY = "ng.name.servix.app.notifications"
        private const val GROUP_SUMMARY_ID = 1001
    }
}
