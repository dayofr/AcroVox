package com.acrovox.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.acrovox.core.data.refresh.RefreshSettingsRepository
import com.acrovox.core.data.refresh.RefreshWorker
import com.acrovox.core.download.CleanupWorker
import com.acrovox.core.download.DownloadCleaner
import com.acrovox.core.sync.SyncScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.runBlocking

@HiltAndroidApp
class AcroVoxApplication :
    Application(),
    Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory

    @Inject lateinit var downloadCleaner: DownloadCleaner

    @Inject lateinit var syncScheduler: SyncScheduler

    @Inject lateinit var refreshSettings: RefreshSettingsRepository

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        runBlocking { RefreshWorker.schedule(this@AcroVoxApplication, refreshSettings.current().intervalHours) }
        CleanupWorker.schedule(this)
        downloadCleaner.start()
        syncScheduler.start()
    }
}
