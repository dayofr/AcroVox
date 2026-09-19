package com.acrovox.core.data.refresh

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Notifie les nouveaux épisodes des podcasts dont les notifications sont activées
 * (réglage par podcast, désactivé par défaut).
 */
class NewEpisodesNotifier @Inject constructor(@param:ApplicationContext private val context: Context) {
    fun notify(summary: RefreshSummary) {
        val notified = summary.newEpisodes.filterKeys { it.notifyNewEpisodes }
        if (notified.isEmpty() || !canNotify()) return
        ensureChannel()
        val total = notified.values.sum()
        val title = if (total == 1) "1 nouvel épisode" else "$total nouveaux épisodes"
        val text = notified.keys.joinToString(", ") { it.title }
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_sync_noanim)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .apply {
                if (launch != null) {
                    setContentIntent(
                        android.app.PendingIntent.getActivity(
                            context,
                            0,
                            launch,
                            android.app.PendingIntent.FLAG_IMMUTABLE
                        )
                    )
                }
            }
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    private fun canNotify(): Boolean {
        val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        return granted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    private fun ensureChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "Nouveaux épisodes", NotificationManager.IMPORTANCE_DEFAULT)
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private companion object {
        const val CHANNEL_ID = "new_episodes"
        const val NOTIFICATION_ID = 1001
    }
}
