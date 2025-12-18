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
    val useVlcForDownloads: StateFlow<Boolean> = repository.useVlcForDownloads
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val autoPlayNextEpisode: StateFlow<Boolean> = repository.autoPlayNextEpisode
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val streamQuality: StateFlow<String> = repository.streamQuality
        .stateIn(viewModelScope, SharingStarted.Eagerly, "Original")
    val subtitleSize: StateFlow<String> = repository.subtitleSize
        .stateIn(viewModelScope, SharingStarted.Eagerly, "Normal")
    val defaultPlayer: StateFlow<com.hasanege.materialtv.data.PlayerPreference> = repository.defaultPlayerPreference
        .stateIn(viewModelScope, SharingStarted.Eagerly, com.hasanege.materialtv.data.PlayerPreference.HYBRID)
    val statsForNerds: StateFlow<Boolean> = repository.statsForNerds
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val experimentalDownloadReconnect: StateFlow<Boolean> = repository.experimentalDownloadReconnect
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val downloadAlgorithm: StateFlow<com.hasanege.materialtv.data.DownloadAlgorithm> = repository.downloadAlgorithm
        .stateIn(viewModelScope, SharingStarted.Eagerly, com.hasanege.materialtv.data.DownloadAlgorithm.OKHTTP)
    val language: StateFlow<String> = repository.language
        .stateIn(viewModelScope, SharingStarted.Eagerly, "system")
    val autoRetryFailedDownloads: StateFlow<Boolean> = repository.autoRetryFailedDownloads
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val useFFmpegDownloader: StateFlow<Boolean> = repository.useFFmpegDownloader
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setMaxConcurrentDownloads(value: Int) {
        viewModelScope.launch {
            repository.setMaxConcurrentDownloads(value)
        }
    }

    fun setDownloadNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setDownloadNotificationsEnabled(enabled) }
    }

    fun setUseVlcForDownloads(enabled: Boolean) {
        viewModelScope.launch { repository.setUseVlcForDownloads(enabled) }
    }
    
    fun setDownloadAlgorithm(algorithm: com.hasanege.materialtv.data.DownloadAlgorithm) {
        viewModelScope.launch { repository.setDownloadAlgorithm(algorithm) }
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

    fun setDefaultPlayerPreference(value: com.hasanege.materialtv.data.PlayerPreference) {
        viewModelScope.launch { repository.setDefaultPlayerPreference(value) }
    }

    fun setStatsForNerds(enabled: Boolean) {
        viewModelScope.launch { repository.setStatsForNerds(enabled) }
    }

    fun setExperimentalDownloadReconnect(enabled: Boolean) {
        viewModelScope.launch { repository.setExperimentalDownloadReconnect(enabled) }
    }

    fun setLanguage(value: String) {
        viewModelScope.launch { repository.setLanguage(value) }
    }

    fun setAutoRetryFailedDownloads(value: Boolean) {
        viewModelScope.launch {
            repository.setAutoRetryFailedDownloads(value)
        }
    }

    fun setUseFFmpegDownloader(value: Boolean) {
        viewModelScope.launch {
            repository.setUseFFmpegDownloader(value)
        }
    }

    val startPage: StateFlow<String> = repository.startPage
        .stateIn(viewModelScope, SharingStarted.Eagerly, "movies")

    fun setStartPage(value: String) {
        viewModelScope.launch {
            repository.setStartPage(value)
        }
    }

    fun clearWatchHistory() {
        viewModelScope.launch {
            com.hasanege.materialtv.WatchHistoryManager.clearHistory()
        }
    }
    
    // Auto-restart on speed drop
    val autoRestartOnSpeedDrop: StateFlow<Boolean> = repository.autoRestartOnSpeedDrop
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)
    
    fun setAutoRestartOnSpeedDrop(enabled: Boolean) {
        viewModelScope.launch {
            repository.setAutoRestartOnSpeedDrop(enabled)
        }
    }
    
    // Minimum download speed threshold (KB/s)
    val minDownloadSpeedKbps: StateFlow<Int> = repository.minDownloadSpeedKbps
        .stateIn(viewModelScope, SharingStarted.Eagerly, 100)
    
    fun setMinDownloadSpeedKbps(value: Int) {
        viewModelScope.launch {
            repository.setMinDownloadSpeedKbps(value)
        }
    }
    
    // Speed restart delay (seconds) - how long to wait before restart
    val speedRestartDelaySeconds: StateFlow<Int> = repository.speedRestartDelaySeconds
        .stateIn(viewModelScope, SharingStarted.Eagerly, 30)
    
    fun setSpeedRestartDelaySeconds(value: Int) {
        viewModelScope.launch {
            repository.setSpeedRestartDelaySeconds(value)
        }
    }

    // Continue Watching Threshold
    val nextEpisodeThresholdMinutes: StateFlow<Int> = repository.nextEpisodeThresholdMinutes
        .stateIn(viewModelScope, SharingStarted.Eagerly, 5)

    fun setNextEpisodeThresholdMinutes(value: Int) {
        viewModelScope.launch {
            repository.setNextEpisodeThresholdMinutes(value)
        }
    }
}
