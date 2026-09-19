package com.acrovox.core.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.acrovox.core.model.EpisodeState
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EpisodeDaoTest : DatabaseTest() {
    private val dao get() = db.episodeDao()

    @Test
    fun merge_insertsUnknownEpisodesAsNew() = runTest {
        val feedId = insertFeed()

        val inserted = dao.mergeFromFeed(feedId, listOf(episode("a"), episode("b")))

        assertThat(inserted).hasSize(2)
        val states = dao.observeByFeed(feedId).first().map { it.state }
        assertThat(states).containsExactly(EpisodeState.NEW, EpisodeState.NEW)
    }

    @Test
    fun merge_updatesMetadataButKeepsUserState() = runTest {
        val feedId = insertFeed()
        val id = dao.mergeFromFeed(feedId, listOf(episode("a", title = "Ancien titre"))).single()
        dao.setState(listOf(id), EpisodeState.IGNORED)
        dao.updatePosition(id, positionMs = 42_000, at = 1)
        dao.setFavorite(id, true)

        val inserted = dao.mergeFromFeed(feedId, listOf(episode("a", title = "Nouveau titre")))

        assertThat(inserted).isEmpty()
        val merged = dao.get(id)!!
        assertThat(merged.title).isEqualTo("Nouveau titre")
        assertThat(merged.positionMs).isEqualTo(42_000)
        assertThat(merged.isFavorite).isTrue()
        // updatePosition fait passer en IN_PROGRESS ; la fusion ne doit pas le remettre à NEW.
        assertThat(merged.state).isEqualTo(EpisodeState.IN_PROGRESS)
    }

    @Test
    fun sameGuidInTwoFeeds_isTwoEpisodes() = runTest {
        val a = insertFeed("https://a.org/feed")
        val b = insertFeed("https://b.org/feed")

        dao.mergeFromFeed(a, listOf(episode("x")))
        dao.mergeFromFeed(b, listOf(episode("x")))

        assertThat(dao.getInboxIds()).hasSize(2)
    }

    @Test
    fun inbox_showsOnlyNewEpisodes_newestFirst() = runTest {
        val feedId = insertFeed()
        val ids = dao.mergeFromFeed(
            feedId,
            listOf(episode("old", pubDate = 1), episode("recent", pubDate = 2), episode("ignored", pubDate = 3))
        )

        dao.observeInbox().test {
            assertThat(awaitItem().map { it.episode.guid }).containsExactly("ignored", "recent", "old").inOrder()
            dao.setState(listOf(ids[2]), EpisodeState.IGNORED)
            assertThat(awaitItem().map { it.episode.guid }).containsExactly("recent", "old").inOrder()
        }
    }

    @Test
    fun ignoringTheRest_emptiesInbox_andHidesFromLatest() = runTest {
        val feedId = insertFeed()
        val ids = dao.mergeFromFeed(feedId, (1..5).map { episode("e$it", pubDate = it.toLong()) })
        dao.setState(ids.take(2), EpisodeState.UNPLAYED)

        dao.setState(dao.getInboxIds(), EpisodeState.IGNORED)

        assertThat(dao.getInboxIds()).isEmpty()
        assertThat(dao.observeLatest(10).first().map { it.episode.id }).containsExactlyElementsIn(ids.take(2))
    }

    @Test
    fun updatePosition_keepsPlayedState() = runTest {
        val feedId = insertFeed()
        val id = dao.mergeFromFeed(feedId, listOf(episode("a"))).single()
        dao.markPlayed(id, at = 10)

        dao.updatePosition(id, positionMs = 5_000, at = 11)

        assertThat(dao.get(id)!!.state).isEqualTo(EpisodeState.PLAYED)
    }

    @Test
    fun feedWithNewCount_countsInboxOnly() = runTest {
        val feedId = insertFeed()
        val ids = dao.mergeFromFeed(feedId, listOf(episode("a"), episode("b"), episode("c")))
        dao.setState(listOf(ids[0]), EpisodeState.IGNORED)

        val feed = db.feedDao().observeAllWithNewCount().first().single()

        assertThat(feed.newCount).isEqualTo(2)
    }

    @Test
    fun deletingFeed_cascadesToEpisodes() = runTest {
        val feedId = insertFeed()
        dao.mergeFromFeed(feedId, listOf(episode("a")))

        db.feedDao().delete(feedId)

        assertThat(dao.getInboxIds()).isEmpty()
    }
}
