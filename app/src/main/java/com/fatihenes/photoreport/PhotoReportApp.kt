package com.fatihenes.photoreport

import android.app.Application
import android.content.Context
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.video.VideoFrameDecoder
import coil3.request.crossfade
import coil3.memory.MemoryCache
import coil3.disk.DiskCache
import okio.Path.Companion.toPath
import android.os.StrictMode

import dagger.hilt.android.HiltAndroidApp
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.fatihenes.photoreport.worker.TrashCleanupWorker
import com.google.firebase.crashlytics.FirebaseCrashlytics

import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class PhotoReportApp : Application(), SingletonImageLoader.Factory, Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        val constraints = androidx.work.Constraints.Builder()
            .setRequiresBatteryNotLow(requiresBatteryNotLow = true)
            .build()

        val cleanupRequest = PeriodicWorkRequestBuilder<TrashCleanupWorker>(24, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "TrashCleanupWork",
            ExistingPeriodicWorkPolicy.KEEP,
            cleanupRequest,
        )

        // Crashlytics configuration - Only if Firebase is initialized
        try {
            com.google.firebase.FirebaseApp.getInstance()
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
        } catch (_: Exception) {
            // Firebase not initialized, skip Crashlytics setup
        }

        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectAll() // Detects disk reads, writes, network, and custom slow calls
                    .penaltyLog()
                    .penaltyFlashScreen()
                    .build()
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectLeakedRegistrationObjects()
                    .detectActivityLeaks()
                    .detectFileUriExposure()
                    .detectContentUriWithoutPermission()
                    .penaltyLog()
                    .build()
            )
        }
    }

    override fun newImageLoader(context: Context): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache").absolutePath.toPath())
                    .maxSizePercent(0.10)
                    .build()
            }
            .crossfade(true)
            .build()
    }
}
