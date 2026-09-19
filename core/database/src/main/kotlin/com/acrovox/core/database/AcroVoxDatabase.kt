package com.acrovox.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.acrovox.core.database.dao.ChapterDao
import com.acrovox.core.database.dao.DownloadDao
import com.acrovox.core.database.dao.EpisodeActionDao
import com.acrovox.core.database.dao.EpisodeDao
import com.acrovox.core.database.dao.FeedDao
import com.acrovox.core.database.dao.PlaybackHistoryDao
import com.acrovox.core.database.dao.QueueDao
import com.acrovox.core.database.entity.ChapterEntity
import com.acrovox.core.database.entity.DownloadEntity
import com.acrovox.core.database.entity.EpisodeActionEntity
import com.acrovox.core.database.entity.EpisodeEntity
import com.acrovox.core.database.entity.FeedEntity
import com.acrovox.core.database.entity.PlaybackHistoryEntity
import com.acrovox.core.database.entity.QueueItemEntity

@Database(
    entities = [
        FeedEntity::class,
        EpisodeEntity::class,
        QueueItemEntity::class,
        DownloadEntity::class,
        ChapterEntity::class,
        EpisodeActionEntity::class,
        PlaybackHistoryEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AcroVoxDatabase : RoomDatabase() {
    abstract fun feedDao(): FeedDao

    abstract fun episodeDao(): EpisodeDao

    abstract fun queueDao(): QueueDao

    abstract fun downloadDao(): DownloadDao

    abstract fun chapterDao(): ChapterDao

    abstract fun episodeActionDao(): EpisodeActionDao

    abstract fun playbackHistoryDao(): PlaybackHistoryDao
}
