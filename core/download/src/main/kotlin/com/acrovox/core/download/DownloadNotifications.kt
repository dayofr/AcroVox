package com.acrovox.core.download

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.ForegroundInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Notification de progression d'un téléchargement, une par épisode. */
class DownloadNotifications @Inject constructor(@param:ApplicationContext private val context: Context) {
    fun foregroundInfo(episodeId: Long, title: String, bytes: Long, total: Long?): ForegroundInfo {
        ensureChannel()
        val notification = build(title, bytes, total)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(idOf(episodeId), notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(idOf(episodeId), notification)
        }
    }

    fun update(episodeId: Long, title: String, bytes: Long, total: Long?) {
        if (!canNotify()) return
        NotificationManagerCompat.from(context).notify(idOf(episodeId), build(title, bytes, total))
    }

    fun cancel(episodeId: Long) = NotificationManagerCompat.from(context).cancel(idOf(episodeId))

    private fun build(title: String, bytes: Long, total: Long?) = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.stat_sys_download)
        .setContentTitle(title)
        .setContentText("Téléchargement")
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setSilent(true)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .apply {
            if (total != null && total > 0) {
                setProgress(PROGRESS_MAX, (bytes * PROGRESS_MAX / total).toInt(), false)
            } else {
                setProgress(0, 0, true)
            }
            context.packageManager.getLaunchIntentForPackage(context.packageName)?.let {
                setContentIntent(PendingIntent.getActivity(context, 0, it, PendingIntent.FLAG_IMMUTABLE))
            }
        }
        .build()

    private fun canNotify(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

    private fun ensureChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "Téléchargements", NotificationManager.IMPORTANCE_LOW)
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun idOf(episodeId: Long) = NOTIFICATION_BASE + (episodeId % NOTIFICATION_RANGE).toInt()

    private companion object {
        const val CHANNEL_ID = "downloads"
        const val NOTIFICATION_BASE = 20_000
        const val NOTIFICATION_RANGE = 1_000_000
        const val PROGRESS_MAX = 1000
    }
}
