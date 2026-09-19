package com.acrovox.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.acrovox.core.database.entity.EpisodeWithFeed
import com.acrovox.core.database.entity.PlaybackHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaybackHistoryDao {
    @Insert
    suspend fun insert(entry: PlaybackHistoryEntity)

    /** Un épisode par ligne, du plus récemment écouté au plus ancien. */
    @Transaction
    @Query(
        """
        SELECT episode.* FROM episode
        INNER JOIN (SELECT episode_id, MAX(played_at) AS last_played_at FROM playback_history GROUP BY episode_id) h
            ON h.episode_id = episode.id
        ORDER BY h.last_played_at DESC
        """
    )
    fun observeHistory(): Flow<List<EpisodeWithFeed>>

    @Query("DELETE FROM playback_history")
    suspend fun clear()
}
