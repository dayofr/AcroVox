package com.acrovox.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.acrovox.core.database.entity.EpisodeEntity
import com.acrovox.core.database.entity.FeedEntity
import org.junit.After
import org.junit.Before

/** Base en mémoire recréée pour chaque test. */
abstract class DatabaseTest {
    protected lateinit var db: AcroVoxDatabase

    @Before
    fun createDb() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AcroVoxDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() = db.close()

    protected suspend fun insertFeed(url: String = "https://example.org/feed.xml", title: String = "Podcast"): Long =
        db.feedDao().insert(FeedEntity(feedUrl = url, title = title, subscribedAt = 0))

    protected fun episode(guid: String, pubDate: Long = 0, title: String = guid) = EpisodeEntity(
        feedId = 0,
        guid = guid,
        title = title,
        pubDate = pubDate,
        mediaUrl = "https://example.org/$guid.mp3"
    )
}
