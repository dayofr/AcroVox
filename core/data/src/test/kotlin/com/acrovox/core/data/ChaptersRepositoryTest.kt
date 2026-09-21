package com.acrovox.core.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.acrovox.core.data.repository.ChaptersRepository
import com.acrovox.core.database.AcroVoxDatabase
import com.acrovox.core.database.entity.EpisodeEntity
import com.acrovox.core.database.entity.FeedEntity
import com.acrovox.core.network.rss.ParsedChapter
import com.acrovox.core.network.rss.PodcastChaptersFetcher
import com.google.common.truth.Truth.assertThat
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
class ChaptersRepositoryTest {
    private val server = MockWebServer()
    private lateinit var db: AcroVoxDatabase
    private lateinit var repository: ChaptersRepository
    private var episodeId: Long = 0

    @Before
    fun setUp() = runTest {
        server.start()
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AcroVoxDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = ChaptersRepository(db, PodcastChaptersFetcher(OkHttpClient(), Dispatchers.Unconfined))
        val feedId = db.feedDao().insert(FeedEntity(feedUrl = "https://example.org/f", title = "P", subscribedAt = 0))
        episodeId = db.episodeDao().mergeFromFeed(
            feedId,
            listOf(
                EpisodeEntity(
                    feedId = feedId,
                    guid = "e1",
                    title = "E1",
                    pubDate = 1,
                    mediaUrl = "https://example.org/e1.mp3",
                    chaptersUrl = server.url("/c.json").toString()
                )
            )
        ).single()
    }

    @After
    fun tearDown() {
        db.close()
        server.close()
    }

    @Test
    fun ensureLoaded_fetchesJsonOnce_thenKeepsDb() = runTest {
        server.enqueue(
            MockResponse.Builder()
                .addHeader("Content-Type", "application/json")
                .body(
                    """{"version":"1.0","chapters":[{"startTime":7.5,"title":"Intro","img":"https://example.org/i.png"},{"startTime":0,"title":"Début"}]}"""
                )
                .build()
        )

        assertThat(repository.ensureLoaded(episodeId)).isTrue()
        val chapters = repository.observe(episodeId).first()
        assertThat(chapters.map { it.title }).containsExactly("Début", "Intro").inOrder()
        assertThat(chapters[1].startMs).isEqualTo(7_500)
        assertThat(chapters[1].imageUrl).isEqualTo("https://example.org/i.png")

        assertThat(repository.ensureLoaded(episodeId)).isTrue()
        assertThat(server.requestCount).isEqualTo(1)
    }

    @Test
    fun storeIfEmpty_neverOverwrites() = runTest {
        val podlove = listOf(ParsedChapter(0, "Podlove"))
        assertThat(repository.storeIfEmpty(episodeId, podlove)).isTrue()
        assertThat(repository.storeIfEmpty(episodeId, listOf(ParsedChapter(0, "ID3")))).isFalse()
        assertThat(repository.observe(episodeId).first().single().title).isEqualTo("Podlove")
    }
}
