package com.example.materialtv.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.materialtv.data.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {
    val maxConcurrentDownloads: StateFlow<Int> = repository.maxConcurrentDownloads
        .stateIn(viewModelScope, SharingStarted.Eagerly, 3)
    val downloadNotificationsEnabled: StateFlow<Boolean> = repository.downloadNotificationsEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    fun setMaxConcurrentDownloads(value: Int) {
        viewModelScope.launch { repository.setMaxConcurrentDownloads(value) }
    }

    fun setDownloadNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setDownloadNotificationsEnabled(enabled) }
    }
}
