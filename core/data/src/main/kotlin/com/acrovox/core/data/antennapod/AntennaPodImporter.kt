package com.acrovox.core.data.antennapod

import com.acrovox.core.data.refresh.RefreshRepository
import com.acrovox.core.database.AcroVoxDatabase
import com.acrovox.core.database.entity.FeedEntity
import com.acrovox.core.model.EpisodeState
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

/** Bilan d'un import AntennaPod. */
data class AntennaPodSummary(
    val feedsAdded: Int,
    val feedsSkipped: Int,
    val episodesMatched: Int,
    val episodesPlayed: Int,
    val episodesInProgress: Int,
    val queued: Int,
    val favorites: Int
)

/** Progression : podcasts traités sur total. */
data class AntennaPodProgress(val done: Int, val total: Int)

/**
 * Migre un export AntennaPod : abonnements (réglages de lecture compris),
 * états lu, positions, file et favoris.
 *
 * Chaque podcast est rafraîchi depuis son flux pour recréer les épisodes,
 * puis les éléments AntennaPod sont rattachés par URL média (à défaut guid).
 * Les podcasts déjà suivis sont ignorés.
 */
@Singleton
class AntennaPodImporter @Inject constructor(
    private val db: AcroVoxDatabase,
    private val refresher: RefreshRepository,
    private val clock: Clock
) {
    private val feedDao get() = db.feedDao()
    private val episodeDao get() = db.episodeDao()
    private val queueDao get() = db.queueDao()

    suspend fun import(path: String, onProgress: suspend (AntennaPodProgress) -> Unit = {}): AntennaPodSummary {
        val backup = AntennaPodBackup.read(path)
        var added = 0
        var skipped = 0
        var matched = 0
        var played = 0
        var inProgress = 0
        // Identifiants AntennaPod (FeedItems.id) vers AcroVox, pour la file et les favoris.
        val apToLocal = mutableMapOf<Long, Long>()
        // File AntennaPod par identifiant d'épisode local, dans l'ordre.
        val queuedLocal = mutableListOf<Long>()
        val favoriteLocal = mutableListOf<Long>()

        backup.feeds.forEachIndexed { index, apFeed ->
            onProgress(AntennaPodProgress(index, backup.feeds.size))
            val existing = feedDao.getByUrl(apFeed.feedUrl)
            val feedId = if (existing != null) {
                skipped++
                existing.id
            } else {
                val now = clock.millis()
                val id = feedDao.insert(
                    FeedEntity(
                        feedUrl = apFeed.feedUrl,
                        title =
                        apFeed.customTitle?.takeIf { it.isNotBlank() } ?: apFeed.title.ifBlank { apFeed.feedUrl },
                        author = apFeed.author,
                        description = apFeed.description,
                        imageUrl = apFeed.imageUrl,
                        link = apFeed.link,
                        language = apFeed.language,
                        subscribedAt = now,
                        playbackSpeed = apFeed.playbackSpeed.takeIf { it > 0 },
                        skipIntroMs = apFeed.skipIntroMs.coerceAtLeast(0),
                        skipOutroMs = apFeed.skipOutroMs.coerceAtLeast(0)
                    )
                )
                added++
                id
            }
            val feed = feedDao.getByUrl(apFeed.feedUrl) ?: return@forEachIndexed
            refresher.refresh(feed)
            if (apFeed.customTitle?.isNotBlank() == true) {
                feedDao.update(feed.copy(title = apFeed.customTitle))
            }
            val lookup = episodeDao.getLookup(feedId)
            val byMedia = lookup.associate { it.mediaUrl to it.id }
            val byGuid = lookup.associate { it.guid to it.id }
            val items = backup.items.filter { it.feedRowId == apFeed.rowId }
            for (item in items) {
                val localId = item.mediaUrl?.let(byMedia::get)
                    ?: item.guid?.let(byGuid::get)
                    ?: continue
                matched++
                apToLocal[item.rowId] = localId
                val now = clock.millis()
                when {
                    item.completedAt > 0 -> {
                        episodeDao.markPlayed(localId, item.completedAt)
                        played++
                    }
                    item.read == 1 -> {
                        episodeDao.markPlayed(localId, item.lastPlayedAt.takeIf { it > 0 } ?: now)
                        played++
                    }
                    item.positionMs > 0 -> {
                        episodeDao.updatePosition(localId, item.positionMs, now)
                        inProgress++
                    }
                    item.read == -1 -> episodeDao.setState(listOf(localId), EpisodeState.NEW)
                    else -> episodeDao.setState(listOf(localId), EpisodeState.AVAILABLE)
                }
            }
            onProgress(AntennaPodProgress(index + 1, backup.feeds.size))
        }
        for (apId in backup.queueItemIds) {
            apToLocal[apId]?.let(queuedLocal::add)
        }
        for (apId in backup.favoriteItemIds) {
            apToLocal[apId]?.let(favoriteLocal::add)
        }
        if (queuedLocal.isNotEmpty()) queueDao.add(queuedLocal)
        for (id in favoriteLocal) episodeDao.setFavorite(id, true)
        return AntennaPodSummary(added, skipped, matched, played, inProgress, queuedLocal.size, favoriteLocal.size)
    }
}
