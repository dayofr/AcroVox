package com.acrovox.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.acrovox.core.database.entity.EpisodeWithFeed
import com.acrovox.core.database.entity.QueueItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class QueueDao {
    @Transaction
    @Query(
        """
        SELECT episode.* FROM episode
        INNER JOIN queue_item ON queue_item.episode_id = episode.id
        ORDER BY queue_item.position
        """
    )
    abstract fun observeQueue(): Flow<List<EpisodeWithFeed>>

    @Query("SELECT episode_id FROM queue_item ORDER BY position")
    abstract suspend fun getEpisodeIds(): List<Long>

    @Query("SELECT episode_id FROM queue_item ORDER BY position")
    abstract fun observeEpisodeIds(): Flow<List<Long>>

    @Query("SELECT EXISTS(SELECT 1 FROM queue_item WHERE episode_id = :episodeId)")
    abstract fun observeContains(episodeId: Long): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertAll(items: List<QueueItemEntity>)

    @Query("DELETE FROM queue_item")
    abstract suspend fun clear()

    /** Réécrit toute la file dans l'ordre donné. */
    @Transaction
    open suspend fun replace(episodeIds: List<Long>) {
        clear()
        insertAll(episodeIds.distinct().mapIndexed { index, id -> QueueItemEntity(id, index) })
    }

    /** Ajoute en fin de file, ou en tête si [first]. Sans effet sur un épisode déjà présent. */
    @Transaction
    open suspend fun add(episodeIds: List<Long>, first: Boolean = false) {
        val current = getEpisodeIds()
        val added = episodeIds.filterNot { it in current }
        replace(if (first) added + current else current + added)
    }

    @Transaction
    open suspend fun remove(episodeIds: List<Long>) {
        replace(getEpisodeIds().filterNot { it in episodeIds })
    }

    @Transaction
    open suspend fun move(from: Int, to: Int) {
        val ids = getEpisodeIds().toMutableList()
        if (from !in ids.indices || to !in ids.indices) return
        ids.add(to, ids.removeAt(from))
        replace(ids)
    }
}
