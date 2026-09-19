package com.acrovox.core.network.di

import com.acrovox.core.network.itunes.ItunesSearchClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import okhttp3.HttpUrl

@Module
@InstallIn(SingletonComponent::class)
object ItunesModule {
    @Provides
    @Named(ItunesSearchClient.BASE_URL_NAME)
    fun itunesBaseUrl(): HttpUrl = ItunesSearchClient.DEFAULT_BASE_URL
}
