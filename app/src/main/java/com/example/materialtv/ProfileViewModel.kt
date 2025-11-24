
package com.example.materialtv

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.materialtv.data.Playlist
import com.example.materialtv.data.PlaylistManager
import com.example.materialtv.model.ContinueWatchingItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(private val playlistManager: PlaylistManager) : ViewModel() {

    private val _watchHistory = MutableStateFlow<List<ContinueWatchingItem>>(emptyList())
    val watchHistory: StateFlow<List<ContinueWatchingItem>> = _watchHistory

    private val _totalWatchTime = MutableStateFlow<Long>(0)
    val totalWatchTime: StateFlow<Long> = _totalWatchTime
    
    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists

    fun loadWatchHistory() {
        viewModelScope.launch {
            val history = WatchHistoryManager.getHistory()
            _watchHistory.value = history
            _totalWatchTime.value = history.sumOf { it.position }
        }
    }
    
    fun loadPlaylists() {
        _playlists.value = playlistManager.getPlaylists()
    }
    
    fun deletePlaylist(id: String) {
        playlistManager.removePlaylist(id)
        loadPlaylists()
    }
}

class ProfileViewModelFactory(private val application: MainApplication) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            return ProfileViewModel(application.playlistManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
