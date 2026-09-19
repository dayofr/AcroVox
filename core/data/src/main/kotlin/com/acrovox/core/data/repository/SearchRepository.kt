package com.acrovox.core.data.repository

import com.acrovox.core.model.PodcastSearchResult
import com.acrovox.core.network.feed.RadioFranceResolver
import com.acrovox.core.network.itunes.ItunesSearchClient
import javax.inject.Inject

interface SearchRepository {
    /** @throws com.acrovox.core.network.itunes.SearchException réseau ou service indisponible. */
    suspend fun search(term: String): List<PodcastSearchResult>
}

internal class ItunesSearchRepository @Inject constructor(private val client: ItunesSearchClient) : SearchRepository {
    /**
     * Les résultats sans flux (Radio France) reçoivent l'adresse probable de leur page
     * radiofrance.fr : la découverte HTML y trouvera le flux.
     */
    override suspend fun search(term: String): List<PodcastSearchResult> = client.search(term).map { result ->
        if (result.feedUrl !=
            null
        ) {
            result
        } else {
            result.copy(feedUrl = RadioFranceResolver.pageUrl(result.title, result.author))
        }
    }
}
