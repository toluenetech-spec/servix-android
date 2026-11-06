package ng.name.servix.app.notifications

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit
import ng.name.servix.app.di.AppContainer

/**
 * Polls GET /account/notifications every 15 minutes while signed in (network constraint) and posts
 * one system notification per new unread item. Approximate 15-minute latency; this is polling, not push.
 */
class NotificationsWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val container = AppContainer.getInstance(applicationContext)
        if (container.tokenStore.getAccessToken() == null) return Result.success()

        val prefs = NotificationPrefs(applicationContext)
        val notifier = ServixNotifier(applicationContext)
        notifier.ensureChannel()

        val list = container.accountRepository.getNotifications().getOrNull() ?: return Result.retry()
        val unread = list.items.filter { it.readAt == null }
        val lastSeen = prefs.lastSeenId
        val fresh = if (lastSeen == null) {
            unread.take(1)
        } else {
            unread.takeWhile { it.id != lastSeen }
        }
        if (fresh.isNotEmpty()) {
            notifier.postNew(fresh)
            prefs.lastSeenId = fresh.first().id
        }
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "servix-notifications-poll"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<NotificationsWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
