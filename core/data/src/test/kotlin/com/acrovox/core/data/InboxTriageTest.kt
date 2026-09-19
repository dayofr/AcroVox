package com.acrovox.core.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.acrovox.core.data.repository.EpisodeRepository
import com.acrovox.core.database.AcroVoxDatabase
import com.acrovox.core.database.entity.EpisodeEntity
import com.acrovox.core.database.entity.FeedEntity
import com.acrovox.core.model.EpisodeActionType
import com.acrovox.core.model.EpisodeState
import com.google.common.truth.Truth.assertThat
import java.time.Clock
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Critère de PULSE-20 : trier la boîte sans rien télécharger. */
@RunWith(RobolectricTestRunner::class)
class InboxTriageTest {
    private lateinit var db: AcroVoxDatabase
    private lateinit var repository: EpisodeRepository
    private lateinit var ids: List<Long>

    @Before
    fun setUp() = runTest {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AcroVoxDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = EpisodeRepository(db, Clock.systemUTC())
        val feedId = db.feedDao().insert(FeedEntity(feedUrl = "https://example.org/f", title = "P", subscribedAt = 0))
        ids = db.episodeDao().mergeFromFeed(
            feedId,
            (1..5).map {
                EpisodeEntity(
                    feedId = feedId,
                    guid = "e$it",
                    title = "e$it",
                    pubDate = it.toLong(),
                    mediaUrl = "https://example.org/e$it.mp3"
                )
            }
        )
    }

    @After fun tearDown() = db.close()

    @Test
    fun keepTwo_ignoreRest() = runTest {
        repository.addToQueue(ids.take(2))
        val rest = repository.observeInbox().first().map { it.episode.id }

        repository.ignore(rest)

        assertThat(repository.observeInbox().first()).isEmpty()
        assertThat(repository.observeQueuedIds().first()).containsExactlyElementsIn(ids.take(2))
        assertThat(ids.map { db.episodeDao().get(it)!!.state })
            .containsExactly(
                EpisodeState.UNPLAYED,
                EpisodeState.UNPLAYED,
                EpisodeState.IGNORED,
                EpisodeState.IGNORED,
                EpisodeState.IGNORED
            )
            .inOrder()
        val actions = db.episodeActionDao().getPending(10)
        assertThat(actions).hasSize(3)
        assertThat(actions.map { it.action }.toSet()).containsExactly(EpisodeActionType.DELETE)
        assertThat(db.downloadDao().observeAll().first()).isEmpty()
    }

    @Test
    fun undoKeep_putsEpisodeBackInInbox() = runTest {
        val previous = repository.statesOf(ids.take(1))
        repository.addToQueue(ids.take(1))

        repository.undoKeep(previous)

        assertThat(repository.observeQueuedIds().first()).isEmpty()
        assertThat(db.episodeDao().get(ids[0])!!.state).isEqualTo(EpisodeState.NEW)
        assertThat(repository.observeInboxCount().first()).isEqualTo(5)
    }
}
