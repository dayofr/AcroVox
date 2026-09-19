package com.acrovox.core.player

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.acrovox.core.database.entity.EpisodeWithFeed

/** Un MediaItem porte l'identifiant de l'épisode dans `mediaId`. */
internal fun episodeIdOf(item: MediaItem?): Long? = item?.mediaId?.toLongOrNull()

/** Demande de lecture envoyée par l'interface : l'identifiant suffit, le service complète. */
fun episodeRequest(episodeId: Long): MediaItem = MediaItem.Builder().setMediaId(episodeId.toString()).build()

internal fun EpisodeWithFeed.toMediaItem(localPath: String? = null): MediaItem = MediaItem.Builder()
    .setMediaId(episode.id.toString())
    .setUri(localPath?.let { Uri.fromFile(java.io.File(it)) } ?: Uri.parse(episode.mediaUrl))
    .setMimeType(episode.mediaType)
    .setMediaMetadata(
        MediaMetadata.Builder()
            .setTitle(episode.title)
            .setArtist(feed.title)
            .setAlbumTitle(feed.title)
            .setArtworkUri((episode.imageUrl ?: feed.imageUrl)?.let(Uri::parse))
            .setDurationMs(episode.durationMs)
            .setMediaType(MediaMetadata.MEDIA_TYPE_PODCAST_EPISODE)
            .build()
    )
    .build()
