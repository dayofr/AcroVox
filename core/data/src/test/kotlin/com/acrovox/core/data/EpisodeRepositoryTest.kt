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

    @Test
    fun complete_marksPlayedAndLeavesQueue_nextFollowsQueueOrder() = runTest {
        repository.addToQueue(listOf(ids[0], ids[1]))

        assertThat(repository.nextInQueue(ids[0])).isEqualTo(ids[1])
        assertThat(repository.previousInQueue(ids[1])).isEqualTo(ids[0])
        // Un épisode hors file enchaîne sur la tête de file.
        assertThat(repository.nextInQueue(ids[2])).isEqualTo(ids[0])

        repository.complete(ids[0])

        assertThat(db.episodeDao().get(ids[0])!!.state).isEqualTo(EpisodeState.PLAYED)
        assertThat(repository.observeQueuedIds().first()).containsExactly(ids[1])
        assertThat(repository.nextInQueue(ids[1])).isNull()
    }

    @Test
    fun listening_recordsPlayAction_savesPositionAndHistory() = runTest {
        repository.onPlaybackStarted(ids[0], 0)
        repository.savePosition(ids[0], 125_000)
        repository.recordListening(ids[0], startedMs = 5_000, positionMs = 125_000, totalMs = 3_600_000)
        repository.recordListening(ids[0], startedMs = 10_000, positionMs = 10_000)

        assertThat(db.episodeDao().get(ids[0])!!.state).isEqualTo(EpisodeState.IN_PROGRESS)
        assertThat(db.episodeDao().get(ids[0])!!.positionMs).isEqualTo(125_000)
        val action = db.episodeActionDao().getPending(10).single()
        assertThat(action.action).isEqualTo(com.acrovox.core.model.EpisodeActionType.PLAY)
        assertThat(action.started).isEqualTo(5)
        assertThat(action.position).isEqualTo(125)
        assertThat(action.total).isEqualTo(3600)
        assertThat(repository.observeHistory().first().map { it.episode.id }).containsExactly(ids[0])
    }
}
