package com.acrovox.core.download

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.acrovox.core.database.AcroVoxDatabase
import com.acrovox.core.database.entity.EpisodeEntity
import com.acrovox.core.database.entity.FeedEntity
import com.acrovox.core.model.DownloadState
import com.acrovox.core.model.DownloadStatus
import com.google.common.truth.Truth.assertThat
import java.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DownloadManagerTest {
    private class FakeScheduler : DownloadScheduler {
        val enqueued = mutableMapOf<Long, Boolean>()
        val cancelled = mutableListOf<Long>()

        override fun enqueue(episodeId: Long, wifiOnly: Boolean) {
            enqueued[episodeId] = wifiOnly
        }

        override fun cancel(episodeId: Long) {
            enqueued.remove(episodeId)
            cancelled += episodeId
        }
    }

    private class FakeNetwork(var wifi: Boolean) : NetworkMonitor {
        override fun isUnmetered() = wifi

        override val unmetered: Flow<Boolean> get() = flowOf(wifi)
    }

    private lateinit var db: AcroVoxDatabase
    private lateinit var settings: DownloadSettingsRepository
    private val scheduler = FakeScheduler()
    private val network = FakeNetwork(wifi = false)
    private lateinit var manager: DownloadManager
    private lateinit var ids: List<Long>

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, AcroVoxDatabase::class.java).allowMainThreadQueries().build()
        settings = DownloadSettingsRepository(context)
        settings.setWifiOnly(true)
        settings.setDownloadedOnly(false)
        manager = DownloadManager(db, scheduler, settings, network, DownloadFiles(context), Clock.systemUTC())
        val feedId = db.feedDao().insert(FeedEntity(feedUrl = "https://example.org/f", title = "P", subscribedAt = 0))
        ids = db.episodeDao().mergeFromFeed(
            feedId,
            (1..3).map {
                EpisodeEntity(
                    feedId = feedId,
                    guid = "e$it",
                    title = "e$it",
                    pubDate = 0,
                    mediaUrl = "https://e/$it.mp3"
                )
            }
        )
    }

    @After fun tearDown() = db.close()

    @Test
    fun wifiOnly_offWifi_asksBeforeDownloading() = runTest {
        manager.request(ids.take(2))

        assertThat(manager.prompt.value).isEqualTo(DownloadPrompt.ChooseNetwork(ids.take(2)))
        assertThat(scheduler.enqueued).isEmpty()
    }

    @Test
    fun choosingWaitForWifi_queuesWithWifiConstraint() = runTest {
        manager.waitForWifi(ids.take(2))

        assertThat(manager.prompt.value).isNull()
        assertThat(scheduler.enqueued).containsExactly(ids[0], true, ids[1], true)
        assertThat(manager.observeState(ids[0]).first()).isEqualTo(DownloadState.Queued(waitingForWifi = true))
    }

    @Test
    fun choosingDownloadNow_usesAnyNetwork() = runTest {
        manager.downloadNow(ids.take(1))

        assertThat(scheduler.enqueued).containsExactly(ids[0], false)
        assertThat(db.downloadDao().get(ids[0])!!.status).isEqualTo(DownloadStatus.QUEUED)
    }

    @Test
    fun onWifi_downloadsWithoutAsking() = runTest {
        network.wifi = true

        manager.request(ids.take(1))

        assertThat(manager.prompt.value).isNull()
        assertThat(scheduler.enqueued).containsExactly(ids[0], true)
    }

    @Test
    fun wifiOnlyDisabled_downloadsOnMobileData() = runTest {
        settings.setWifiOnly(false)

        manager.request(ids.take(1))

        assertThat(manager.prompt.value).isNull()
        assertThat(scheduler.enqueued).containsExactly(ids[0], false)
    }

    @Test
    fun toggleOnQueued_cancels() = runTest {
        manager.waitForWifi(ids.take(1))

        manager.toggle(ids[0])

        assertThat(scheduler.cancelled).containsExactly(ids[0])
        assertThat(db.downloadDao().get(ids[0])).isNull()
    }

    @Test
    fun downloadedOnly_blocksStreaming() = runTest {
        settings.setDownloadedOnly(true)

        assertThat(manager.checkPlayable(ids[0])).isFalse()
        assertThat(manager.prompt.value).isEqualTo(DownloadPrompt.NotDownloaded(ids[0]))

        settings.setDownloadedOnly(false)
        assertThat(manager.checkPlayable(ids[1])).isTrue()
    }

    @Test
    fun deleteKeepsEpisodeState() = runTest {
        val file = DownloadFiles(ApplicationProvider.getApplicationContext()).finalFile(ids[0], "https://e/1.mp3", null)
        file.writeText("audio")
        manager.downloadNow(ids.take(1))
        db.downloadDao().complete(ids[0], file.path, 5, 1)
        settings.setDownloadedOnly(true)
        assertThat(manager.checkPlayable(ids[0])).isTrue()

        manager.delete(ids.take(1))

        assertThat(file.exists()).isFalse()
        assertThat(db.downloadDao().get(ids[0])).isNull()
        assertThat(db.episodeActionDao().getPending(10)).isEmpty()
    }
}
