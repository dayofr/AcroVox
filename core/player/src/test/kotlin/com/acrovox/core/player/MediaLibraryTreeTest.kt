package com.acrovox.core.player

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.acrovox.core.data.repository.EpisodeRepository
import com.acrovox.core.database.AcroVoxDatabase
import com.acrovox.core.database.entity.EpisodeEntity
import com.acrovox.core.database.entity.FeedEntity
import com.acrovox.core.download.DownloadFiles
import com.acrovox.core.download.DownloadManager
import com.acrovox.core.download.DownloadScheduler
import com.acrovox.core.download.DownloadSettingsRepository
import com.acrovox.core.download.NetworkMonitor
import com.acrovox.core.model.EpisodeState
import com.google.common.truth.Truth.assertThat
import java.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MediaLibraryTreeTest {
    private object NoScheduler : DownloadScheduler {
        override fun enqueue(episodeId: Long, wifiOnly: Boolean) = Unit

        override fun cancel(episodeId: Long) = Unit
    }

    private object Wifi : NetworkMonitor {
        override fun isUnmetered() = true

        override val unmetered: Flow<Boolean> = flowOf(true)
    }

    private lateinit var db: AcroVoxDatabase
    private lateinit var settings: DownloadSettingsRepository
    private lateinit var episodes: EpisodeRepository
    private lateinit var tree: MediaLibraryTree
    private lateinit var ids: List<Long>
    private var feedId = 0L

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, AcroVoxDatabase::class.java).allowMainThreadQueries().build()
        settings = DownloadSettingsRepository(context).apply { setDownloadedOnly(false) }
        episodes = EpisodeRepository(db, Clock.systemUTC())
        val downloads = DownloadManager(db, NoScheduler, settings, Wifi, DownloadFiles(context), Clock.systemUTC())
        tree = MediaLibraryTree(db, episodes, downloads, settings)
        feedId = db.feedDao().insert(FeedEntity(feedUrl = "https://e/f", title = "Underscore_", subscribedAt = 0))
        ids = db.episodeDao().mergeFromFeed(
            feedId,
            listOf("Les puces RISC-V", "L'IA et le code", "Les écrans pliants").mapIndexed { i, title ->
                EpisodeEntity(
                    feedId = feedId,
                    guid = "g$i",
                    title = title,
                    pubDate = i.toLong(),
                    mediaUrl = "https://e/$i.mp3"
                )
            }
        )
    }

    @After fun tearDown() = db.close()

    @Test
    fun root_listsFourFolders() = runTest {
        val folders = tree.children(MediaLibraryTree.ROOT)!!

        assertThat(folders.map { it.mediaMetadata.title.toString() })
            .containsExactly("File d'attente", "Boîte de réception", "Téléchargements", "Abonnements").inOrder()
        assertThat(folders.all { it.mediaMetadata.isBrowsable == true }).isTrue()
    }

    @Test
    fun feeds_leadToPlayableEpisodes_withoutIgnored() = runTest {
        episodes.ignore(listOf(ids[0]))

        val feed = tree.children(MediaLibraryTree.FEEDS)!!.single()
        val items = tree.children(feed.mediaId)!!

        assertThat(feed.mediaMetadata.title.toString()).isEqualTo("Underscore_")
        assertThat(items.map { it.mediaId }).containsExactly(ids[2].toString(), ids[1].toString()).inOrder()
        assertThat(items.first().mediaMetadata.isPlayable).isTrue()
    }

    @Test
    fun queueAndInbox() = runTest {
        episodes.addToQueue(listOf(ids[1]))

        assertThat(tree.children(MediaLibraryTree.QUEUE)!!.map { it.mediaId }).containsExactly(ids[1].toString())
        assertThat(tree.children(MediaLibraryTree.INBOX)!!.map { it.mediaId })
            .containsExactly(ids[2].toString(), ids[0].toString()).inOrder()
    }

    @Test
    fun offlineOnly_hidesEpisodesNotDownloaded() = runTest {
        settings.setDownloadedOnly(true)

        assertThat(tree.children(MediaLibraryTree.INBOX)).isEmpty()
        assertThat(tree.episodeForVoice(null)).isNull()
    }

    @Test
    fun voiceSearch_matchesEpisodeOrPodcastTitle() = runTest {
        assertThat(tree.episodeForVoice("RISC")).isEqualTo(ids[0])
        // Le nom du podcast suffit : le plus récent est choisi.
        assertThat(tree.episodeForVoice("underscore")).isEqualTo(ids[2])
        assertThat(tree.search("inconnu")).isEmpty()
    }

    @Test
    fun voiceWithoutQuery_resumesThenQueue() = runTest {
        episodes.addToQueue(listOf(ids[1]))
        assertThat(tree.episodeForVoice("")).isEqualTo(ids[1])

        db.episodeDao().setState(listOf(ids[2]), EpisodeState.IN_PROGRESS)
        episodes.savePosition(ids[2], 60_000)
        assertThat(tree.episodeForVoice(null)).isEqualTo(ids[2])
    }

    @Test
    fun unknownIds() = runTest {
        assertThat(tree.children("nope")).isNull()
        assertThat(tree.item("feed:999")).isNull()
        assertThat(tree.item(ids[0].toString())!!.mediaMetadata.title.toString()).isEqualTo("Les puces RISC-V")
    }
}
