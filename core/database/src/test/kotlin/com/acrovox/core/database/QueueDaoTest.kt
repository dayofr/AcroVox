package com.acrovox.core.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QueueDaoTest : DatabaseTest() {
    private val queue get() = db.queueDao()
    private lateinit var ids: List<Long>

    @Before
    fun seed() = runTest {
        val feedId = insertFeed()
        ids = db.episodeDao().mergeFromFeed(feedId, listOf(episode("a"), episode("b"), episode("c"), episode("d")))
    }

    @Test
    fun add_appendsAndIgnoresDuplicates() = runTest {
        queue.add(listOf(ids[0], ids[1]))
        queue.add(listOf(ids[1], ids[2]))

        assertThat(queue.getEpisodeIds()).containsExactly(ids[0], ids[1], ids[2]).inOrder()
    }

    @Test
    fun add_first_putsAtHead() = runTest {
        queue.add(listOf(ids[0], ids[1]))
        queue.add(listOf(ids[2]), first = true)

        assertThat(queue.getEpisodeIds()).containsExactly(ids[2], ids[0], ids[1]).inOrder()
    }

    @Test
    fun move_reorders() = runTest {
        queue.add(ids)

        queue.move(from = 3, to = 0)

        assertThat(queue.getEpisodeIds()).containsExactly(ids[3], ids[0], ids[1], ids[2]).inOrder()
    }

    @Test
    fun move_outOfRange_isNoOp() = runTest {
        queue.add(ids)

        queue.move(from = 0, to = 10)

        assertThat(queue.getEpisodeIds()).containsExactlyElementsIn(ids).inOrder()
    }

    @Test
    fun remove_keepsOrderWithoutGaps() = runTest {
        queue.add(ids)

        queue.remove(listOf(ids[1]))

        assertThat(queue.getEpisodeIds()).containsExactly(ids[0], ids[2], ids[3]).inOrder()
        assertThat(queue.observeQueue().first().map { it.episode.id }).containsExactly(ids[0], ids[2], ids[3]).inOrder()
    }
}
