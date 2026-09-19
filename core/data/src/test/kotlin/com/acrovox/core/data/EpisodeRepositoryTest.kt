package com.acrovox.core.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.acrovox.core.data.repository.EpisodeRepository
import com.acrovox.core.data.repository.EpisodeSort
import com.acrovox.core.database.AcroVoxDatabase
import com.acrovox.core.database.entity.EpisodeEntity
import com.acrovox.core.database.entity.FeedEntity
import com.acrovox.core.model.EpisodeState
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EpisodeRepositoryTest {
    private lateinit var db: AcroVoxDatabase
    private lateinit var repository: EpisodeRepository
    private lateinit var ids: List<Long>

    @Before
    fun setUp() = runTest {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AcroVoxDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = EpisodeRepository(db, java.time.Clock.systemUTC())
        val feedId = db.feedDao().insert(FeedEntity(feedUrl = "https://example.org/f", title = "P", subscribedAt = 0))
        fun ep(guid: String, date: Long) = EpisodeEntity(
            feedId = feedId,
            guid = guid,
            title = guid,
            pubDate = date,
            mediaUrl = "https://example.org/$guid.mp3"
        )
        ids = db.episodeDao().mergeFromFeed(feedId, listOf(ep("new", 3), ep("played", 2), ep("ignored", 1)))
        db.episodeDao().markPlayed(ids[1], at = 0)
        db.episodeDao().setState(listOf(ids[2]), EpisodeState.IGNORED)
    }

    @After fun tearDown() = db.close()

    @Test
    fun addToQueue_keepsInboxEpisode_butNotPlayedOne() = runTest {
        repository.addToQueue(listOf(ids[0], ids[1]))

        assertThat(db.episodeDao().get(ids[0])!!.state).isEqualTo(EpisodeState.UNPLAYED)
        assertThat(db.episodeDao().get(ids[1])!!.state).isEqualTo(EpisodeState.PLAYED)
        assertThat(repository.observeQueuedIds().first()).containsExactly(ids[0], ids[1])
    }

    @Test
    fun latest_excludesIgnored_andSorts() = runTest {
        assertThat(
            repository.observeLatest(EpisodeSort.NEWEST_FIRST).first().map {
                it.episode.guid
            }
        ).containsExactly("new", "played").inOrder()
        assertThat(
            repository.observeLatest(EpisodeSort.OLDEST_FIRST).first().map {
                it.episode.guid
            }
        ).containsExactly("played", "new").inOrder()
    }

    @Test
    fun ignore_recordsDeleteAction_andLeavesQueue_undoRestoresEverything() = runTest {
        repository.addToQueue(listOf(ids[0]))

        val actionIds = repository.ignore(listOf(ids[0], ids[2]))

        assertThat(db.episodeDao().get(ids[0])!!.state).isEqualTo(EpisodeState.IGNORED)
        assertThat(repository.observeQueuedIds().first()).isEmpty()
        // ids[2] était déjà ignoré : une seule action.
        assertThat(actionIds).hasSize(1)
        val action = db.episodeActionDao().getPending(10).single()
        assertThat(action.action).isEqualTo(com.acrovox.core.model.EpisodeActionType.DELETE)
        assertThat(action.episodeUrl).isEqualTo("https://example.org/new.mp3")

        repository.undoIgnore(mapOf(ids[0] to EpisodeState.UNPLAYED), actionIds)

        assertThat(db.episodeDao().get(ids[0])!!.state).isEqualTo(EpisodeState.UNPLAYED)
        assertThat(db.episodeActionDao().count()).isEqualTo(0)
    }

    @Test
    fun restore_keepsEpisode_andRecordsNewAction() = runTest {
        repository.restore(listOf(ids[2], ids[0]))

        assertThat(db.episodeDao().get(ids[2])!!.state).isEqualTo(EpisodeState.UNPLAYED)
        assertThat(db.episodeDao().get(ids[0])!!.state).isEqualTo(EpisodeState.NEW)
        assertThat(
            db.episodeActionDao().getPending(10).map {
                it.action
            }
        ).containsExactly(com.acrovox.core.model.EpisodeActionType.NEW)
    }
}
