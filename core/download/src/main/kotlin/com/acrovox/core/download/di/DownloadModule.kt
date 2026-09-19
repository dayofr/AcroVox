package com.acrovox.core.download.di

import com.acrovox.core.download.ConnectivityNetworkMonitor
import com.acrovox.core.download.DownloadScheduler
import com.acrovox.core.download.NetworkMonitor
import com.acrovox.core.download.WorkManagerDownloadScheduler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DownloadModule {
    @Binds
    abstract fun scheduler(impl: WorkManagerDownloadScheduler): DownloadScheduler

    @Binds
    abstract fun networkMonitor(impl: ConnectivityNetworkMonitor): NetworkMonitor
}
