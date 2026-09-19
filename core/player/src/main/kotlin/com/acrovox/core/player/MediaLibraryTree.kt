package com.acrovox.core.player

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.acrovox.core.data.repository.EpisodeRepository
import com.acrovox.core.database.AcroVoxDatabase
import com.acrovox.core.database.entity.EpisodeWithFeed
import com.acrovox.core.download.DownloadManager
import com.acrovox.core.download.DownloadSettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * Arborescence proposée à Android Auto et aux autres navigateurs de médias :
 * File, Boîte de réception, Téléchargements, Abonnements → podcast → épisodes.
 *
 * Les épisodes gardent leur identifiant comme `mediaId`, comme partout dans le lecteur.
 * En mode « lecture hors connexion uniquement », seuls les épisodes téléchargés sont proposés.
 */
class MediaLibraryTree @Inject constructor(
    private val db: AcroVoxDatabase,
    private val episodes: EpisodeRepository,
    private val downloads: DownloadManager,
    private val downloadSettings: DownloadSettingsRepository
) {
    fun root(): MediaItem = folder(ROOT, "AcroVox")

    /** Enfants d'un dossier, ou null s'il n'existe pas. */
    suspend fun children(parentId: String): List<MediaItem>? = when {
        parentId == ROOT -> listOf(
            folder(QUEUE, "File d'attente"),
            folder(INBOX, "Boîte de réception"),
            folder(DOWNLOADS, "Téléchargements"),
            folder(FEEDS, "Abonnements")
        )
        parentId == QUEUE -> playable(episodes.observeQueue().first())
        parentId == INBOX -> playable(episodes.observeInbox().first())
        parentId == DOWNLOADS -> db.downloadDao().observeCompletedEpisodes().first().map { it.toBrowseItem() }
        parentId == FEEDS -> db.feedDao().getAll().map { feed ->
            folder(FEED_PREFIX + feed.id, feed.title, feed.imageUrl, subtitle = feed.author)
        }
        parentId.startsWith(FEED_PREFIX) -> parentId.removePrefix(FEED_PREFIX).toLongOrNull()
            ?.let { playable(db.episodeDao().getRecentForFeed(it, FEED_LIMIT)) }
        else -> null
    }

    suspend fun item(mediaId: String): MediaItem? = when {
        mediaId == ROOT -> root()
        mediaId in FOLDERS -> children(ROOT)?.firstOrNull { it.mediaId == mediaId }
        mediaId.startsWith(FEED_PREFIX) -> mediaId.removePrefix(FEED_PREFIX).toLongOrNull()
            ?.let { db.feedDao().get(it) }
            ?.let { folder(mediaId, it.title, it.imageUrl, subtitle = it.author) }
        else -> mediaId.toLongOrNull()?.let { episodes.getWithFeed(it) }?.toBrowseItem()
    }

    suspend fun search(query: String): List<MediaItem> = playable(db.episodeDao().search(query.trim(), SEARCH_LIMIT))

    /**
     * Épisode à lire pour une demande vocale : le premier résultat de la recherche,
     * ou, sans requête (« lis mes podcasts »), l'épisode commencé puis la tête de file.
     */
    suspend fun episodeForVoice(query: String?): Long? {
        if (!query.isNullOrBlank()) return search(query).firstOrNull()?.mediaId?.toLongOrNull()
        return listOfNotNull(
            episodes.observeResume().first()?.episode?.id,
            episodes.observeQueue().first().firstOrNull()?.episode?.id
        ).firstOrNull { !downloads.isBlocked(it) }
    }

    private suspend fun playable(list: List<EpisodeWithFeed>): List<MediaItem> {
        val offlineOnly = downloadSettings.current().downloadedOnly
        return list.filter { !offlineOnly || !downloads.isBlocked(it.episode.id) }.map { it.toBrowseItem() }
    }

    private fun folder(id: String, title: String, artwork: String? = null, subtitle: String? = null) =
        MediaItem.Builder()
            .setMediaId(id)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setSubtitle(subtitle)
                    .setArtworkUri(artwork?.let(Uri::parse))
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .setMediaType(
                        if (id.startsWith(FEED_PREFIX)) {
                            MediaMetadata.MEDIA_TYPE_PODCAST
                        } else {
                            MediaMetadata.MEDIA_TYPE_FOLDER_PODCASTS
                        }
                    )
                    .build()
            )
            .build()

    companion object {
        const val ROOT = "root"
        const val QUEUE = "queue"
        const val INBOX = "inbox"
        const val DOWNLOADS = "downloads"
        const val FEEDS = "feeds"
        const val FEED_PREFIX = "feed:"
        private val FOLDERS = setOf(QUEUE, INBOX, DOWNLOADS, FEEDS)
        private const val FEED_LIMIT = 50
        private const val SEARCH_LIMIT = 30
    }
}

/** Épisode tel qu'affiché dans un navigateur : lisable, sans adresse (le service la complète). */
internal fun EpisodeWithFeed.toBrowseItem(): MediaItem = MediaItem.Builder()
    .setMediaId(episode.id.toString())
    .setMediaMetadata(
        MediaMetadata.Builder()
            .setTitle(episode.title)
            .setArtist(feed.title)
            .setSubtitle(feed.title)
            .setArtworkUri((episode.imageUrl ?: feed.imageUrl)?.let(Uri::parse))
            .setDurationMs(episode.durationMs)
            .setIsBrowsable(false)
            .setIsPlayable(true)
            .setMediaType(MediaMetadata.MEDIA_TYPE_PODCAST_EPISODE)
            .build()
    )
    .build()
