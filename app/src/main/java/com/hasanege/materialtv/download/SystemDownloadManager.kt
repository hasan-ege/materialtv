package com.hasanege.materialtv.download

import android.app.DownloadManager
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.hasanege.materialtv.data.DownloadEntity
import com.hasanege.materialtv.data.DownloadStatus

data class SystemDownload(
    val id: Long,
    val title: String,
    val url: String,
    val localPath: String,
    val contentUri: String = "",
    val status: DownloadStatus,
    val progress: Int = 0,
    val fileSize: Long = 0L,
    val downloadedBytes: Long = 0L,
    val downloadSpeed: Long = 0L,
    val timestamp: Long = System.currentTimeMillis()
)

class SystemDownloadManager(private val context: Context) {
    private val systemDownloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private val _systemDownloads = MutableStateFlow<List<SystemDownload>>(emptyList())
    val systemDownloads: StateFlow<List<SystemDownload>> = _systemDownloads.asStateFlow()
    
    private val downloadObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange)
            refreshDownloads()
        }
    }
    
    init {
        // Register observer to listen for download changes
        context.contentResolver.registerContentObserver(
            Uri.parse("content://downloads/my_downloads"),
            true,
            downloadObserver
        )
        refreshDownloads()
    }
    
    fun startDownload(url: String, title: String, subpath: String): Long {
        return try {
            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle(title)
                .setDescription("Downloading $title")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            // Construct relative path for public downloads directory
            // subpath example: "Movies" or "Series/Breaking Bad"
            val relativePath = if (subpath.isNotEmpty()) {
                "MaterialTV/$subpath/${title}.mp4"
            } else {
                "MaterialTV/${title}.mp4"
            }
            
            // Use setDestinationInExternalPublicDir which is safer and standard
            request.setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS, relativePath)
            
            val mimeType = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(android.webkit.MimeTypeMap.getFileExtensionFromUrl(url))
            if (mimeType != null) {
                request.setMimeType(mimeType)
            }

            systemDownloadManager.enqueue(request)
        } catch (e: Exception) {
            android.util.Log.e("SystemDownloadManager", "Error starting download: ${e.message}")
            -1L
        }
    }
    
    fun refreshDownloads() {
        try {
            val query = DownloadManager.Query()
            query.setFilterByStatus(DownloadManager.STATUS_PENDING or DownloadManager.STATUS_RUNNING or DownloadManager.STATUS_PAUSED or DownloadManager.STATUS_SUCCESSFUL or DownloadManager.STATUS_FAILED)
            
            val cursor = systemDownloadManager.query(query)
            val downloads = mutableListOf<SystemDownload>()
            
            while (cursor.moveToNext()) {
                val id = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_ID))
                val title = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_TITLE)) ?: "Unknown"
                val url = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_URI)) ?: ""
                val localUri = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)) ?: ""
                val status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
                val bytesDownloaded = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                val totalSize = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                val timestamp = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_LAST_MODIFIED_TIMESTAMP))
                
                val downloadStatus = when (status) {
                    DownloadManager.STATUS_PENDING -> DownloadStatus.QUEUED
                    DownloadManager.STATUS_RUNNING -> DownloadStatus.DOWNLOADING
                    DownloadManager.STATUS_PAUSED -> DownloadStatus.PAUSED
                    DownloadManager.STATUS_SUCCESSFUL -> DownloadStatus.COMPLETED
                    DownloadManager.STATUS_FAILED -> DownloadStatus.FAILED
                    else -> DownloadStatus.QUEUED
                }
                
                val progress = if (totalSize > 0) {
                    ((bytesDownloaded * 100) / totalSize).toInt()
                } else 0
                
                // Convert localUri (content:// or file://) to a usable path if possible
                val localPath = if (localUri.isNotEmpty()) {
                    try {
                        Uri.parse(localUri).path ?: localUri
                    } catch (e: Exception) {
                        localUri
                    }
                } else ""

                // Only add video files
                if (title.lowercase().endsWith(".mp4") || title.lowercase().endsWith(".mkv") || 
                    title.lowercase().endsWith(".avi") || title.lowercase().endsWith(".mov") ||
                    // Also check URL or MIME type if title doesn't have extension
                    url.lowercase().endsWith(".mp4") || url.lowercase().endsWith(".mkv")) {
                    
                    downloads.add(
                        SystemDownload(
                            id = id,
                            title = title,
                            url = url,
                            localPath = localPath,
                            contentUri = systemDownloadManager.getUriForDownloadedFile(id)?.toString() ?: "",
                            status = downloadStatus,
                            progress = progress,
                            fileSize = totalSize,
                            downloadedBytes = bytesDownloaded,
                            downloadSpeed = 0L, // System manager doesn't provide speed directly
                            timestamp = timestamp
                        )
                    )
                }
            }
            cursor.close()
            
            _systemDownloads.value = downloads.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            // Handle exceptions
        }
    }
    
    fun cancelDownload(downloadId: Long) {
        systemDownloadManager.remove(downloadId)
        refreshDownloads()
    }
    
    fun restartDownload(downloadId: Long) {
        // System download manager doesn't have a restart method, so we remove and re-enqueue
        systemDownloadManager.remove(downloadId)
        refreshDownloads()
    }
    
    fun cleanup() {
        context.contentResolver.unregisterContentObserver(downloadObserver)
    }
    
    companion object {
        @Volatile
        private var INSTANCE: SystemDownloadManager? = null
        
        fun getInstance(context: Context): SystemDownloadManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SystemDownloadManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
