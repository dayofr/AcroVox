package com.acrovox.core.data.refresh

import androidx.room.withTransaction
import com.acrovox.core.data.repository.ChaptersRepository
import com.acrovox.core.data.repository.toEntity
import com.acrovox.core.database.AcroVoxDatabase
import com.acrovox.core.database.entity.FeedEntity
import com.acrovox.core.model.EpisodeState
import com.acrovox.core.network.feed.FeedFetcher
import com.acrovox.core.network.feed.FetchResult
import com.acrovox.core.network.rss.ParsedEpisode
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit

/** Bilan d'un rafraîchissement. */
data class RefreshSummary(
    /** Nouveaux épisodes par podcast (seulement les podcasts qui en ont). */
    val newEpisodes: Map<FeedEntity, Int>,
    /** Podcasts en échec et message d'erreur. */
    val failures: Map<FeedEntity, String>
) {
    val newEpisodeCount: Int get() = newEpisodes.values.sum()
}

/**
 * Relit les flux et range les nouveaux épisodes dans la boîte de réception.
 * Ne télécharge rien et ne touche pas à la file.
 */
@Singleton
class RefreshRepository @Inject constructor(
    private val db: AcroVoxDatabase,
    private val fetcher: FeedFetcher,
    private val chapters: ChaptersRepository,
    private val clock: Clock
) {
    private val feedDao get() = db.feedDao()
    private val episodeDao get() = db.episodeDao()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    /** Un seul rafraîchissement à la fois : le manuel et le périodique ne se chevauchent pas. */
    private val mutex = Mutex()

    suspend fun refreshAll(): RefreshSummary = mutex.withLock {
        _isRefreshing.value = true
        try {
            val semaphore = Semaphore(MAX_PARALLEL_FEEDS)
            val outcomes = coroutineScope {
                feedDao.getAll().map { feed ->
                    async { feed to semaphore.withPermit { refreshCatching(feed) } }
                }.awaitAll()
            }
            RefreshSummary(
                newEpisodes = outcomes.mapNotNull { (feed, o) ->
                    (o as? Outcome.Updated)?.takeIf { it.newCount > 0 }?.let {
                        feed to
                            it.newCount
                    }
                }.toMap(),
                failures = outcomes.mapNotNull { (feed, o) ->
                    (o as? Outcome.Failed)?.let { feed to it.message }
                }.toMap()
            )
        } finally {
            _isRefreshing.value = false
        }
    }

    private sealed interface Outcome {
        data class Updated(val newCount: Int) : Outcome

        data class Failed(val message: String) : Outcome
    }

    private suspend fun refreshCatching(feed: FeedEntity): Outcome = try {
        Outcome.Updated(refresh(feed))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Outcome.Failed(e.message ?: e.javaClass.simpleName)
    }

    /** @return nombre de nouveaux épisodes. */
    internal suspend fun refresh(feed: FeedEntity): Int {
        val now = clock.millis()
        val result = fetcher.fetch(feed.feedUrl, feed.etag, feed.lastModified)
        if (result !is FetchResult.Fetched) {
            feedDao.updateRefresh(feed.id, now, feed.etag, feed.lastModified)
            return 0
        }
        val parsed = result.feed
        return db.withTransaction {
            val known = episodeDao.getIdentities(feed.id)
            val knownGuids = known.mapTo(HashSet()) { it.guid }
            val guidByMediaUrl = known.associate { it.mediaUrl to it.guid }
            val parsedEpisodes = parsed.episodes
                .map { it.keepKnownGuid(knownGuids, guidByMediaUrl) }
                .distinctBy { it.guid }
            val episodes = parsedEpisodes.map { it.toEntity(feed.id, fallbackDate = now) }
            val inserted = episodeDao.mergeFromFeed(feed.id, episodes, stateForNew = EpisodeState.NEW)
            chapters.storePodlove(feed.id, parsedEpisodes)
            feedDao.updateMetadata(
                id = feed.id,
                title = parsed.title.ifBlank { feed.title },
                author = parsed.author,
                description = parsed.description,
                imageUrl = parsed.imageUrl ?: feed.imageUrl,
                link = parsed.link,
                language = parsed.language,
                categories = parsed.categories.joinToString(",").ifEmpty { null }
            )
            feedDao.updateRefresh(feed.id, now, result.etag, result.lastModified)
            moveIfNeeded(feed, parsed.newFeedUrl ?: result.url)
            inserted.size
        }
    }

    /** Suit un déménagement (redirection permanente, itunes:new-feed-url) si l'adresse est libre. */
    private suspend fun moveIfNeeded(feed: FeedEntity, url: String) {
        if (url == feed.feedUrl || feedDao.getByUrl(url) != null) return
        feedDao.updateUrl(feed.id, url)
    }

    private companion object {
        const val MAX_PARALLEL_FEEDS = 4
    }
}

/**
 * Certains hébergeurs changent le guid d'un épisode déjà publié. Si l'URL média est connue,
 * on garde l'ancien guid : l'épisode n'est pas dupliqué ni remis dans la boîte.
 */
private fun ParsedEpisode.keepKnownGuid(knownGuids: Set<String>, guidByMediaUrl: Map<String, String>): ParsedEpisode {
    if (guid in knownGuids) return this
    val previous = guidByMediaUrl[mediaUrl] ?: return this
    return copy(guid = previous)
}
