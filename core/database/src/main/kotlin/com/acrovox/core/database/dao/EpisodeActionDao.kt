package com.acrovox.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.acrovox.core.database.entity.EpisodeActionEntity

@Dao
interface EpisodeActionDao {
    @Insert
    suspend fun insertAll(actions: List<EpisodeActionEntity>): List<Long>

    @Query("SELECT * FROM episode_action ORDER BY timestamp, id LIMIT :limit")
    suspend fun getPending(limit: Int): List<EpisodeActionEntity>

    @Query("SELECT COUNT(*) FROM episode_action")
    suspend fun count(): Int

    /** Retire des actions envoyées, ou annulées par « Annuler ». */
    @Query("DELETE FROM episode_action WHERE id IN (:ids)")
    suspend fun delete(ids: List<Long>)
}
