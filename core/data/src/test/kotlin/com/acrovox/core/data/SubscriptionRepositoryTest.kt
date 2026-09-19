package com.acrovox.core.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.acrovox.core.data.repository.SubscriptionRepository
import com.acrovox.core.database.AcroVoxDatabase
import com.acrovox.core.model.EpisodeState
import com.acrovox.core.network.feed.FeedFetcher
import com.acrovox.core.network.rss.FeedParser
import com.google.common.truth.Truth.assertThat
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SubscriptionRepositoryTest {
    private val server = MockWebServer()
    private lateinit var db: AcroVoxDatabase
    private lateinit var repository: SubscriptionRepository
    private val now = Instant.parse("2026-09-19T10:00:00Z")

    private val rss = """
        <rss version="2.0"><channel><title>Podcast</title>
          <item><title>Ancien</title><guid>1</guid><pubDate>Mon, 07 Sep 2026 08:00:00 GMT</pubDate>
            <enclosure url="https://example.org/1.mp3" type="audio/mpeg"/></item>
          <item><title>Récent</title><guid>3</guid><pubDate>Fri, 18 Sep 2026 08:00:00 GMT</pubDate>
            <enclosure url="https://example.org/3.mp3" type="audio/mpeg"/></item>
          <item><title>Moyen</title><guid>2</guid><pubDate>Fri, 11 Sep 2026 08:00:00 GMT</pubDate>
            <enclosure url="https://example.org/2.mp3" type="audio/mpeg"/></item>
        </channel></rss>
    """.trimIndent()

    @Before
    fun setUp() {
        server.start()
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AcroVoxDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val fetcher = FeedFetcher(OkHttpClient(), FeedParser(), Dispatchers.Unconfined)
        repository = SubscriptionRepository(db, fetcher, Clock.fixed(now, ZoneOffset.UTC))
    }

    @After
    fun tearDown() {
        db.close()
        server.close()
    }

    private fun enqueueFeed() = server.enqueue(
        MockResponse.Builder().body(
            rss
        ).addHeader("Content-Type", "application/rss+xml").addHeader("ETag", "\"e1\"").build()
    )

    @Test
    fun subscribe_putsOnlyLatestEpisodeInInbox() = runTest {
        enqueueFeed()
        val preview = repository.preview(server.url("/feed.xml").toString())

        val feedId = repository.subscribe(preview)

        val states = db.episodeDao().observeByFeed(feedId).first().associate { it.title to it.state }
        assertThat(states).containsExactly(
            "Récent",
            EpisodeState.NEW,
            "Moyen",
            EpisodeState.AVAILABLE,
            "Ancien",
            EpisodeState.AVAILABLE
        )
        val feed = db.feedDao().get(feedId)!!
        assertThat(feed.etag).isEqualTo("\"e1\"")
        assertThat(feed.subscribedAt).isEqualTo(now.toEpochMilli())
    }

    @Test
    fun subscribeTwice_returnsSameFeed() = runTest {
        enqueueFeed()
        val preview = repository.preview(server.url("/feed.xml").toString())

        val first = repository.subscribe(preview)
        val second = repository.subscribe(preview)

        assertThat(second).isEqualTo(first)
        assertThat(db.feedDao().getAll()).hasSize(1)
        assertThat(repository.observeIsSubscribed(preview.feedUrl).first()).isTrue()
    }

    @Test
    fun preview_listsLatestEpisodesFirst() = runTest {
        enqueueFeed()

        val preview = repository.preview(server.url("/feed.xml").toString())

        assertThat(preview.latestEpisodes.map { it.title }).containsExactly("Récent", "Moyen", "Ancien").inOrder()
        assertThat(db.feedDao().getAll()).isEmpty()
    }
}
