package com.acrovox.core.database.di

import android.content.Context
import androidx.room.Room
import com.acrovox.core.database.AcroVoxDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseModule {
    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): AcroVoxDatabase =
        Room.databaseBuilder(context, AcroVoxDatabase::class.java, "acrovox.db").build()

    @Provides fun feedDao(db: AcroVoxDatabase) = db.feedDao()

    @Provides fun episodeDao(db: AcroVoxDatabase) = db.episodeDao()

    @Provides fun queueDao(db: AcroVoxDatabase) = db.queueDao()

    @Provides fun downloadDao(db: AcroVoxDatabase) = db.downloadDao()

    @Provides fun chapterDao(db: AcroVoxDatabase) = db.chapterDao()

    @Provides fun episodeActionDao(db: AcroVoxDatabase) = db.episodeActionDao()

    @Provides fun playbackHistoryDao(db: AcroVoxDatabase) = db.playbackHistoryDao()
}
