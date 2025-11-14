
package com.example.materialtv

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.materialtv.model.ContinueWatchingItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {

    private val _watchHistory = MutableStateFlow<List<ContinueWatchingItem>>(emptyList())
    val watchHistory: StateFlow<List<ContinueWatchingItem>> = _watchHistory

    private val _totalWatchTime = MutableStateFlow<Long>(0)
    val totalWatchTime: StateFlow<Long> = _totalWatchTime

    fun loadWatchHistory() {
        viewModelScope.launch {
            val history = WatchHistoryManager.getHistory()
            _watchHistory.value = history
            _totalWatchTime.value = history.sumOf { it.position }
        }
    }
}

object ProfileViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            return ProfileViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
