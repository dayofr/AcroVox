package com.acrovox.core.download

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.acrovox.core.database.AcroVoxDatabase
import com.acrovox.core.database.entity.DownloadEntity
import com.acrovox.core.database.entity.EpisodeEntity
import com.acrovox.core.database.entity.FeedEntity
import com.acrovox.core.model.DownloadStatus
import com.acrovox.core.model.EpisodeState
import com.google.common.truth.Truth.assertThat
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DownloadCleanerTest {
    private object NoScheduler : DownloadScheduler {
        override fun enqueue(episodeId: Long, wifiOnly: Boolean) = Unit

        override fun cancel(episodeId: Long) = Unit
    }

    private object Wifi : NetworkMonitor {
        override fun isUnmetered() = true

        override val unmetered: Flow<Boolean> = flowOf(true)
    }

    private val now = 10 * DAY
    private val clock = Clock.fixed(Instant.ofEpochMilli(now), ZoneOffset.UTC)
    private lateinit var db: AcroVoxDatabase
    private lateinit var settings: DownloadSettingsRepository
    private lateinit var files: DownloadFiles
    private lateinit var cleaner: DownloadCleaner
    private var feedId = 0L

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, AcroVoxDatabase::class.java).allowMainThreadQueries().build()
        settings = DownloadSettingsRepository(context)
        files = DownloadFiles(context)
        val manager = DownloadManager(db, NoScheduler, settings, Wifi, files, clock)
        cleaner = DownloadCleaner(db, settings, manager, clock)
        feedId = db.feedDao().insert(FeedEntity(feedUrl = "https://example.org/f", title = "P", subscribedAt = 0))
    }

    @After fun tearDown() = db.close()

    /** Épisode téléchargé dans l'état donné ; renvoie son fichier. */
    private suspend fun downloaded(
        guid: String,
        state: EpisodeState,
        completedAt: Long? = null,
        favorite: Boolean = false
    ): File {
        val id = db.episodeDao().mergeFromFeed(
            feedId,
            listOf(
                EpisodeEntity(feedId = feedId, guid = guid, title = guid, pubDate = 0, mediaUrl = "https://e/$guid.mp3")
            )
        ).single()
        db.episodeDao().setState(listOf(id), state)
        if (completedAt != null) db.episodeDao().markPlayed(id, completedAt)
        if (favorite) db.episodeDao().setFavorite(id, true)
        val file = files.finalFile(id, "https://e/$guid.mp3", null).apply { writeText("audio") }
        db.downloadDao().upsert(
            DownloadEntity(id, DownloadStatus.COMPLETED, localPath = file.path, totalBytes = 5, createdAt = 0)
        )
        return file
    }

    @Test
    fun ignoredIsDeleted_unplayedAndInProgressKept() = runTest {
        settings.setDeleteAfterPlayed(CleanupDelay.NEVER)
        val ignored = downloaded("a", EpisodeState.IGNORED)
        val kept = downloaded("b", EpisodeState.UNPLAYED)
        val started = downloaded("c", EpisodeState.IN_PROGRESS)

        assertThat(cleaner.clean()).isEqualTo(1)

        assertThat(ignored.exists()).isFalse()
        assertThat(kept.exists()).isTrue()
        assertThat(started.exists()).isTrue()
        assertThat(db.episodeActionDao().getPending(10)).isEmpty()
    }

    @Test
    fun playedDeletedAfterDelay() = runTest {
        settings.setDeleteAfterPlayed(CleanupDelay.ONE_DAY)
        val old = downloaded("old", EpisodeState.PLAYED, completedAt = now - 2 * DAY)
        val recent = downloaded("recent", EpisodeState.PLAYED, completedAt = now - DAY / 2)

        cleaner.clean()

        assertThat(old.exists()).isFalse()
        assertThat(recent.exists()).isTrue()
    }

    @Test
    fun neverKeepsPlayed_favoritesAlwaysKept() = runTest {
        settings.setDeleteAfterPlayed(CleanupDelay.NEVER)
        val played = downloaded("p", EpisodeState.PLAYED, completedAt = 0)
        settings.setDeleteAfterPlayed(CleanupDelay.IMMEDIATELY)
        val favorite = downloaded("f", EpisodeState.PLAYED, completedAt = 0, favorite = true)
        val ignoredFavorite = downloaded("i", EpisodeState.IGNORED, favorite = true)
        settings.setDeleteAfterPlayed(CleanupDelay.NEVER)

        cleaner.clean()

        assertThat(played.exists()).isTrue()
        assertThat(favorite.exists()).isTrue()
        assertThat(ignoredFavorite.exists()).isTrue()
    }

    @Test
    fun deletionKeepsEpisodeState() = runTest {
        settings.setDeleteAfterPlayed(CleanupDelay.IMMEDIATELY)
        downloaded("p", EpisodeState.PLAYED, completedAt = now)

        cleaner.clean()

        assertThat(db.downloadDao().getWithEpisodes()).isEmpty()
        assertThat(db.episodeDao().getWithFeed(listOf(1)).single().episode.state).isEqualTo(EpisodeState.PLAYED)
    }

    private companion object {
        const val DAY = 24 * 60 * 60 * 1000L
    }
}
