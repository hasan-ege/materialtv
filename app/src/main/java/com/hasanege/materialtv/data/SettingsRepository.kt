package com.hasanege.materialtv.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Singleton DataStore instance
private val Context.settingsDataStore by preferencesDataStore(name = "settings")

class SettingsRepository private constructor(private val context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: SettingsRepository? = null

        fun getInstance(context: Context): SettingsRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SettingsRepository(context.applicationContext).also { INSTANCE = it }
            }
        }

        val MAX_CONCURRENT_DOWNLOADS = intPreferencesKey("max_concurrent_downloads")
        val DOWNLOAD_NOTIFICATIONS_ENABLED = booleanPreferencesKey("download_notifications_enabled")
        val AUTO_PLAY_NEXT_EPISODE = booleanPreferencesKey("auto_play_next_episode")
        val STREAM_QUALITY = stringPreferencesKey("stream_quality")
        val SUBTITLE_SIZE = stringPreferencesKey("subtitle_size")
        val DEFAULT_PLAYER = stringPreferencesKey("default_player")
        val STATS_FOR_NERDS = booleanPreferencesKey("stats_for_nerds")
    }

    val maxConcurrentDownloads: Flow<Int> = context.settingsDataStore.data.map { prefs ->
        prefs[MAX_CONCURRENT_DOWNLOADS] ?: 3
    }

    val downloadNotificationsEnabled: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[DOWNLOAD_NOTIFICATIONS_ENABLED] ?: true
    }

    val autoPlayNextEpisode: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[AUTO_PLAY_NEXT_EPISODE] ?: true
    }

    val streamQuality: Flow<String> = context.settingsDataStore.data.map { prefs ->
        prefs[STREAM_QUALITY] ?: "Original"
    }

    val subtitleSize: Flow<String> = context.settingsDataStore.data.map { prefs ->
        prefs[SUBTITLE_SIZE] ?: "Normal"
    }

    val defaultPlayer: Flow<String> = context.settingsDataStore.data.map { prefs ->
        prefs[DEFAULT_PLAYER] ?: "VLC"
    }

    val statsForNerds: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[STATS_FOR_NERDS] ?: false
    }

    suspend fun setMaxConcurrentDownloads(value: Int) {
        context.settingsDataStore.edit { prefs ->
            prefs[MAX_CONCURRENT_DOWNLOADS] = value
        }
    }

    suspend fun setDownloadNotificationsEnabled(value: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[DOWNLOAD_NOTIFICATIONS_ENABLED] = value
        }
    }

    suspend fun setAutoPlayNextEpisode(value: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[AUTO_PLAY_NEXT_EPISODE] = value
        }
    }

    suspend fun setStreamQuality(value: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[STREAM_QUALITY] = value
        }
    }

    suspend fun setSubtitleSize(value: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[SUBTITLE_SIZE] = value
        }
    }

    suspend fun setDefaultPlayer(value: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[DEFAULT_PLAYER] = value
        }
    }

    suspend fun setStatsForNerds(value: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[STATS_FOR_NERDS] = value
        }
    }
}
