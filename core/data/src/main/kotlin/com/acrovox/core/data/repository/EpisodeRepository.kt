package com.acrovox.core.data.repository

import androidx.room.withTransaction
import com.acrovox.core.database.AcroVoxDatabase
import com.acrovox.core.database.entity.EpisodeActionEntity
import com.acrovox.core.database.entity.EpisodeEntity
import com.acrovox.core.database.entity.EpisodeWithFeed
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
