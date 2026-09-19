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

/** Podcast, nombre d'épisodes dans la boîte de réception et date du dernier épisode. */
data class FeedWithNewCount(
    @Embedded val feed: FeedEntity,
    @ColumnInfo(name = "new_count") val newCount: Int,
    @ColumnInfo(name = "last_pub_date") val lastPubDate: Long? = null
)

@Dao
interface FeedDao {
    @Query(
        """
        SELECT feed.*,
            (SELECT COUNT(*) FROM episode WHERE episode.feed_id = feed.id AND episode.state = 'NEW') AS new_count,
            (SELECT MAX(pub_date) FROM episode WHERE episode.feed_id = feed.id) AS last_pub_date
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

    @Query("SELECT * FROM feed WHERE feed_url = :url")
    fun observeByUrl(url: String): Flow<FeedEntity?>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(feed: FeedEntity): Long

    @Update
    suspend fun update(feed: FeedEntity)

    @Query("UPDATE feed SET last_refresh_at = :at, etag = :etag, last_modified = :lastModified WHERE id = :id")
    suspend fun updateRefresh(id: Long, at: Long, etag: String?, lastModified: String?)

    @Query(
        """
        UPDATE feed SET title = :title, author = :author, description = :description, image_url = :imageUrl,
            link = :link, language = :language, categories = :categories
        WHERE id = :id
        """
    )
    suspend fun updateMetadata(
        id: Long,
        title: String,
        author: String?,
        description: String?,
        imageUrl: String?,
        link: String?,
        language: String?,
        categories: String?
    )

    @Query("UPDATE feed SET feed_url = :url WHERE id = :id")
    suspend fun updateUrl(id: Long, url: String)

    @Query("DELETE FROM feed WHERE id = :id")
    suspend fun delete(id: Long)
}
