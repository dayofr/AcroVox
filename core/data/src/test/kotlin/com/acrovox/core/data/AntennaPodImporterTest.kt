package com.acrovox.core.data

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.acrovox.core.data.antennapod.AntennaPodImporter
import com.acrovox.core.data.refresh.RefreshRepository
import com.acrovox.core.data.repository.ChaptersRepository
import com.acrovox.core.database.AcroVoxDatabase
import com.acrovox.core.model.EpisodeState
import com.acrovox.core.network.feed.FeedFetcher
import com.acrovox.core.network.rss.FeedParser
import com.acrovox.core.network.rss.PodcastChaptersFetcher
import com.google.common.truth.Truth.assertThat
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
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

/**
 * Import depuis une base AntennaPod minimale, flux servis par MockWebServer.
 * Le fichier réel (14 Mo) sert de référence de schéma, pas de fixture.
 */
@RunWith(RobolectricTestRunner::class)
class AntennaPodImporterTest {
    private val server = MockWebServer()
    private lateinit var db: AcroVoxDatabase
    private lateinit var importer: AntennaPodImporter
    private lateinit var backupFile: File

    private fun feedXml(): String = """
        <rss version="2.0"><channel><title>Podcast</title>
          <item><title>Ecouté</title><guid>g-played</guid><pubDate>Mon, 07 Sep 2026 08:00:00 GMT</pubDate>
            <enclosure url="https://example.org/played.mp3" type="audio/mpeg"/></item>
          <item><title>En cours</title><guid>g-progress</guid><pubDate>Fri, 11 Sep 2026 08:00:00 GMT</pubDate>
            <enclosure url="https://example.org/progress.mp3" type="audio/mpeg"/></item>
          <item><title>Nouveau</title><guid>g-new</guid><pubDate>Fri, 18 Sep 2026 08:00:00 GMT</pubDate>
            <enclosure url="https://example.org/new.mp3" type="audio/mpeg"/></item>
          <item><title>Démarqué</title><guid>g-unmarked</guid><pubDate>Sat, 19 Sep 2026 08:00:00 GMT</pubDate>
            <enclosure url="https://example.org/unmarked.mp3" type="audio/mpeg"/></item>
        </channel></rss>
    """.trimIndent()

    @Before
    fun setUp() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest) = MockResponse.Builder()
                .addHeader("Content-Type", "application/rss+xml")
                .body(feedXml())
                .build()
        }
        server.start()
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AcroVoxDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val client = OkHttpClient()
        val fetcher = FeedFetcher(client, FeedParser(), Dispatchers.Unconfined)
        val chapters = ChaptersRepository(db, PodcastChaptersFetcher(client, Dispatchers.Unconfined))
        val clock = Clock.fixed(Instant.parse("2026-09-19T10:00:00Z"), ZoneOffset.UTC)
        importer = AntennaPodImporter(db, RefreshRepository(db, fetcher, chapters, clock), clock)
        backupFile = File.createTempFile("antennapod", ".db").also { it.deleteOnExit() }
        SQLiteDatabase.openOrCreateDatabase(backupFile, null).use { ap ->
            ap.execSQL(
                "CREATE TABLE Feeds (id INTEGER PRIMARY KEY, title TEXT, custom_title TEXT, download_url TEXT, image_url TEXT, feed_playback_speed REAL, feed_skip_intro INTEGER, feed_skip_ending INTEGER)"
            )
            ap.execSQL(
                "CREATE TABLE FeedItems (id INTEGER PRIMARY KEY, feed INTEGER, title TEXT, item_identifier TEXT, read INTEGER)"
            )
            ap.execSQL(
                "CREATE TABLE FeedMedia (id INTEGER PRIMARY KEY, download_url TEXT, position INTEGER, playback_completion_date INTEGER, last_played_time INTEGER, feeditem INTEGER)"
            )
            ap.execSQL("CREATE TABLE Queue(id INTEGER PRIMARY KEY, feeditem INTEGER, feed INTEGER)")
            ap.execSQL("CREATE TABLE Favorites(id INTEGER PRIMARY KEY, feeditem INTEGER, feed INTEGER)")
            val feedUrl = server.url("/feed.xml").toString()
            ap.execSQL("INSERT INTO Feeds VALUES (7, 'Podcast', 'Mon podcast', '$feedUrl', null, 1.5, 10000, 20000)")
            ap.execSQL("INSERT INTO FeedItems VALUES (11, 7, 'Ecouté', 'g-played', 1)")
            ap.execSQL("INSERT INTO FeedItems VALUES (12, 7, 'En cours', 'g-progress', 0)")
            ap.execSQL("INSERT INTO FeedItems VALUES (13, 7, 'Nouveau', 'g-new', -1)")
            ap.execSQL("INSERT INTO FeedItems VALUES (14, 7, 'Démarqué', 'g-unmarked', 0)")
            ap.execSQL("INSERT INTO FeedMedia VALUES (21, 'https://example.org/played.mp3', 0, 1758000000000, 0, 11)")
            ap.execSQL("INSERT INTO FeedMedia VALUES (22, 'https://example.org/progress.mp3', 61000, 0, 0, 12)")
            ap.execSQL("INSERT INTO FeedMedia VALUES (23, 'https://example.org/new.mp3', 0, 0, 0, 13)")
            // Écouté puis démarqué côté AntennaPod : complétion sans lu, position gardée.
            ap.execSQL(
                "INSERT INTO FeedMedia VALUES (24, 'https://example.org/unmarked.mp3', 534, 1758000000000, 1758000000000, 14)"
            )
            ap.execSQL("INSERT INTO Queue VALUES (1, 12, 7)")
            ap.execSQL("INSERT INTO Queue VALUES (2, 11, 7)")
            ap.execSQL("INSERT INTO Favorites VALUES (1, 12, 7)")
        }
    }

    @After
    fun tearDown() {
        db.close()
        server.close()
    }

    @Test
    fun import_restoresEverything() = runTest {
        val summary = importer.import(backupFile.absolutePath)

        assertThat(summary.feedsAdded).isEqualTo(1)
        assertThat(summary.episodesMatched).isEqualTo(4)
        assertThat(summary.episodesPlayed).isEqualTo(1)
        assertThat(summary.episodesInProgress).isEqualTo(2)
        assertThat(summary.queued).isEqualTo(2)
        assertThat(summary.favorites).isEqualTo(1)

        val feed = db.feedDao().getByUrl(server.url("/feed.xml").toString())!!
        assertThat(feed.title).isEqualTo("Mon podcast")
        assertThat(feed.playbackSpeed).isEqualTo(1.5f)
        assertThat(feed.skipIntroMs).isEqualTo(10_000)

        val states = db.episodeDao().getLookup(feed.id).associate { it.guid to it.id }
        assertThat(db.episodeDao().get(states.getValue("g-played"))!!.state).isEqualTo(EpisodeState.PLAYED)
        val progress = db.episodeDao().get(states.getValue("g-progress"))!!
        assertThat(progress.state).isEqualTo(EpisodeState.IN_PROGRESS)
        assertThat(progress.positionMs).isEqualTo(61_000)
        assertThat(db.episodeDao().get(states.getValue("g-new"))!!.state).isEqualTo(EpisodeState.NEW)
        // Complétion sans lu : pas écouté, position reprise.
        val unmarked = db.episodeDao().get(states.getValue("g-unmarked"))!!
        assertThat(unmarked.state).isEqualTo(EpisodeState.IN_PROGRESS)
        assertThat(unmarked.positionMs).isEqualTo(534)

        val queue = db.queueDao().getEpisodeIds()
        assertThat(queue).containsExactly(states.getValue("g-progress"), states.getValue("g-played")).inOrder()
        assertThat(db.episodeDao().get(states.getValue("g-progress"))!!.isFavorite).isTrue()
    }

    @Test
    fun import_skipsExistingFeeds() = runTest {
        importer.import(backupFile.absolutePath)
        val second = importer.import(backupFile.absolutePath)

        assertThat(second.feedsAdded).isEqualTo(0)
        assertThat(second.feedsSkipped).isEqualTo(1)
    }
}
