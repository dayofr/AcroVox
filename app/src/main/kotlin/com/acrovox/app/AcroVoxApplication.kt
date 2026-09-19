package com.acrovox.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.acrovox.core.data.refresh.RefreshWorker
import com.acrovox.core.download.CleanupWorker
import com.acrovox.core.download.DownloadCleaner
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class AcroVoxApplication :
    Application(),
    Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory

    @Inject lateinit var downloadCleaner: DownloadCleaner

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        RefreshWorker.schedule(this)
        CleanupWorker.schedule(this)
        downloadCleaner.start()
    }
}
