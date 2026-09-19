package com.acrovox.core.model

/** Podcast trouvé dans un annuaire (iTunes). */
data class PodcastSearchResult(
    val id: Long,
    val title: String,
    val author: String?,
    val artworkUrl: String?,
    /** Null si l'annuaire ne publie pas le flux (Radio France notamment). */
    val feedUrl: String?,
    val genre: String?,
    val episodeCount: Int?
)
