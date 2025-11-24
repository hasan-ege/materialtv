package com.hasanege.materialtv

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.hasanege.materialtv.data.PlaylistManager
import com.hasanege.materialtv.network.CredentialsManager

class MainApplication : Application(), ImageLoaderFactory {

    lateinit var credentialsManager: CredentialsManager
    lateinit var playlistManager: PlaylistManager

    override fun onCreate() {
        super.onCreate()
        credentialsManager = CredentialsManager(this)
        playlistManager = PlaylistManager(this)
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
}
