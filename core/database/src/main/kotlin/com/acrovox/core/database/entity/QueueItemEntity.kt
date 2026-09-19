package com.acrovox.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "queue_item",
    foreignKeys = [
        ForeignKey(
            entity = EpisodeEntity::class,
            parentColumns = ["id"],
            childColumns = ["episode_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["position"])]
)
data class QueueItemEntity(
    @PrimaryKey @ColumnInfo(name = "episode_id") val episodeId: Long,
    /** Rang dans la file, à partir de 0, sans trou. */
    val position: Int
)
