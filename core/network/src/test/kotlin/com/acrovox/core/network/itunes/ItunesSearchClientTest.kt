package com.acrovox.core.network.itunes

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Before
import org.junit.Test

class ItunesSearchClientTest {
    private val server = MockWebServer()
    private lateinit var client: ItunesSearchClient

    @Before
    fun start() {
        server.start()
        client =
            ItunesSearchClient(
                OkHttpClient(),
                Json {
                    ignoreUnknownKeys = true
                },
                Dispatchers.Unconfined,
                server.url("/")
            )
    }

    @After fun stop() = server.close()

    @Test
    fun search_mapsResults_andSendsQuery() = runTest {
        val body = javaClass.getResourceAsStream("/itunes/search.json")!!.bufferedReader().readText()
        server.enqueue(MockResponse.Builder().body(body).build())

        val results = client.search("affaires sensibles")

        assertThat(results).hasSize(3)
        val first = results.first()
        assertThat(first.title).isEqualTo("Affaires sensibles")
        assertThat(first.author).isEqualTo("France Inter")
        assertThat(first.feedUrl).isNull()
        assertThat(first.artworkUrl).endsWith("600x600bb.jpg")
        val url = server.takeRequest().url
        assertThat(url.encodedPath).isEqualTo("/search")
        assertThat(url.queryParameter("term")).isEqualTo("affaires sensibles")
        assertThat(url.queryParameter("media")).isEqualTo("podcast")
        assertThat(url.queryParameter("country")).isEqualTo("FR")
    }
}
