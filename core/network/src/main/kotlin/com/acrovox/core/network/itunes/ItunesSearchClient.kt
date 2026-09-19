package com.acrovox.core.network.itunes

import com.acrovox.core.model.PodcastSearchResult
import com.acrovox.core.network.di.IoDispatcher
import java.io.IOException
import javax.inject.Inject
import javax.inject.Named
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

class SearchException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Recherche de podcasts via l'API iTunes Search (sans clé). */
class ItunesSearchClient @Inject constructor(
    private val client: OkHttpClient,
    private val json: Json,
    @param:IoDispatcher private val io: CoroutineDispatcher,
    @param:Named(BASE_URL_NAME) private val baseUrl: HttpUrl
) {
    suspend fun search(term: String, country: String = "FR", limit: Int = 50): List<PodcastSearchResult> =
        withContext(io) {
            val url = baseUrl.newBuilder()
                .addPathSegment("search")
                .addQueryParameter("media", "podcast")
                .addQueryParameter("entity", "podcast")
                .addQueryParameter("country", country)
                .addQueryParameter("limit", limit.toString())
                .addQueryParameter("term", term)
                .build()
            try {
                client.newCall(Request.Builder().url(url).build()).execute().use { response ->
                    if (!response.isSuccessful) throw SearchException("Recherche indisponible (${response.code})")
                    json.decodeFromString<SearchResponse>(response.body.string()).results.mapNotNull { it.toModel() }
                }
            } catch (e: IOException) {
                throw SearchException("Connexion impossible", e)
            }
        }

    @Serializable
    private data class SearchResponse(val results: List<Result> = emptyList())

    @Serializable
    private data class Result(
        val collectionId: Long? = null,
        val collectionName: String? = null,
        val artistName: String? = null,
        val feedUrl: String? = null,
        val artworkUrl600: String? = null,
        val artworkUrl100: String? = null,
        val primaryGenreName: String? = null,
        val trackCount: Int? = null
    ) {
        fun toModel(): PodcastSearchResult? {
            val id = collectionId ?: return null
            val title = collectionName ?: return null
            return PodcastSearchResult(
                id = id,
                title = title,
                author = artistName,
                artworkUrl = artworkUrl600 ?: artworkUrl100,
                feedUrl = feedUrl,
                genre = primaryGenreName,
                episodeCount = trackCount
            )
        }
    }

    companion object {
        const val BASE_URL_NAME = "itunesBaseUrl"
        val DEFAULT_BASE_URL = "https://itunes.apple.com/".toHttpUrl()
    }
}
