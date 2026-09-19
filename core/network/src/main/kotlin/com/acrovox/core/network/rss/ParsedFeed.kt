package com.acrovox.core.network.rss

/** Contenu d'un flux tel que lu, avant fusion en base. */
data class ParsedFeed(
    val title: String,
    val author: String? = null,
    val description: String? = null,
    val imageUrl: String? = null,
    val link: String? = null,
    val language: String? = null,
    val categories: List<String> = emptyList(),
    /** `podcast:guid`, identifiant stable du podcast quel que soit l'hébergeur. */
    val podcastGuid: String? = null,
    /** `itunes:new-feed-url` : le flux a déménagé. */
    val newFeedUrl: String? = null,
    val episodes: List<ParsedEpisode> = emptyList()
)

data class ParsedEpisode(
    /** guid du flux, ou URL média à défaut. Jamais vide. */
    val guid: String,
    val title: String,
    val description: String? = null,
    val link: String? = null,
    /** Epoch en millisecondes ; null si la date est absente ou illisible. */
    val pubDate: Long? = null,
    val durationMs: Long? = null,
    val mediaUrl: String,
    val mediaType: String? = null,
    val mediaSize: Long? = null,
    val imageUrl: String? = null,
    val chaptersUrl: String? = null,
    val transcriptUrl: String? = null,
    val transcriptType: String? = null
)

class FeedParseException(message: String, cause: Throwable? = null) : Exception(message, cause)
