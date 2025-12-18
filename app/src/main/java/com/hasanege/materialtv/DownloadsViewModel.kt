package com.hasanege.materialtv

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hasanege.materialtv.download.DownloadItem
import com.hasanege.materialtv.download.DownloadManagerImpl
import com.hasanege.materialtv.download.DownloadStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * DownloadsViewModel - Yeni indirme sistemi
 */
class DownloadsViewModel : ViewModel() {
    
    private var downloadManager: DownloadManagerImpl? = null
    
    // Raw downloads from database (unfiltered)
    private val _rawDownloads = MutableStateFlow<List<DownloadItem>>(emptyList())
    
    // Filtered downloads for UI
    private val _downloads = MutableStateFlow<List<DownloadItem>>(emptyList())
    val downloads: StateFlow<List<DownloadItem>> = _downloads.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _selectedFilter = MutableStateFlow(DownloadFilter.ALL)
    val selectedFilter: StateFlow<DownloadFilter> = _selectedFilter.asStateFlow()
    
    fun initialize(context: Context) {
        downloadManager = DownloadManagerImpl.getInstance(context)
        
        // Mevcut indirmeleri tara
        downloadManager?.scanExistingDownloads()
        
        viewModelScope.launch {
            downloadManager?.downloads?.collect { items ->
                _rawDownloads.value = items
                _downloads.value = filterDownloads(items, _selectedFilter.value)
            }
        }
    }
    
    fun setFilter(filter: DownloadFilter) {
        _selectedFilter.value = filter
        // Always filter from raw data, not from already filtered data
        _downloads.value = filterDownloads(_rawDownloads.value, filter)
    }
    
    private fun filterDownloads(items: List<DownloadItem>, filter: DownloadFilter): List<DownloadItem> {
        // First, exclude CANCELLED and FAILED downloads from the list
        val activeItems = items.filter { 
            it.status != DownloadStatus.CANCELLED && it.status != DownloadStatus.FAILED 
        }
        
        return when (filter) {
            DownloadFilter.ALL -> activeItems
            DownloadFilter.DOWNLOADING -> activeItems.filter { 
                it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.PENDING 
            }
            DownloadFilter.COMPLETED -> activeItems.filter { it.status == DownloadStatus.COMPLETED }
            DownloadFilter.PAUSED -> activeItems.filter { it.status == DownloadStatus.PAUSED }
        }
    }
    
    fun pauseDownload(id: String) {
        downloadManager?.pauseDownload(id)
    }
    
    fun resumeDownload(id: String) {
        downloadManager?.resumeDownload(id)
    }
    
    fun cancelDownload(id: String) {
        downloadManager?.cancelDownload(id)
    }
    
    fun deleteDownload(id: String) {
        downloadManager?.deleteDownload(id)
    }
    
    /**
     * Mevcut indirmeleri yeniden tara
     */
    fun rescanDownloads() {
        downloadManager?.scanExistingDownloads()
    }
    
    fun playDownload(context: Context, download: DownloadItem) {
        val file = File(download.filePath)
        if (file.exists()) {
            val intent = Intent(context, PlayerActivity::class.java).apply {
                // Use URI for local file playback (PlayerActivity expects this)
                putExtra("URI", download.filePath)
                putExtra("TITLE", download.title)
                putExtra("IS_DOWNLOADED_FILE", true)
            }
            context.startActivity(intent)
        } else {
            android.widget.Toast.makeText(context, "Dosya bulunamadı", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}

enum class DownloadFilter {
    ALL, DOWNLOADING, COMPLETED, PAUSED
}

object DownloadsViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return DownloadsViewModel() as T
    }
}
