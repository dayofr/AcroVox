package com.acrovox.core.player.di

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

private const val STREAM_CACHE_BYTES = 200L * 1024 * 1024

@Module
@InstallIn(SingletonComponent::class)
object PlayerModule {
    /** Cache disque du streaming : revenir en arrière ne retélécharge pas. Distinct des téléchargements. */
    @OptIn(UnstableApi::class)
    @Provides
    @Singleton
    fun streamCache(@ApplicationContext context: Context): Cache = SimpleCache(
        File(context.cacheDir, "stream"),
        LeastRecentlyUsedCacheEvictor(STREAM_CACHE_BYTES),
        StandaloneDatabaseProvider(context)
    )
}
