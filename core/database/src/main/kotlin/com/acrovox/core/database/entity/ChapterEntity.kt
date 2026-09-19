package com.acrovox.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chapter",
    foreignKeys = [
        ForeignKey(
            entity = EpisodeEntity::class,
            parentColumns = ["id"],
            childColumns = ["episode_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["episode_id", "start_ms"])]
)
data class ChapterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "episode_id") val episodeId: Long,
    @ColumnInfo(name = "start_ms") val startMs: Long,
    val title: String,
    @ColumnInfo(name = "image_url") val imageUrl: String? = null,
    val url: String? = null
)
