package com.hasanege.materialtv.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hasanege.materialtv.data.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {
    val maxConcurrentDownloads: StateFlow<Int> = repository.maxConcurrentDownloads
        .stateIn(viewModelScope, SharingStarted.Eagerly, 3)
    val downloadNotificationsEnabled: StateFlow<Boolean> = repository.downloadNotificationsEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val autoPlayNextEpisode: StateFlow<Boolean> = repository.autoPlayNextEpisode
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val streamQuality: StateFlow<String> = repository.streamQuality
        .stateIn(viewModelScope, SharingStarted.Eagerly, "Original")
    val subtitleSize: StateFlow<String> = repository.subtitleSize
        .stateIn(viewModelScope, SharingStarted.Eagerly, "Normal")
    val defaultPlayer: StateFlow<String> = repository.defaultPlayer
        .stateIn(viewModelScope, SharingStarted.Eagerly, "VLC")
    val statsForNerds: StateFlow<Boolean> = repository.statsForNerds
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun setMaxConcurrentDownloads(value: Int) {
        viewModelScope.launch { repository.setMaxConcurrentDownloads(value) }
    }

    fun setDownloadNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setDownloadNotificationsEnabled(enabled) }
    }

    fun setAutoPlayNextEpisode(enabled: Boolean) {
        viewModelScope.launch { repository.setAutoPlayNextEpisode(enabled) }
    }

    fun setStreamQuality(value: String) {
        viewModelScope.launch { repository.setStreamQuality(value) }
    }

    fun setSubtitleSize(value: String) {
        viewModelScope.launch { repository.setSubtitleSize(value) }
    }

    fun setDefaultPlayer(value: String) {
        viewModelScope.launch { repository.setDefaultPlayer(value) }
    }

    fun setStatsForNerds(enabled: Boolean) {
        viewModelScope.launch { repository.setStatsForNerds(enabled) }
    }

    fun clearWatchHistory() {
        viewModelScope.launch {
            com.hasanege.materialtv.WatchHistoryManager.clearHistory()
        }
    }
}
