package com.acrovox.core.data.repository

import androidx.room.withTransaction
import com.acrovox.core.database.AcroVoxDatabase
import com.acrovox.core.database.dao.FeedWithNewCount
import com.acrovox.core.database.entity.FeedEntity
import com.acrovox.core.model.EpisodeState
import com.acrovox.core.model.normalizeFeedUrl
import com.acrovox.core.network.feed.FeedFetchException
import com.acrovox.core.network.feed.FeedFetcher
import com.acrovox.core.network.feed.FetchResult
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

@Singleton
class SubscriptionRepository @Inject constructor(
    private val db: AcroVoxDatabase,
    private val fetcher: FeedFetcher,
    private val clock: Clock
) {
    private val feedDao get() = db.feedDao()
    private val episodeDao get() = db.episodeDao()

    fun observeSubscriptions(): Flow<List<FeedWithNewCount>> = feedDao.observeAllWithNewCount()

    fun observeIsSubscribed(preview: FeedPreview): Flow<Boolean> =
        combine(preview.knownUrls.map { feedDao.observeByUrl(it) }) { feeds -> feeds.any { it != null } }

    /** Podcast déjà suivi sous l'une des adresses du flux, ou null. */
    suspend fun findSubscribed(preview: FeedPreview): FeedEntity? =
        preview.knownUrls.firstNotNullOfOrNull { feedDao.getByUrl(it) }

    /**
     * Lit un flux sans l'enregistrer.
     * @throws FeedFetchException adresse invalide, réseau, page sans flux, XML illisible.
     */
    suspend fun preview(input: String): FeedPreview {
        val url = normalizeFeedUrl(input) ?: throw FeedFetchException("Adresse invalide : $input")
        return when (val result = fetcher.fetch(url)) {
            is FetchResult.Fetched -> FeedPreview(result.url, result.feed, result.etag, result.lastModified)
            FetchResult.NotModified -> throw FeedFetchException("Réponse inattendue du serveur (304)")
        }
    }

    /**
     * Enregistre le podcast et ses épisodes. Seul le plus récent entre dans la boîte de réception ;
     * les autres restent au catalogue ([EpisodeState.AVAILABLE]).
     *
     * @param inboxLatest faux : aucun épisode dans la boîte (import en masse).
     * @return identifiant du podcast, existant si déjà abonné.
     */
    suspend fun subscribe(preview: FeedPreview, inboxLatest: Boolean = true): Long = db.withTransaction {
        findSubscribed(preview)?.let { return@withTransaction it.id }
        val now = clock.millis()
        val feed = preview.feed
        val feedId = feedDao.insert(
            FeedEntity(
                feedUrl = preview.canonicalUrl,
                title = feed.title,
                author = feed.author,
                description = feed.description,
                imageUrl = feed.imageUrl,
                link = feed.link,
                language = feed.language,
                categories = feed.categories.joinToString(",").ifEmpty { null },
                subscribedAt = now,
                lastRefreshAt = now,
                etag = preview.etag,
                lastModified = preview.lastModified
            )
        )
        val episodes = feed.episodes.map { it.toEntity(feedId, fallbackDate = now) }.sortedByDescending { it.pubDate }
        val inboxCount = if (inboxLatest) 1 else 0
        episodeDao.mergeFromFeed(feedId, episodes.take(inboxCount), stateForNew = EpisodeState.NEW)
        episodeDao.mergeFromFeed(feedId, episodes.drop(inboxCount), stateForNew = EpisodeState.AVAILABLE)
        feedId
    }

    suspend fun unsubscribe(feedId: Long) = feedDao.delete(feedId)
}
