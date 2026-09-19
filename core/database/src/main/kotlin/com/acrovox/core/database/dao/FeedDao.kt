package com.acrovox.core.database.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.acrovox.core.database.entity.FeedEntity
import kotlinx.coroutines.flow.Flow

/** Podcast et nombre d'épisodes dans la boîte de réception. */
data class FeedWithNewCount(@Embedded val feed: FeedEntity, @ColumnInfo(name = "new_count") val newCount: Int)

@Dao
interface FeedDao {
    @Query(
        """
        SELECT feed.*, (SELECT COUNT(*) FROM episode WHERE episode.feed_id = feed.id AND episode.state = 'NEW') AS new_count
        FROM feed ORDER BY title COLLATE NOCASE
        """
    )
    fun observeAllWithNewCount(): Flow<List<FeedWithNewCount>>

    @Query("SELECT * FROM feed ORDER BY title COLLATE NOCASE")
    suspend fun getAll(): List<FeedEntity>

    @Query("SELECT * FROM feed WHERE id = :id")
    fun observe(id: Long): Flow<FeedEntity?>

    @Query("SELECT * FROM feed WHERE id = :id")
    suspend fun get(id: Long): FeedEntity?

    @Query("SELECT * FROM feed WHERE feed_url = :url")
    suspend fun getByUrl(url: String): FeedEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(feed: FeedEntity): Long

    @Update
    suspend fun update(feed: FeedEntity)

    @Query("UPDATE feed SET last_refresh_at = :at, etag = :etag, last_modified = :lastModified WHERE id = :id")
    suspend fun updateRefresh(id: Long, at: Long, etag: String?, lastModified: String?)

    @Query("DELETE FROM feed WHERE id = :id")
    suspend fun delete(id: Long)
}
