package com.acrovox.core.data.repository

import androidx.room.withTransaction
import com.acrovox.core.database.AcroVoxDatabase
import com.acrovox.core.database.entity.EpisodeWithFeed
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class EpisodeSort { NEWEST_FIRST, OLDEST_FIRST }

@Singleton
class EpisodeRepository @Inject constructor(private val db: AcroVoxDatabase) {
    private val episodeDao get() = db.episodeDao()
    private val queueDao get() = db.queueDao()

    /** Derniers épisodes des abonnements, épisodes ignorés exclus. */
    fun observeLatest(sort: EpisodeSort, limit: Int = LATEST_LIMIT): Flow<List<EpisodeWithFeed>> = when (sort) {
        EpisodeSort.NEWEST_FIRST -> episodeDao.observeLatest(limit)
        EpisodeSort.OLDEST_FIRST -> episodeDao.observeOldest(limit)
    }

    /** Épisode commencé le plus récemment écouté, pour « Reprendre l'écoute ». */
    fun observeResume(): Flow<EpisodeWithFeed?> = episodeDao.observeInProgress(1).map { it.firstOrNull() }

    fun observeQueuedIds(): Flow<Set<Long>> = queueDao.observeEpisodeIds().map { it.toSet() }

    /**
     * Ajoute à la file. Un épisode de la boîte ou du catalogue devient « gardé » ([com.acrovox.core.model.EpisodeState.UNPLAYED]).
     * Ne lance aucun téléchargement.
     */
    suspend fun addToQueue(episodeIds: List<Long>, first: Boolean = false) = db.withTransaction {
        queueDao.add(episodeIds, first)
        episodeDao.markKept(episodeIds)
    }

    suspend fun removeFromQueue(episodeIds: List<Long>) = queueDao.remove(episodeIds)

    private companion object {
        const val LATEST_LIMIT = 50
    }
}
