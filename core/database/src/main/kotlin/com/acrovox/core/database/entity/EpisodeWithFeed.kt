package com.acrovox.core.database.entity

import androidx.room.Embedded
import androidx.room.Relation

data class EpisodeWithFeed(
    @Embedded val episode: EpisodeEntity,
    @Relation(parentColumn = "feed_id", entityColumn = "id")
    val feed: FeedEntity
)
