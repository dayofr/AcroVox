package com.acrovox.core.data.repository

import com.acrovox.core.network.rss.ParsedFeed

/** Flux lu mais pas encore enregistré : affiché avant l'abonnement. */
class FeedPreview internal constructor(
    /** Adresse définitive du flux (après découverte HTML ou redirection permanente). */
    val feedUrl: String,
    internal val feed: ParsedFeed,
    internal val etag: String?,
    internal val lastModified: String?
) {
    /**
     * Adresse à enregistrer : `itunes:new-feed-url` si le flux annonce un déménagement,
     * sinon [feedUrl]. Le rafraîchissement suit la même règle.
     */
    val canonicalUrl: String
        get() = feed.newFeedUrl?.takeIf { it.startsWith("http://") || it.startsWith("https://") } ?: feedUrl

    /** Toutes les adresses sous lesquelles ce podcast peut déjà être suivi. */
    val knownUrls: Set<String> get() = setOf(feedUrl, canonicalUrl)

    val title: String get() = feed.title
    val author: String? get() = feed.author
    val description: String? get() = feed.description
    val imageUrl: String? get() = feed.imageUrl
    val episodeCount: Int get() = feed.episodes.size
    val latestEpisodes: List<PreviewEpisode>
        get() = feed.episodes.sortedByDescending { it.pubDate ?: 0 }.take(PREVIEW_EPISODES).map {
            PreviewEpisode(it.title, it.pubDate, it.durationMs)
        }

    private companion object {
        const val PREVIEW_EPISODES = 20
    }
}

data class PreviewEpisode(val title: String, val pubDate: Long?, val durationMs: Long?)
