package com.acrovox.core.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.acrovox.core.database.entity.EpisodeActionEntity
import com.acrovox.core.model.EpisodeActionType
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EpisodeActionDaoTest : DatabaseTest() {
    private val dao get() = db.episodeActionDao()

    private fun action(episode: String, at: Long) = EpisodeActionEntity(
        podcastUrl = "https://example.org/feed.xml",
        episodeUrl = "https://example.org/$episode.mp3",
        action = EpisodeActionType.DELETE,
        timestamp = at
    )

    @Test
    fun pending_isOrderedByTimestamp_andSurvivesEpisodeDeletion() = runTest {
        val feedId = insertFeed()
        dao.insertAll(listOf(action("b", at = 2), action("a", at = 1)))

        db.feedDao().delete(feedId)

        assertThat(dao.getPending(10).map { it.episodeUrl })
            .containsExactly("https://example.org/a.mp3", "https://example.org/b.mp3").inOrder()
    }

    @Test
    fun delete_removesSentActions() = runTest {
        val ids = dao.insertAll(listOf(action("a", 1), action("b", 2), action("c", 3)))

        dao.delete(ids.take(2))

        assertThat(dao.count()).isEqualTo(1)
    }
}
