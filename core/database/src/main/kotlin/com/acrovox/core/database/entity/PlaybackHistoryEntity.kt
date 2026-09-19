package com.acrovox.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "playback_history",
    foreignKeys = [
        ForeignKey(
            entity = EpisodeEntity::class,
            parentColumns = ["id"],
            childColumns = ["episode_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["episode_id"]), Index(value = ["played_at"])]
)
data class PlaybackHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "episode_id") val episodeId: Long,
    @ColumnInfo(name = "played_at") val playedAt: Long,
    @ColumnInfo(name = "position_ms") val positionMs: Long
)
