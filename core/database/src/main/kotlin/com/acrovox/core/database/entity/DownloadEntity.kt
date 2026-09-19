package com.acrovox.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.acrovox.core.model.DownloadStatus

@Entity(
    tableName = "download",
    foreignKeys = [
        ForeignKey(
            entity = EpisodeEntity::class,
            parentColumns = ["id"],
            childColumns = ["episode_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class DownloadEntity(
    @PrimaryKey @ColumnInfo(name = "episode_id") val episodeId: Long,
    val status: DownloadStatus,
    @ColumnInfo(name = "local_path") val localPath: String? = null,
    @ColumnInfo(name = "bytes_downloaded") val bytesDownloaded: Long = 0,
    @ColumnInfo(name = "total_bytes") val totalBytes: Long? = null,
    val error: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "completed_at") val completedAt: Long? = null
)
