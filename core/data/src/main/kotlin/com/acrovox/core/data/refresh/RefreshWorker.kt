package com.acrovox.core.data.refresh

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/** Rafraîchissement en arrière-plan, puis notification des nouveaux épisodes. */
@HiltWorker
class RefreshWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: RefreshRepository,
    private val notifier: NewEpisodesNotifier
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val summary = repository.refreshAll()
        notifier.notify(summary)
        return Result.success()
    }

    companion object {
        private const val PERIODIC_NAME = "refresh-periodic"
        private const val NOW_NAME = "refresh-now"
        const val DEFAULT_INTERVAL_HOURS = 1L

        private val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        /** Planifie le rafraîchissement périodique ; sans effet s'il l'est déjà avec le même intervalle. */
        fun schedule(context: Context, intervalHours: Long = DEFAULT_INTERVAL_HOURS) {
            val request = PeriodicWorkRequestBuilder<RefreshWorker>(intervalHours, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(
                context
            ).enqueueUniquePeriodicWork(PERIODIC_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
        }

        /** Rafraîchit dès que le réseau est disponible, même application fermée. */
        fun runNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<RefreshWorker>().setConstraints(constraints).build()
            WorkManager.getInstance(context).enqueueUniqueWork(NOW_NAME, ExistingWorkPolicy.KEEP, request)
        }
    }
}
