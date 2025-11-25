package com.hasanege.materialtv

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hasanege.materialtv.data.AppDatabase
import com.hasanege.materialtv.data.DownloadEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DownloadsViewModel(application: Application) : AndroidViewModel(application) {
    
    private val downloadDao = AppDatabase.getDatabase(application).downloadDao()
    
    val downloads: StateFlow<List<DownloadEntity>> = downloadDao.getAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    var isLoading by mutableStateOf(false)
        private set
    
    fun deleteDownload(downloadId: java.util.UUID) {
        viewModelScope.launch {
            downloadDao.deleteById(downloadId)
        }
    }
    
    fun retryDownload(download: DownloadEntity) {
        viewModelScope.launch {
            // TODO: Implement retry logic
            val updatedDownload = download.copy(status = "QUEUED", progress = 0)
            downloadDao.update(updatedDownload)
        }
    }
}
