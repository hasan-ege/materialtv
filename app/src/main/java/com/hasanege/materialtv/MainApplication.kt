package com.hasanege.materialtv

import android.app.Application
import androidx.work.Configuration
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.hasanege.materialtv.data.PlaylistManager
import com.hasanege.materialtv.data.DownloadNotificationManager
import com.hasanege.materialtv.network.CredentialsManager

class MainApplication : Application(), ImageLoaderFactory, Configuration.Provider {

    lateinit var credentialsManager: CredentialsManager
    lateinit var playlistManager: PlaylistManager

    override fun onCreate() {
        super.onCreate()
        credentialsManager = CredentialsManager(this)
        playlistManager = PlaylistManager(this)
        
        // Initialize download notification channel
        DownloadNotificationManager.createNotificationChannel(this)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(this.cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.02)
                    .build()
            }
            .build()
    }
    
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            // Use default executor - queue management is handled by DownloadQueueProcessor
            .build()
}
