package com.acrovox.core.network.feed

import com.acrovox.core.network.di.IoDispatcher
import com.acrovox.core.network.rss.FeedParseException
import com.acrovox.core.network.rss.FeedParser
import com.acrovox.core.network.rss.ParsedFeed
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

sealed interface FetchResult {
    /**
     * @param url adresse à mémoriser : la cible d'une redirection permanente, sinon l'adresse demandée.
     */
    data class Fetched(val feed: ParsedFeed, val url: String, val etag: String?, val lastModified: String?) :
        FetchResult

    /** 304 : le flux n'a pas changé depuis [FeedFetcher.fetch] précédent. */
    data object NotModified : FetchResult
}

class FeedFetchException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Télécharge et lit un flux. Requêtes conditionnelles (ETag, Last-Modified).
 * Une page HTML est acceptée si elle annonce un flux (`<link rel="alternate">`).
 */
class FeedFetcher @Inject constructor(
    private val client: OkHttpClient,
    private val parser: FeedParser,
    @param:IoDispatcher private val io: CoroutineDispatcher
) {
    suspend fun fetch(url: String, etag: String? = null, lastModified: String? = null): FetchResult = withContext(io) {
        fetchInternal(url, etag, lastModified, allowDiscovery = true)
    }

    private fun fetchInternal(url: String, etag: String?, lastModified: String?, allowDiscovery: Boolean): FetchResult {
        val request = try {
            Request.Builder().url(url).apply {
                header(
                    "Accept",
                    "application/rss+xml, application/atom+xml, application/xml;q=0.9, text/xml;q=0.9, */*;q=0.5"
                )
                etag?.let { header("If-None-Match", it) }
                lastModified?.let { header("If-Modified-Since", it) }
            }.build()
        } catch (e: IllegalArgumentException) {
            throw FeedFetchException("Adresse invalide : $url", e)
        }
        try {
            client.newCall(request).execute().use { response ->
                if (response.code == HTTP_NOT_MODIFIED) return FetchResult.NotModified
                if (!response.isSuccessful) throw FeedFetchException("Le serveur a répondu ${response.code}")
                val body = response.body
                val finalUrl = if (response.hasPermanentRedirect()) response.request.url.toString() else url
                if (isHtml(response)) {
                    if (!allowDiscovery) throw FeedFetchException("Page web sans flux de podcast")
                    val html = body.string()
                    val feedUrl = HtmlFeedDiscovery.find(html, response.request.url)
                        ?: throw FeedFetchException("Aucun flux annoncé sur cette page")
                    return fetchInternal(feedUrl, null, null, allowDiscovery = false)
                }
                val feed = try {
                    body.byteStream().use { parser.parse(it) }
                } catch (e: FeedParseException) {
                    throw FeedFetchException("Ce n'est pas un flux de podcast lisible", e)
                }
                return FetchResult.Fetched(feed, finalUrl, response.header("ETag"), response.header("Last-Modified"))
            }
        } catch (e: IOException) {
            throw FeedFetchException("Connexion impossible : ${e.message}", e)
        }
    }

    private fun Response.hasPermanentRedirect(): Boolean {
        var prior = priorResponse
        while (prior != null) {
            if (prior.code == 301 || prior.code == 308) return true
            prior = prior.priorResponse
        }
        return false
    }

    private fun isHtml(response: Response): Boolean {
        val type = response.header("Content-Type").orEmpty().lowercase()
        if ("html" in type) return true
        if ("xml" in type || "rss" in type || "atom" in type) return false
        val head = response.peekBody(HTML_SNIFF_BYTES).string().trimStart().lowercase()
        return head.startsWith("<!doctype html") || head.startsWith("<html")
    }

    private companion object {
        const val HTTP_NOT_MODIFIED = 304
        const val HTML_SNIFF_BYTES = 512L
    }
}
