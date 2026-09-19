package com.acrovox.core.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.acrovox.core.data.opml.OpmlOutline
import com.acrovox.core.data.opml.OpmlRepository
import com.acrovox.core.data.repository.SubscriptionRepository
import com.acrovox.core.database.AcroVoxDatabase
import com.acrovox.core.network.feed.FeedFetcher
import com.acrovox.core.network.rss.FeedParser
import com.google.common.truth.Truth.assertThat
import java.time.Clock
import kotlinx.coroutines.Dispatchers
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
class OpmlImportTest {
    private val server = MockWebServer()
    private lateinit var db: AcroVoxDatabase
    private lateinit var subscriptions: SubscriptionRepository
    private lateinit var opml: OpmlRepository

    private val rss = """
        <rss version="2.0"><channel><title>P</title>
          <item><title>E</title><guid>g</guid><enclosure url="https://example.org/e.mp3" type="audio/mpeg"/></item>
        </channel></rss>
    """.trimIndent()

    @Before
    fun setUp() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest) = when (request.url.encodedPath) {
                "/missing.xml" -> MockResponse.Builder().code(404).build()
                else -> MockResponse.Builder().addHeader("Content-Type", "application/rss+xml").body(rss).build()
            }
        }
        server.start()
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AcroVoxDatabase::class.java).allowMainThreadQueries().build()
        subscriptions =
            SubscriptionRepository(
                db,
                FeedFetcher(OkHttpClient(), FeedParser(), Dispatchers.Unconfined),
                Clock.systemUTC()
            )
        opml = OpmlRepository(context, db, subscriptions, Dispatchers.Unconfined)
    }

    @After
    fun tearDown() {
        db.close()
        server.close()
    }

    private fun outline(path: String) = OpmlOutline(path, server.url(path).toString())

    @Test
    fun import_reportsSubscribedExistingAndFailed_withoutFillingInbox() = runTest {
        subscriptions.subscribe(subscriptions.preview(server.url("/existing.xml").toString()))
        val before = db.episodeDao().getInboxIds().size
        val progress = mutableListOf<Int>()

        val result = opml.import(
            listOf(outline("/a.xml"), outline("/b.xml"), outline("/existing.xml"), outline("/missing.xml")),
            onProgress = { progress += it }
        )

        assertThat(result.subscribed).isEqualTo(2)
        assertThat(result.alreadySubscribed).isEqualTo(1)
        assertThat(result.failures.map { it.first }).containsExactly("/missing.xml")
        assertThat(db.feedDao().getAll()).hasSize(3)
        assertThat(db.episodeDao().getInboxIds()).hasSize(before)
        assertThat(progress.sorted()).containsExactly(1, 2, 3, 4).inOrder()
    }

    @Test
    fun import_withInboxLatest_putsOneEpisodePerFeedInInbox() = runTest {
        opml.import(listOf(outline("/a.xml"), outline("/b.xml")), inboxLatest = true)

        assertThat(db.episodeDao().getInboxIds()).hasSize(2)
    }
}
