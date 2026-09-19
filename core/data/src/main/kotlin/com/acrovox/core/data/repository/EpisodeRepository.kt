package com.acrovox.core.data.repository

import androidx.room.withTransaction
import com.acrovox.core.database.AcroVoxDatabase
import com.acrovox.core.database.entity.EpisodeActionEntity
import com.acrovox.core.database.entity.EpisodeEntity
import com.acrovox.core.database.entity.EpisodeWithFeed
import com.acrovox.core.database.entity.PlaybackHistoryEntity
import com.acrovox.core.model.EpisodeActionType
import com.acrovox.core.model.EpisodeState
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class EpisodeSort { NEWEST_FIRST, OLDEST_FIRST }

/** Filtres de la page podcast. */
enum class EpisodeFilter(val states: List<EpisodeState>) {
    ALL(EpisodeState.entries - EpisodeState.IGNORED),
    UNPLAYED(listOf(EpisodeState.NEW, EpisodeState.AVAILABLE, EpisodeState.UNPLAYED, EpisodeState.IN_PROGRESS)),
    INBOX(listOf(EpisodeState.NEW)),
    PLAYED(listOf(EpisodeState.PLAYED)),
    IGNORED(listOf(EpisodeState.IGNORED))
}

@Singleton
class EpisodeRepository @Inject constructor(private val db: AcroVoxDatabase, private val clock: Clock) {
    private val episodeDao get() = db.episodeDao()
    private val queueDao get() = db.queueDao()

    /** Derniers épisodes des abonnements, épisodes ignorés exclus. */
    fun observeLatest(sort: EpisodeSort, limit: Int = LATEST_LIMIT): Flow<List<EpisodeWithFeed>> = when (sort) {
        EpisodeSort.NEWEST_FIRST -> episodeDao.observeLatest(limit)
        EpisodeSort.OLDEST_FIRST -> episodeDao.observeOldest(limit)
    }

    fun observeByFeed(feedId: Long, filter: EpisodeFilter): Flow<List<EpisodeEntity>> =
        episodeDao.observeByFeedAndStates(feedId, filter.states)

    fun observeEpisode(id: Long): Flow<EpisodeWithFeed?> = episodeDao.observe(id)

    /** Épisode commencé le plus récemment écouté, pour « Reprendre l'écoute ». */
    fun observeResume(): Flow<EpisodeWithFeed?> = episodeDao.observeInProgress(1).map { it.firstOrNull() }

    fun observeFavorites(): Flow<List<EpisodeWithFeed>> = episodeDao.observeFavorites()

    /** Épisodes écoutés, du plus récent au plus ancien, un par ligne. */
    fun observeHistory(): Flow<List<EpisodeWithFeed>> = db.playbackHistoryDao().observeHistory()

    suspend fun clearHistory() = db.playbackHistoryDao().clear()

    fun observeQueuedIds(): Flow<Set<Long>> = queueDao.observeEpisodeIds().map { it.toSet() }

    /**
     * Ajoute à la file. Un épisode de la boîte ou du catalogue devient « gardé » ([EpisodeState.UNPLAYED]).
     * Ne lance aucun téléchargement.
     */
    suspend fun addToQueue(episodeIds: List<Long>, first: Boolean = false) = db.withTransaction {
        queueDao.add(episodeIds, first)
        episodeDao.markKept(episodeIds)
    }

    suspend fun removeFromQueue(episodeIds: List<Long>) = queueDao.remove(episodeIds)

    fun observeQueue(): Flow<List<EpisodeWithFeed>> = queueDao.observeQueue()

    /** Réordonne toute la file (après un glisser-déposer). */
    suspend fun reorderQueue(episodeIds: List<Long>) = queueDao.replace(episodeIds)

    suspend fun clearQueue() = queueDao.clear()

    suspend fun shuffleQueue() = queueDao.replace(queueDao.getEpisodeIds().shuffled())

    // Boîte de réception

    fun observeInbox(): Flow<List<EpisodeWithFeed>> = episodeDao.observeInbox()

    fun observeInboxCount(): Flow<Int> = episodeDao.observeInbox().map { it.size }

    /** États actuels, pour pouvoir annuler une action. */
    suspend fun statesOf(episodeIds: List<Long>): Map<Long, EpisodeState> =
        episodeDao.getWithFeed(episodeIds).associate { it.episode.id to it.episode.state }

    /** Annule un « garder » : l'épisode quitte la file et reprend son état d'avant. */
    suspend fun undoKeep(previous: Map<Long, EpisodeState>) = db.withTransaction {
        queueDao.remove(previous.keys.toList())
        previous.entries.groupBy({ it.value }, { it.key }).forEach { (state, ids) -> episodeDao.setState(ids, state) }
    }

    /**
     * Écarte des épisodes : état [EpisodeState.IGNORED], retirés de la file, action gPodder `delete`
     * enregistrée pour la synchronisation.
     *
     * @return identifiants des actions enregistrées, pour pouvoir annuler.
     */
    suspend fun ignore(episodeIds: List<Long>): List<Long> = db.withTransaction {
        val episodes = episodeDao.getWithFeed(episodeIds).filter { it.episode.state != EpisodeState.IGNORED }
        val ids = episodes.map { it.episode.id }
        episodeDao.setState(ids, EpisodeState.IGNORED)
        queueDao.remove(ids)
        record(episodes, EpisodeActionType.DELETE)
    }

    /** Annule [ignore] : états d'origine rétablis, actions retirées avant envoi. */
    suspend fun undoIgnore(previous: Map<Long, EpisodeState>, actionIds: List<Long>) = db.withTransaction {
        previous.entries.groupBy({ it.value }, { it.key }).forEach { (state, ids) -> episodeDao.setState(ids, state) }
        db.episodeActionDao().delete(actionIds)
    }

    /** Reprend un épisode ignoré : il redevient « gardé », action gPodder `new`. */
    suspend fun restore(episodeIds: List<Long>) = db.withTransaction {
        val episodes = episodeDao.getWithFeed(episodeIds).filter { it.episode.state == EpisodeState.IGNORED }
        episodeDao.setState(episodes.map { it.episode.id }, EpisodeState.UNPLAYED)
        record(episodes, EpisodeActionType.NEW)
    }

    /** Marque écouté : action gPodder `play` jusqu'à la fin si la durée est connue. */
    suspend fun markPlayed(episodeIds: List<Long>) = db.withTransaction {
        val now = clock.millis()
        val episodes = episodeDao.getWithFeed(episodeIds)
        episodes.forEach { episodeDao.markPlayed(it.episode.id, now) }
        queueDao.remove(episodeIds)
        record(episodes.filter { it.episode.durationMs != null }, EpisodeActionType.PLAY)
    }

    suspend fun setFavorite(episodeId: Long, favorite: Boolean) = episodeDao.setFavorite(episodeId, favorite)

    // Lecture

    suspend fun getWithFeed(episodeId: Long): EpisodeWithFeed? = episodeDao.getWithFeed(listOf(episodeId)).firstOrNull()

    /** Début d'écoute : entrée d'historique. */
    suspend fun onPlaybackStarted(episodeId: Long, positionMs: Long) {
        db.playbackHistoryDao().insert(
            PlaybackHistoryEntity(episodeId = episodeId, playedAt = clock.millis(), positionMs = positionMs)
        )
    }

    /** Position courante ; l'épisode passe « en cours » s'il n'est pas déjà écouté. */
    suspend fun savePosition(episodeId: Long, positionMs: Long) =
        episodeDao.updatePosition(episodeId, positionMs, clock.millis())

    /**
     * Segment d'écoute terminé (pause, arrêt, changement d'épisode) : action gPodder `play`
     * de [startedMs] à [positionMs].
     */
    suspend fun recordListening(episodeId: Long, startedMs: Long, positionMs: Long, totalMs: Long? = null) {
        if (positionMs <= startedMs) return
        val item = getWithFeed(episodeId) ?: return
        val (episode, feed) = item
        db.episodeActionDao().insertAll(
            listOf(
                EpisodeActionEntity(
                    podcastUrl = feed.feedUrl,
                    episodeUrl = episode.mediaUrl,
                    guid = episode.guid,
                    action = EpisodeActionType.PLAY,
                    timestamp = clock.millis(),
                    started = (startedMs / 1000).toInt(),
                    position = (positionMs / 1000).toInt(),
                    total = (totalMs ?: episode.durationMs)?.div(1000)?.toInt()
                )
            )
        )
    }

    /** Fin d'épisode : écouté, retiré de la file. */
    suspend fun complete(episodeId: Long) = db.withTransaction {
        episodeDao.markPlayed(episodeId, clock.millis())
        queueDao.remove(listOf(episodeId))
    }

    /** Épisode à lire après [episodeId] : le suivant dans la file, ou la tête de file. */
    suspend fun nextInQueue(episodeId: Long): Long? {
        val queue = queueDao.getEpisodeIds()
        val index = queue.indexOf(episodeId)
        return if (index >= 0) queue.getOrNull(index + 1) else queue.firstOrNull { it != episodeId }
    }

    suspend fun previousInQueue(episodeId: Long): Long? {
        val queue = queueDao.getEpisodeIds()
        val index = queue.indexOf(episodeId)
        return if (index > 0) queue[index - 1] else null
    }

    private suspend fun record(episodes: List<EpisodeWithFeed>, type: EpisodeActionType): List<Long> {
        if (episodes.isEmpty()) return emptyList()
        val now = clock.millis()
        return db.episodeActionDao().insertAll(
            episodes.map { (episode, feed) ->
                val totalSeconds = episode.durationMs?.div(1000)?.toInt()
                EpisodeActionEntity(
                    podcastUrl = feed.feedUrl,
                    episodeUrl = episode.mediaUrl,
                    guid = episode.guid,
                    action = type,
                    timestamp = now,
                    started = if (type == EpisodeActionType.PLAY) 0 else null,
                    position = if (type == EpisodeActionType.PLAY) totalSeconds else null,
                    total = if (type == EpisodeActionType.PLAY) totalSeconds else null
                )
            }
        )
    }

    private companion object {
        const val LATEST_LIMIT = 50
    }
}
