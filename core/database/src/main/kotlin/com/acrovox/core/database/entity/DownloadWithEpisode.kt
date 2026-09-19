package com.acrovox.core.database.entity

import androidx.room.Embedded
import androidx.room.Relation

data class DownloadWithEpisode(
    @Embedded val download: DownloadEntity,
    @Relation(entity = EpisodeEntity::class, parentColumn = "episode_id", entityColumn = "id")
    val episode: EpisodeWithFeed
)
