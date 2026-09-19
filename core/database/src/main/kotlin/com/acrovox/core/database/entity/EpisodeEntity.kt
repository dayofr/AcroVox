package com.acrovox.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.acrovox.core.model.EpisodeState

@Entity(
    tableName = "episode",
    foreignKeys = [
        ForeignKey(
            entity = FeedEntity::class,
            parentColumns = ["id"],
            childColumns = ["feed_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["feed_id", "guid"], unique = true),
        Index(value = ["pub_date"]),
        Index(value = ["state"]),
        Index(value = ["media_url"])
    ]
)
data class EpisodeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "feed_id") val feedId: Long,
    /** guid du flux, ou URL média si le flux n'en fournit pas. */
    val guid: String,
    val title: String,
    val description: String? = null,
    val link: String? = null,
    /** Date de publication, epoch en millisecondes. */
    @ColumnInfo(name = "pub_date") val pubDate: Long,
    @ColumnInfo(name = "duration_ms") val durationMs: Long? = null,
    @ColumnInfo(name = "media_url") val mediaUrl: String,
    @ColumnInfo(name = "media_type") val mediaType: String? = null,
    @ColumnInfo(name = "media_size") val mediaSize: Long? = null,
    @ColumnInfo(name = "image_url") val imageUrl: String? = null,
    @ColumnInfo(name = "chapters_url") val chaptersUrl: String? = null,
    @ColumnInfo(name = "transcript_url") val transcriptUrl: String? = null,
    @ColumnInfo(name = "transcript_type") val transcriptType: String? = null,
    val state: EpisodeState = EpisodeState.NEW,
    @ColumnInfo(name = "position_ms") val positionMs: Long = 0,
    @ColumnInfo(name = "last_played_at") val lastPlayedAt: Long? = null,
    @ColumnInfo(name = "completed_at") val completedAt: Long? = null,
    @ColumnInfo(name = "is_favorite") val isFavorite: Boolean = false
)
