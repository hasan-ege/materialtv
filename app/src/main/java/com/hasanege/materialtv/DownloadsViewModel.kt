package com.hasanege.materialtv

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hasanege.materialtv.data.DownloadEntity
import com.hasanege.materialtv.data.DownloadStatus
import com.hasanege.materialtv.data.GroupedDownloads
import com.hasanege.materialtv.data.EpisodeGroupingHelper
import com.hasanege.materialtv.download.DownloadManager
import com.hasanege.materialtv.download.DownloadManagerImpl
import com.hasanege.materialtv.download.SystemDownloadManager
import com.hasanege.materialtv.download.SystemDownload
import com.hasanege.materialtv.model.ContinueWatchingItem
import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class DownloadsViewModel() : ViewModel() {

    private val _downloads = MutableStateFlow<List<DownloadEntity>>(emptyList())
    val downloads: StateFlow<List<DownloadEntity>> = _downloads.asStateFlow()
    
    private val _systemDownloads = MutableStateFlow<List<SystemDownload>>(emptyList())
    val systemDownloads: StateFlow<List<SystemDownload>> = _systemDownloads.asStateFlow()
    
    val groupedDownloads: StateFlow<GroupedDownloads> = _downloads.map { downloads ->
        EpisodeGroupingHelper.groupDownloads(downloads)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GroupedDownloads(emptyList(), emptyList())
    )

    private var downloadManager: DownloadManager? = null
    private var systemDownloadManager: SystemDownloadManager? = null

    fun initialize(context: Context) {
        val manager = DownloadManagerImpl.getInstance(context)
        downloadManager = manager
        val systemManager = SystemDownloadManager.getInstance(context)
        systemDownloadManager = systemManager
        
        // Bridge manager's downloads flow into our own state
        viewModelScope.launch {
            manager.downloads.collect { managerDownloads ->
                // Scan local files for completed downloads
                val localFiles = com.hasanege.materialtv.download.LocalFileScanner.scanDownloadedFiles(context)
                
                // Create a map of local files by title for quick lookup
                // Normalize titles to improve matching (remove special chars, lowercase)
                val localFileMap = localFiles.associateBy { it.title.lowercase().replace(Regex("[^a-z0-9]"), "") }
                
                // Merge logic:
                // 1. Active downloads (DOWNLOADING, QUEUED, PAUSED) - keep as is with progress
                // 2. Completed downloads - use local file if found (has real filePath)
                val merged = managerDownloads.map { download ->
                    val normalizedTitle = download.title.lowercase().replace(Regex("[^a-z0-9]"), "")
                    val localMatch = localFileMap[normalizedTitle]
                    
                    if (download.status == DownloadStatus.COMPLETED) {
                        // For completed downloads, check if we have the local file
                        if (localMatch != null) {
                             // Use local file entity but keep original ID if possible or merge info
                             localMatch.copy(
                                 id = download.id, // Keep manager ID
                                 url = download.url,
                                 thumbnailUrl = download.thumbnailUrl
                             )
                        } else {
                             download
                        }
                    } else {
                        // For active downloads, keep the manager's version (has progress)
                        download
                    }
                }.toMutableList()
                
                // Add any local files that aren't in the manager
                localFiles.forEach { localFile ->
                    val normalizedLocalTitle = localFile.title.lowercase().replace(Regex("[^a-z0-9]"), "")
                    val existsInManager = managerDownloads.any { 
                        it.title.lowercase().replace(Regex("[^a-z0-9]"), "") == normalizedLocalTitle 
                    }
                    
                    if (!existsInManager) {
                        merged.add(localFile)
                    }
                }
                
                _downloads.value = merged
            }
        }
        
        // Bridge system downloads flow
        viewModelScope.launch {
            systemManager.systemDownloads.collect { list ->
                _systemDownloads.value = list
            }
        }
    }

    fun startDownload(url: String, title: String, thumbnailUrl: String = "") {
        viewModelScope.launch {
            val downloadManager = downloadManager ?: return@launch
            val filePath = "/storage/emulated/0/Download/${title.replace(Regex("[^a-zA-Z0-9]"), "_")}.mp4"
            downloadManager.startDownload(url, title, filePath, thumbnailUrl)
        }
    }

    fun deleteDownload(download: DownloadEntity) {
        viewModelScope.launch {
            downloadManager?.deleteDownload(download.id)
        }
    }

    fun retryDownload(download: DownloadEntity) {
        viewModelScope.launch {
            downloadManager?.retryDownload(download.id)
        }
    }

    fun pauseDownload(download: DownloadEntity) {
        viewModelScope.launch {
            downloadManager?.pauseDownload(download.id)
        }
    }

    fun resumeDownload(download: DownloadEntity) {
        viewModelScope.launch {
            downloadManager?.resumeDownload(download.id)
        }
    }

    fun cancelDownload(download: DownloadEntity) {
        viewModelScope.launch {
            downloadManager?.cancelDownload(download.id)
        }
    }

    fun reconnectDownload(download: DownloadEntity) {
        viewModelScope.launch {
            val manager = downloadManager ?: return@launch
            // Kısa bir pause + resume döngüsü ile bağlantıyı tazele
            manager.pauseDownload(download.id)
            delay(500L)
            manager.resumeDownload(download.id)
        }
    }
    
    fun cancelSystemDownload(systemDownload: SystemDownload) {
        viewModelScope.launch {
            systemDownloadManager?.cancelDownload(systemDownload.id)
        }
    }
    
    fun refreshSystemDownloads() {
        viewModelScope.launch {
            systemDownloadManager?.refreshDownloads()
        }
    }
    
    fun playDownloadedFile(context: Context, download: DownloadEntity) {
        // Extract series information from title if it's a series episode
        val episodeInfo = EpisodeGroupingHelper.extractEpisodeInfo(download.title)
        
        // Check if file exists locally
        // Handle potential file:// prefix
        val cleanPath = if (download.filePath.startsWith("file://")) {
            download.filePath.substring(7)
        } else {
            download.filePath
        }
        
        val file = java.io.File(cleanPath)
        val fileExists = cleanPath.isNotEmpty() && 
                        !cleanPath.equals("/", ignoreCase = true) &&
                        file.exists() && file.length() > 0
        
        // Create a ContinueWatchingItem for the downloaded file
        val watchItem = ContinueWatchingItem(
            streamId = "downloaded_${download.id}".hashCode(),
            name = download.title,
            streamIcon = if (fileExists) cleanPath else download.url,
            duration = 0L,
            position = 0L,
            type = if (episodeInfo != null) "series" else "downloaded",
            seriesId = episodeInfo?.seriesName?.hashCode(),
            episodeId = download.url
        )
        
        // Add to watch history
        WatchHistoryManager.saveItem(watchItem)
        
        // Start player activity
        val intent = Intent(context, PlayerActivity::class.java).apply {
            if (fileExists) {
                // Play from local file
                putExtra("URI", cleanPath)
                putExtra("IS_DOWNLOADED_FILE", true)
                android.util.Log.d("DownloadsViewModel", "Playing local file: $cleanPath")
            } else {
                // File not found or download incomplete, stream from original URL
                putExtra("url", download.url)
                putExtra("IS_DOWNLOADED_FILE", false)
                android.util.Log.d("DownloadsViewModel", "Local file not found ($cleanPath), streaming from: ${download.url}")
            }
            putExtra("TITLE", download.title)
            putExtra("ORIGINAL_URL", download.url)
            if (episodeInfo != null) {
                putExtra("SERIES_ID", episodeInfo.seriesName.hashCode())
            }
        }
        context.startActivity(intent)
    }
    
    fun playSystemDownloadedFile(context: Context, systemDownload: SystemDownload) {
        // Extract series information from title if it's a series episode
        val episodeInfo = EpisodeGroupingHelper.extractEpisodeInfo(systemDownload.title)
        
        // Use contentUri if available (handles permissions correctly), otherwise fallback to local path
        val filePath = if (systemDownload.contentUri.isNotEmpty()) {
            systemDownload.contentUri
        } else if (systemDownload.localPath.startsWith("file://")) {
            systemDownload.localPath.substring(7)
        } else {
            systemDownload.localPath
        }
        
        // Create a ContinueWatchingItem for the downloaded file
        val watchItem = ContinueWatchingItem(
            streamId = "system_download_${systemDownload.id}".hashCode(),
            name = systemDownload.title,
            streamIcon = filePath, // Store file path for playback
            duration = 0L,
            position = 0L,
            type = if (episodeInfo != null) "series" else "downloaded",
            seriesId = episodeInfo?.seriesName?.hashCode(), // Use series name hash as seriesId
            episodeId = systemDownload.url // Store original URL in episodeId field
        )
        
        // Add to watch history
        WatchHistoryManager.saveItem(watchItem)
        
        // Start player activity
        val intent = Intent(context, PlayerActivity::class.java).apply {
            putExtra("URI", filePath)
            putExtra("TITLE", systemDownload.title)
            putExtra("ORIGINAL_URL", systemDownload.url)
            putExtra("IS_DOWNLOADED_FILE", true)
            if (episodeInfo != null) {
                putExtra("SERIES_ID", episodeInfo.seriesName.hashCode())
            }
        }
        context.startActivity(intent)
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
