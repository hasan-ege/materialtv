
package com.hasanege.materialtv

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hasanege.materialtv.data.Playlist
import com.hasanege.materialtv.data.PlaylistManager
import com.hasanege.materialtv.model.ContinueWatchingItem
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
            val history = WatchHistoryManager.getFullHistory()
            _watchHistory.value = history
            _totalWatchTime.value = WatchHistoryManager.getTotalActualWatchTime()
        }
    }
    
    fun loadPlaylists() {
        _playlists.value = playlistManager.getPlaylists()
    }
    
    fun deletePlaylist(id: String) {
        playlistManager.removePlaylist(id)
        loadPlaylists()
    }

    fun addPlaylist(name: String) {
        // Note: This creates a placeholder M3U playlist. 
        // In a full implementation, we would ask for URL/Credentials.
        playlistManager.addPlaylist(Playlist(
            id = java.util.UUID.randomUUID().toString(), 
            name = name, 
            type = com.hasanege.materialtv.network.SessionManager.LoginType.M3U,
            url = ""
        ))
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
