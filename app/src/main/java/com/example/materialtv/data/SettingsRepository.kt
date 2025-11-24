package com.example.materialtv.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

class SettingsRepository(private val context: Context) {
    private val Context.settingsDataStore by preferencesDataStore(name = "settings")

    companion object {
        val MAX_CONCURRENT_DOWNLOADS = intPreferencesKey("max_concurrent_downloads")
        val DOWNLOAD_NOTIFICATIONS_ENABLED = booleanPreferencesKey("download_notifications_enabled")
    }

    val maxConcurrentDownloads = context.settingsDataStore.data.map { prefs ->
        prefs[MAX_CONCURRENT_DOWNLOADS] ?: 3
    }

    val downloadNotificationsEnabled = context.settingsDataStore.data.map { prefs ->
        prefs[DOWNLOAD_NOTIFICATIONS_ENABLED] ?: true
    }

    suspend fun setMaxConcurrentDownloads(value: Int) {
        context.settingsDataStore.updateData { prefs ->
            prefs.toMutablePreferences().apply { set(MAX_CONCURRENT_DOWNLOADS, value) }
        }
    }

    suspend fun setDownloadNotificationsEnabled(value: Boolean) {
        context.settingsDataStore.updateData { prefs ->
            prefs.toMutablePreferences().apply { set(DOWNLOAD_NOTIFICATIONS_ENABLED, value) }
        }
    }
}
