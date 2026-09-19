package com.acrovox.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.acrovox.core.database.entity.ChapterEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ChapterDao {
    @Query("SELECT * FROM chapter WHERE episode_id = :episodeId ORDER BY start_ms")
    abstract fun observe(episodeId: Long): Flow<List<ChapterEntity>>

    @Query("DELETE FROM chapter WHERE episode_id = :episodeId")
    protected abstract suspend fun deleteForEpisode(episodeId: Long)

    @Insert
    protected abstract suspend fun insertAll(chapters: List<ChapterEntity>)

    @Transaction
    open suspend fun replace(episodeId: Long, chapters: List<ChapterEntity>) {
        deleteForEpisode(episodeId)
        insertAll(chapters.map { it.copy(id = 0, episodeId = episodeId) })
    }
}
