package com.acrovox.core.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.acrovox.core.data.refresh.RefreshRepository
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
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RefreshRepositoryTest {
    private val server = MockWebServer()
    private lateinit var db: AcroVoxDatabase
    private lateinit var subscriptions: SubscriptionRepository
    private lateinit var refresher: RefreshRepository

    /** Réponses par chemin, modifiables pendant le test. */
    private val responses = mutableMapOf<String, MockResponse>()

    private fun rss(vararg items: Triple<String, String, String>) = MockResponse.Builder()
        .addHeader("Content-Type", "application/rss+xml")
        .body(
            "<rss version=\"2.0\"><channel><title>Podcast</title>" +
                items.joinToString("") { (guid, title, date) ->
                    "<item><title>$title</title><guid>$guid</guid><pubDate>$date</pubDate>" +
                        "<enclosure url=\"https://example.org/$title.mp3\" type=\"audio/mpeg\"/></item>"
                } + "</channel></rss>"
        )
        .build()

    private val ep1 = Triple("1", "Un", "Mon, 07 Sep 2026 08:00:00 GMT")
    private val ep2 = Triple("2", "Deux", "Fri, 11 Sep 2026 08:00:00 GMT")
    private val ep3 = Triple("3", "Trois", "Fri, 18 Sep 2026 08:00:00 GMT")

    @Before
    fun setUp() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest) =
                responses[request.url.encodedPath] ?: MockResponse.Builder().code(404).build()
        }
        server.start()
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AcroVoxDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val fetcher = FeedFetcher(OkHttpClient(), FeedParser(), Dispatchers.Unconfined)
        val clock = Clock.fixed(Instant.parse("2026-09-19T10:00:00Z"), ZoneOffset.UTC)
        subscriptions = SubscriptionRepository(db, fetcher, clock)
        refresher = RefreshRepository(db, fetcher, clock)
    }

    @After
    fun tearDown() {
        db.close()
        server.close()
    }

    private suspend fun subscribe(path: String): Long =
        subscriptions.subscribe(subscriptions.preview(server.url(path).toString()))

    private suspend fun states(feedId: Long) = db.episodeDao().observeByFeed(feedId).first().associate {
        it.title to
            it.state
    }

    @Test
    fun newEpisode_goesToInbox_existingStatesUntouched() = runTest {
        responses["/a.xml"] = rss(ep1, ep2)
        val feedId = subscribe("/a.xml")
        db.episodeDao().setState(db.episodeDao().getInboxIds(), EpisodeState.IGNORED)

        responses["/a.xml"] = rss(ep1, ep2, ep3)
        val summary = refresher.refreshAll()

        assertThat(summary.newEpisodeCount).isEqualTo(1)
        assertThat(
            states(feedId)
        ).containsExactly("Un", EpisodeState.AVAILABLE, "Deux", EpisodeState.IGNORED, "Trois", EpisodeState.NEW)
        assertThat(db.queueDao().getEpisodeIds()).isEmpty()
        assertThat(db.downloadDao().observeAll().first()).isEmpty()
    }

    @Test
    fun notModified_changesNothing() = runTest {
        responses["/a.xml"] = rss(ep1)
        val feedId = subscribe("/a.xml")
        responses["/a.xml"] = MockResponse.Builder().code(304).build()

        val summary = refresher.refreshAll()

        assertThat(summary.newEpisodeCount).isEqualTo(0)
        assertThat(summary.failures).isEmpty()
        assertThat(states(feedId)).hasSize(1)
    }

    @Test
    fun changedGuidWithSameMediaUrl_isNotDuplicated() = runTest {
        responses["/a.xml"] = rss(ep1)
        val feedId = subscribe("/a.xml")

        responses["/a.xml"] = rss(ep1.copy(first = "nouveau-guid"))
        val summary = refresher.refreshAll()

        assertThat(summary.newEpisodeCount).isEqualTo(0)
        assertThat(states(feedId)).containsExactly("Un", EpisodeState.NEW)
    }

    @Test
    fun oneFailingFeed_doesNotBlockOthers() = runTest {
        responses["/a.xml"] = rss(ep1)
        responses["/b.xml"] = rss(ep1)
        subscribe("/a.xml")
        val b = subscribe("/b.xml")
        responses.remove("/a.xml")
        responses["/b.xml"] = rss(ep1, ep2)

        val summary = refresher.refreshAll()

        assertThat(summary.failures.keys.map { it.feedUrl }).containsExactly(server.url("/a.xml").toString())
        assertThat(summary.newEpisodes.keys.map { it.id }).containsExactly(b)
    }

    @Test
    fun permanentRedirect_movesFeedUrl() = runTest {
        responses["/old.xml"] = rss(ep1)
        val feedId = subscribe("/old.xml")
        responses["/old.xml"] = MockResponse.Builder().code(301).addHeader("Location", "/new.xml").build()
        responses["/new.xml"] = rss(ep1)

        refresher.refreshAll()

        assertThat(db.feedDao().get(feedId)!!.feedUrl).isEqualTo(server.url("/new.xml").toString())
    }
}
