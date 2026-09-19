package com.acrovox.core.download

import com.acrovox.core.database.AcroVoxDatabase
import com.acrovox.core.database.entity.DownloadWithEpisode
import com.acrovox.core.model.DownloadStatus
import com.acrovox.core.model.EpisodeState
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

/**
 * Libère de la place : supprime le fichier d'un épisode ignoré, et celui d'un épisode écouté
 * selon [DownloadSettings.deleteAfterPlayed]. Les favoris sont toujours gardés.
 * N'enregistre aucune action gPodder : l'état de l'épisode ne change pas.
 */
@Singleton
class DownloadCleaner @Inject constructor(
    private val db: AcroVoxDatabase,
    private val settings: DownloadSettingsRepository,
    private val downloads: DownloadManager,
    private val clock: Clock
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** @return nombre de fichiers supprimés. */
    suspend fun clean(): Int {
        val policy = settings.current().deleteAfterPlayed
        val now = clock.millis()
        val ids = db.downloadDao().getWithEpisodes()
            .filter { shouldDelete(it, policy, now) }
            .map { it.download.episodeId }
        downloads.delete(ids)
        return ids.size
    }

    /**
     * Nettoie à chaque changement d'état, après [GRACE_MS] : le temps d'annuler un « ignorer »
     * ou un « marquer écouté » depuis le bandeau.
     */
    @OptIn(FlowPreview::class)
    fun start() {
        scope.launch {
            combine(db.downloadDao().observeWithEpisodes(), settings.settings) { _, _ -> }
                .debounce(GRACE_MS)
                .collect { clean() }
        }
    }

    companion object {
        const val GRACE_MS = 10_000L
    }
}

internal fun shouldDelete(item: DownloadWithEpisode, policy: CleanupDelay, now: Long): Boolean {
    val episode = item.episode.episode
    if (episode.isFavorite) return false
    if (episode.state == EpisodeState.IGNORED) return true
    if (item.download.status != DownloadStatus.COMPLETED || episode.state != EpisodeState.PLAYED) return false
    val delay = policy.delayMs ?: return false
    return (episode.completedAt ?: 0) + delay <= now
}
