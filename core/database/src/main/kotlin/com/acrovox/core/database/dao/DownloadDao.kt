package com.acrovox.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.acrovox.core.database.entity.DownloadEntity
import com.acrovox.core.database.entity.DownloadWithEpisode
import com.acrovox.core.database.entity.EpisodeWithFeed
import com.acrovox.core.model.DownloadStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Query("SELECT * FROM download")
    fun observeAll(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM download WHERE episode_id = :episodeId")
    fun observe(episodeId: Long): Flow<DownloadEntity?>

    @Query("SELECT * FROM download WHERE episode_id = :episodeId")
    suspend fun get(episodeId: Long): DownloadEntity?

    @Query("SELECT * FROM download WHERE episode_id IN (:episodeIds)")
    suspend fun get(episodeIds: List<Long>): List<DownloadEntity>

    /** Téléchargements avec leur épisode, du plus récent au plus ancien. */
    @Transaction
    @Query("SELECT * FROM download ORDER BY COALESCE(completed_at, created_at) DESC")
    fun observeWithEpisodes(): Flow<List<DownloadWithEpisode>>

    @Transaction
    @Query("SELECT * FROM download")
    suspend fun getWithEpisodes(): List<DownloadWithEpisode>

    @Transaction
    @Query(
        """
        SELECT episode.* FROM episode
        INNER JOIN download ON download.episode_id = episode.id
        WHERE download.status = 'COMPLETED'
        ORDER BY download.completed_at DESC
        """
    )
    fun observeCompletedEpisodes(): Flow<List<EpisodeWithFeed>>

    @Query("SELECT COALESCE(SUM(total_bytes), 0) FROM download WHERE status = 'COMPLETED'")
    fun observeUsedBytes(): Flow<Long>

    @Upsert
    suspend fun upsert(download: DownloadEntity)

    @Query(
        "UPDATE download SET status = :status, bytes_downloaded = :bytes, total_bytes = :total WHERE episode_id = :episodeId"
    )
    suspend fun updateProgress(episodeId: Long, status: DownloadStatus, bytes: Long, total: Long?)

    @Query("UPDATE download SET status = :status, error = :error WHERE episode_id = :episodeId")
    suspend fun setStatus(episodeId: Long, status: DownloadStatus, error: String? = null)

    @Query(
        """
        UPDATE download SET status = 'COMPLETED', local_path = :path, bytes_downloaded = :bytes,
        total_bytes = :bytes, error = NULL, completed_at = :completedAt WHERE episode_id = :episodeId
        """
    )
    suspend fun complete(episodeId: Long, path: String, bytes: Long, completedAt: Long)

    @Query("DELETE FROM download WHERE episode_id = :episodeId")
    suspend fun delete(episodeId: Long)
}
