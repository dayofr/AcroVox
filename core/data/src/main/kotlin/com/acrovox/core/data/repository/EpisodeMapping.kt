package com.acrovox.core.data.repository

import com.acrovox.core.database.entity.EpisodeEntity
import com.acrovox.core.network.rss.ParsedEpisode

/** @param fallbackDate date retenue si le flux n'en donne pas (heure de lecture du flux). */
internal fun ParsedEpisode.toEntity(feedId: Long, fallbackDate: Long) = EpisodeEntity(
    feedId = feedId,
    guid = guid,
    title = title,
    description = description,
    link = link,
    pubDate = pubDate ?: fallbackDate,
    durationMs = durationMs,
    mediaUrl = mediaUrl,
    mediaType = mediaType,
    mediaSize = mediaSize,
    imageUrl = imageUrl,
    chaptersUrl = chaptersUrl,
    transcriptUrl = transcriptUrl,
    transcriptType = transcriptType
)
