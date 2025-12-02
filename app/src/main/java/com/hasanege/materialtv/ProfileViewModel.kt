
package com.hasanege.materialtv

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hasanege.materialtv.data.Playlist
import com.hasanege.materialtv.data.PlaylistManager
import com.hasanege.materialtv.model.ContinueWatchingItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfileViewModel(private val playlistManager: PlaylistManager) : ViewModel() {

    val watchHistory: StateFlow<List<ContinueWatchingItem>> = WatchHistoryManager.historyFlow
        .stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val continueWatching: StateFlow<List<ContinueWatchingItem>> = WatchHistoryManager.historyFlow
        .map { history ->
            history.filter { item ->
                if (item.dismissedFromContinueWatching) return@filter false
                val progress = if (item.duration > 0) {
                    (item.position.toFloat() / item.duration.toFloat())
                } else {
                    0f
                }
                progress < 0.95f && progress > 0.01f
            }.sortedByDescending { it.isPinned }
        }
        .stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val totalWatchTime: StateFlow<Long> = WatchHistoryManager.historyFlow
        .map { history ->
            history.sumOf { it.actualWatchTime }
        }
        .stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
            initialValue = 0L
        )
    
    val totalItemsWatched: StateFlow<Int> = WatchHistoryManager.historyFlow
        .map { history -> history.size }
        .stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )
    
    val totalMoviesWatched: StateFlow<Int> = WatchHistoryManager.historyFlow
        .map { history -> history.count { it.type == "movie" } }
        .stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )
    
    val totalSeriesWatched: StateFlow<Int> = WatchHistoryManager.historyFlow
        .map { history -> history.count { it.type == "series" } }
        .stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )
    
    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists

    fun loadWatchHistory() {
        // No-op, flows are now reactive
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
