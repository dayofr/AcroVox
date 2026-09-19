package com.acrovox.core.network.feed

import com.acrovox.core.network.rss.FeedParser
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FeedFetcherTest {
    private val server = MockWebServer()
    private val fetcher = FeedFetcher(OkHttpClient(), FeedParser(), Dispatchers.Unconfined)

    private val rss = """
        <rss version="2.0"><channel><title>Test</title>
          <item><title>E</title><guid>g</guid><enclosure url="https://example.org/e.mp3" type="audio/mpeg"/></item>
        </channel></rss>
    """.trimIndent()

    @Before fun start() = server.start()

    @After fun stop() = server.close()

    private fun respond(code: Int = 200, body: String = "", vararg headers: Pair<String, String>) {
        server.enqueue(
            MockResponse.Builder().code(code).body(body).apply {
                headers.forEach { addHeader(it.first, it.second) }
            }.build()
        )
    }

    @Test
    fun fetch_parsesFeed_andReturnsCacheHeaders() = runTest {
        respond(
            200,
            rss,
            "Content-Type" to "application/rss+xml",
            "ETag" to "\"v1\"",
            "Last-Modified" to "Sat, 19 Sep 2026 08:00:00 GMT"
        )
        val url = server.url("/feed.xml").toString()

        val result = fetcher.fetch(url) as FetchResult.Fetched

        assertThat(result.feed.title).isEqualTo("Test")
        assertThat(result.url).isEqualTo(url)
        assertThat(result.etag).isEqualTo("\"v1\"")
        assertThat(result.lastModified).isEqualTo("Sat, 19 Sep 2026 08:00:00 GMT")
    }

    @Test
    fun fetch_sendsConditionalHeaders_andHandles304() = runTest {
        respond(304)

        val result = fetcher.fetch(
            server.url("/feed.xml").toString(),
            etag = "\"v1\"",
            lastModified = "Sat, 19 Sep 2026 08:00:00 GMT"
        )

        assertThat(result).isEqualTo(FetchResult.NotModified)
        val request = server.takeRequest()
        assertThat(request.headers["If-None-Match"]).isEqualTo("\"v1\"")
        assertThat(request.headers["If-Modified-Since"]).isEqualTo("Sat, 19 Sep 2026 08:00:00 GMT")
    }

    @Test
    fun permanentRedirect_updatesUrl_temporaryDoesNot() = runTest {
        respond(301, "", "Location" to "/moved.xml")
        respond(200, rss, "Content-Type" to "application/xml")
        val permanent = fetcher.fetch(server.url("/old.xml").toString()) as FetchResult.Fetched
        assertThat(permanent.url).isEqualTo(server.url("/moved.xml").toString())

        respond(302, "", "Location" to "/cdn.xml")
        respond(200, rss, "Content-Type" to "application/xml")
        val temporary = fetcher.fetch(server.url("/stable.xml").toString()) as FetchResult.Fetched
        assertThat(temporary.url).isEqualTo(server.url("/stable.xml").toString())
    }

    @Test
    fun htmlPage_discoversAnnouncedFeed() = runTest {
        val html = """
            <!DOCTYPE html><html><head>
            <link rel="stylesheet" href="/style.css">
            <link rel="alternate" title="Affaires" href="/podcast.xml" type="application/rss+xml"/>
            </head><body></body></html>
        """.trimIndent()
        respond(200, html, "Content-Type" to "text/html; charset=utf-8")
        respond(200, rss, "Content-Type" to "application/rss+xml")

        val result = fetcher.fetch(server.url("/emission").toString()) as FetchResult.Fetched

        assertThat(result.feed.title).isEqualTo("Test")
        assertThat(result.url).isEqualTo(server.url("/podcast.xml").toString())
    }

    @Test
    fun htmlWithoutFeed_andHttpErrors_throw() = runTest {
        respond(200, "<html><body>rien</body></html>", "Content-Type" to "text/html")
        assertThrows(FeedFetchException::class.java) {
            runBlocking { fetcher.fetch(server.url("/a").toString()) }
        }

        respond(404)
        assertThrows(FeedFetchException::class.java) {
            runBlocking { fetcher.fetch(server.url("/b").toString()) }
        }

        respond(200, "{\"not\":\"xml\"}", "Content-Type" to "application/json")
        assertThrows(FeedFetchException::class.java) {
            runBlocking { fetcher.fetch(server.url("/c").toString()) }
        }
    }
}
