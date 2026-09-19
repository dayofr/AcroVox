package com.acrovox.core.download

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.acrovox.core.database.AcroVoxDatabase
import com.acrovox.core.database.entity.EpisodeActionEntity
import com.acrovox.core.model.DownloadStatus
import com.acrovox.core.model.EpisodeActionType
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.IOException
import java.time.Clock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

/**
 * Télécharge un épisode. Interrompu (perte du Wi-Fi, arrêt du système), il repart plus tard
 * là où il s'était arrêté. Les erreurs réseau sont réessayées [MAX_ATTEMPTS] fois.
 */
@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val db: AcroVoxDatabase,
    private val downloader: EpisodeDownloader,
    private val files: DownloadFiles,
    private val notifications: DownloadNotifications,
    private val limiter: DownloadLimiter,
    private val clock: Clock
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val id = inputData.getLong(KEY_EPISODE_ID, -1)
        val wifiOnly = inputData.getBoolean(KEY_WIFI_ONLY, false)
        val dao = db.downloadDao()
        // Annulé entre-temps : la ligne n'existe plus.
        if (dao.get(id) == null) return Result.success()
        val episode = db.episodeDao().get(id) ?: return Result.failure().also { dao.delete(id) }
        val waiting = if (wifiOnly) DownloadStatus.WAITING_FOR_WIFI else DownloadStatus.QUEUED

        return limiter.permits.withPermit {
            try {
                runCatching { setForeground(notifications.foregroundInfo(id, episode.title, 0, null)) }
                dao.setStatus(id, DownloadStatus.RUNNING)
                val part = files.partFile(id)
                val bytes = downloader.download(episode.mediaUrl, part, files::freeBytes) { done, total ->
                    dao.updateProgress(id, DownloadStatus.RUNNING, done, total)
                    notifications.update(id, episode.title, done, total)
                }
                val target = files.finalFile(id, episode.mediaUrl, episode.mediaType)
                if (!part.renameTo(target)) throw IOException("Impossible d'enregistrer le fichier")
                dao.complete(id, target.path, bytes, clock.millis())
                recordDownloadAction(id)
                Result.success()
            } catch (e: CancellationException) {
                // Arrêt par le système ou annulation : on attend la prochaine exécution.
                withContext(NonCancellable) { dao.setStatus(id, waiting) }
                throw e
            } catch (e: PermanentDownloadException) {
                dao.setStatus(id, DownloadStatus.FAILED, e.message)
                Result.failure()
            } catch (e: IOException) {
                if (runAttemptCount + 1 >= MAX_ATTEMPTS) {
                    dao.setStatus(id, DownloadStatus.FAILED, e.message ?: "Erreur réseau")
                    Result.failure()
                } else {
                    dao.setStatus(id, waiting, e.message)
                    Result.retry()
                }
            } finally {
                notifications.cancel(id)
            }
        }
    }

    /** Action gPodder `download`, envoyée à la prochaine synchronisation. */
    private suspend fun recordDownloadAction(episodeId: Long) {
        val (episode, feed) = db.episodeDao().getWithFeed(listOf(episodeId)).firstOrNull() ?: return
        db.episodeActionDao().insertAll(
            listOf(
                EpisodeActionEntity(
                    podcastUrl = feed.feedUrl,
                    episodeUrl = episode.mediaUrl,
                    guid = episode.guid,
                    action = EpisodeActionType.DOWNLOAD,
                    timestamp = clock.millis()
                )
            )
        )
    }

    companion object {
        const val KEY_EPISODE_ID = "episode_id"
        const val KEY_WIFI_ONLY = "wifi_only"
        const val MAX_ATTEMPTS = 5
    }
}
