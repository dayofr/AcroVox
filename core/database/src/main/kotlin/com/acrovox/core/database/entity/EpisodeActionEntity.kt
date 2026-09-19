package com.acrovox.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.acrovox.core.model.EpisodeActionType

/**
 * Action gPodder en attente d'envoi.
 *
 * Porte les URL plutôt qu'une clé étrangère : l'action doit partir même si
 * l'épisode ou le podcast a été supprimé localement entre-temps.
 */
@Entity(
    tableName = "episode_action",
    indices = [Index(value = ["timestamp"])]
)
data class EpisodeActionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "podcast_url") val podcastUrl: String,
    @ColumnInfo(name = "episode_url") val episodeUrl: String,
    val guid: String? = null,
    val action: EpisodeActionType,
    /** Epoch en millisecondes. */
    val timestamp: Long,
    /** Secondes, pour [EpisodeActionType.PLAY] seulement. */
    val started: Int? = null,
    val position: Int? = null,
    val total: Int? = null
)
