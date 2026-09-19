package com.acrovox.core.download

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/** Planifie les téléchargements en arrière-plan. */
interface DownloadScheduler {
    /** @param wifiOnly attendre un réseau non facturé à l'usage. */
    fun enqueue(episodeId: Long, wifiOnly: Boolean)

    fun cancel(episodeId: Long)
}

internal class WorkManagerDownloadScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context
) : DownloadScheduler {
    override fun enqueue(episodeId: Long, wifiOnly: Boolean) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .setRequiresStorageNotLow(true)
            .build()
        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
            .setInputData(
                workDataOf(DownloadWorker.KEY_EPISODE_ID to episodeId, DownloadWorker.KEY_WIFI_ONLY to wifiOnly)
            )
            .addTag(TAG)
            .build()
        // REPLACE : « télécharger maintenant » remplace une attente du Wi-Fi.
        WorkManager.getInstance(context).enqueueUniqueWork(nameOf(episodeId), ExistingWorkPolicy.REPLACE, request)
    }

    override fun cancel(episodeId: Long) {
        WorkManager.getInstance(context).cancelUniqueWork(nameOf(episodeId))
    }

    private fun nameOf(episodeId: Long) = "download-$episodeId"

    private companion object {
        const val TAG = "download"
        const val BACKOFF_SECONDS = 30L
    }
}
