package com.acrovox.core.data.di

import com.acrovox.core.data.repository.ItunesSearchRepository
import com.acrovox.core.data.repository.SearchRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataModule {
    @Binds
    abstract fun searchRepository(impl: ItunesSearchRepository): SearchRepository
}
