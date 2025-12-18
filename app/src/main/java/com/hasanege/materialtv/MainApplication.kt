package com.hasanege.materialtv

import android.app.Application
import androidx.work.Configuration
import com.hasanege.materialtv.data.SettingsRepository
import com.hasanege.materialtv.data.settingsDataStore
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.hasanege.materialtv.data.PlaylistManager
import com.hasanege.materialtv.network.CredentialsManager
import com.hasanege.materialtv.utils.LanguageManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class MainApplication : Application(), ImageLoaderFactory, Configuration.Provider {

    lateinit var credentialsManager: CredentialsManager
    lateinit var playlistManager: PlaylistManager

    companion object {
        lateinit var instance: MainApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        credentialsManager = CredentialsManager(this)
        playlistManager = PlaylistManager(this)
        FavoritesManager.initialize(this)
        WatchHistoryManager.initialize(this)
        applySavedLanguage()
    }

    private fun applySavedLanguage() {
        runBlocking {
            val prefs = settingsDataStore.data.first()
            val languageCode = prefs[SettingsRepository.LANGUAGE] ?: "system"
            LanguageManager.applyLanguage(languageCode)
        }
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
