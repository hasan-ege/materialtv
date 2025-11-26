package com.hasanege.materialtv

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hasanege.materialtv.data.DownloadEntity
import com.hasanege.materialtv.data.DownloadManager
import com.hasanege.materialtv.data.DownloadStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DownloadsViewModel() : ViewModel() {
    
    private val _downloads = MutableStateFlow<List<DownloadEntity>>(emptyList())
    val downloads: StateFlow<List<DownloadEntity>> = _downloads.asStateFlow()
    
    private var downloadManager: DownloadManager? = null
    
    fun initialize(context: Context) {
        downloadManager = DownloadManager(context)
        
        // Listen to download progress
        viewModelScope.launch {
            downloadManager?.downloadProgress?.collect { progressMap ->
                val currentDownloads = _downloads.value.toMutableList()
                progressMap.forEach { (downloadId, progressData) ->
                    val index = currentDownloads.indexOfFirst { it.id == downloadId }
                    if (index != -1) {
                        currentDownloads[index] = currentDownloads[index].copy(
                            progress = progressData.progress,
                            downloadSpeed = progressData.downloadSpeed,
                            fileSize = progressData.totalBytes,
                            downloadedBytes = progressData.downloadedBytes,
                            status = if (progressData.progress == 100) DownloadStatus.COMPLETED else DownloadStatus.DOWNLOADING
                        )
                    }
                }
                _downloads.value = currentDownloads
            }
        }
    }
    
    fun startDownload(url: String, title: String) {
        val downloadManager = downloadManager ?: return
        val downloadId = downloadManager.startDownload(url, title)
        
        val downloadEntity = DownloadEntity(
            id = downloadId,
            title = title,
            url = url,
            status = DownloadStatus.DOWNLOADING,
            progress = 0
        )
        
        val currentDownloads = _downloads.value.toMutableList()
        currentDownloads.add(downloadEntity)
        _downloads.value = currentDownloads
    }
    
    fun deleteDownload(download: DownloadEntity) {
        viewModelScope.launch {
            downloadManager?.deleteDownload(download.id)
            val currentDownloads = _downloads.value.toMutableList()
            currentDownloads.removeAll { it.id == download.id }
            _downloads.value = currentDownloads
        }
    }
    
    fun retryDownload(download: DownloadEntity) {
        viewModelScope.launch {
            downloadManager?.startDownload(download.url, download.title)
            val currentDownloads = _downloads.value.toMutableList()
            val index = currentDownloads.indexOfFirst { it.id == download.id }
            if (index != -1) {
                currentDownloads[index] = currentDownloads[index].copy(
                    status = DownloadStatus.DOWNLOADING,
                    progress = 0
                )
                _downloads.value = currentDownloads
            }
        }
    }
    
    fun pauseDownload(download: DownloadEntity) {
        viewModelScope.launch {
            downloadManager?.pauseDownload(download.id)
            val currentDownloads = _downloads.value.toMutableList()
            val index = currentDownloads.indexOfFirst { it.id == download.id }
            if (index != -1) {
                currentDownloads[index] = currentDownloads[index].copy(
                    status = DownloadStatus.PAUSED
                )
                _downloads.value = currentDownloads
            }
        }
    }
    
    fun resumeDownload(download: DownloadEntity) {
        viewModelScope.launch {
            downloadManager?.resumeDownload(download.id)
            val currentDownloads = _downloads.value.toMutableList()
            val index = currentDownloads.indexOfFirst { it.id == download.id }
            if (index != -1) {
                currentDownloads[index] = currentDownloads[index].copy(
                    status = DownloadStatus.DOWNLOADING
                )
                _downloads.value = currentDownloads
            }
        }
    }
    
    fun cancelDownload(download: DownloadEntity) {
        viewModelScope.launch {
            downloadManager?.cancelDownload(download.id)
            val currentDownloads = _downloads.value.toMutableList()
            val index = currentDownloads.indexOfFirst { it.id == download.id }
            if (index != -1) {
                currentDownloads[index] = currentDownloads[index].copy(
                    status = DownloadStatus.CANCELLED
                )
                _downloads.value = currentDownloads
            }
        }
    }
}

object DownloadsViewModelFactory : ViewModelProvider.Factory {
    
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DownloadsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DownloadsViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
