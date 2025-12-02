package com.hasanege.materialtv.download

import android.app.DownloadManager as AndroidDownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import com.hasanege.materialtv.data.DownloadAlgorithm
import com.hasanege.materialtv.data.DownloadEntity
import com.hasanege.materialtv.data.DownloadStatus
import com.hasanege.materialtv.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.buffer
import okio.sink
import okio.source
import okio.appendingSink
import okio.HashingSink
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile

import kotlinx.coroutines.flow.collectLatest

data class DownloadProgress(
    val progress: Int = 0,
    val downloadSpeed: Long = 0L,
    val fileSize: Long = 0L,
    val downloadedBytes: Long = 0L
)

interface DownloadManager {
    val downloads: Flow<List<DownloadEntity>>
    val downloadProgress: Flow<Map<String, DownloadProgress>>

    suspend fun startDownload(url: String, title: String, filePath: String, thumbnailUrl: String = ""): DownloadEntity
    suspend fun pauseDownload(downloadId: String)
    suspend fun resumeDownload(downloadId: String)
    suspend fun cancelDownload(downloadId: String)
    suspend fun deleteDownload(downloadId: String)
    suspend fun retryDownload(downloadId: String)
    suspend fun downloadThumbnail(url: String, filePath: String): String
}

class DownloadManagerImpl(private val context: Context) : DownloadManager {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()
    
    private val androidDownloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as AndroidDownloadManager

    // Thread-safe collections
    private val downloadsMap = java.util.concurrent.ConcurrentHashMap<String, DownloadEntity>()
    private val jobs = java.util.concurrent.ConcurrentHashMap<String, Job>()
    
    private val _downloadsState = MutableStateFlow<List<DownloadEntity>>(emptyList())
    private val _downloadProgress = MutableStateFlow<Map<String, DownloadProgress>>(emptyMap())
    
    override val downloads: Flow<List<DownloadEntity>> = _downloadsState.asStateFlow()
    override val downloadProgress: Flow<Map<String, DownloadProgress>> = _downloadProgress.asStateFlow()
    
    // Kuyruk sistemi için değişkenler - Thread safe list needed or synchronized access
    private val downloadQueue = java.util.Collections.synchronizedList(mutableListOf<String>())

    init {
        // System Download Manager Synchronization Loop
        scope.launch {
            while (true) {
                syncSystemDownloads()
                kotlinx.coroutines.delay(1000) // Poll every second
            }
        }

        // Auto retry failed downloads
        scope.launch {
            com.hasanege.materialtv.data.SettingsRepository.getInstance(context).autoRetryFailedDownloads.collectLatest { enabled ->
                if (enabled) {
                    while (true) {
                        kotlinx.coroutines.delay(30000) // 30 saniyede bir kontrol et
                        val failedDownloads = downloadsMap.values.filter { it.status == DownloadStatus.FAILED }
                        if (failedDownloads.isNotEmpty()) {
                            failedDownloads.forEach { failed ->
                                try {
                                    retryDownload(failed.id)
                                    android.util.Log.d("DownloadManager", "Auto retrying failed download: ${failed.title}")
                                } catch (e: Exception) {
                                    android.util.Log.e("DownloadManager", "Auto retry failed for ${failed.title}: ${e.message}")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun syncSystemDownloads() {
        android.util.Log.v("DownloadManager", "syncSystemDownloads called")
        // Query ALL downloads to find ones that belong to us but might be missing from our map
        // or to update existing ones.
        val query = AndroidDownloadManager.Query()
        
        try {
            val cursor = androidDownloadManager.query(query)
            val systemDownloadsFound = mutableSetOf<Long>()

            while (cursor.moveToNext()) {
                val idColumn = cursor.getColumnIndex(AndroidDownloadManager.COLUMN_ID)
                val titleColumn = cursor.getColumnIndex(AndroidDownloadManager.COLUMN_TITLE)
                val descColumn = cursor.getColumnIndex(AndroidDownloadManager.COLUMN_DESCRIPTION)
                val uriColumn = cursor.getColumnIndex(AndroidDownloadManager.COLUMN_URI)
                val localUriColumn = cursor.getColumnIndex(AndroidDownloadManager.COLUMN_LOCAL_URI)
                val statusColumn = cursor.getColumnIndex(AndroidDownloadManager.COLUMN_STATUS)
                val totalSizeColumn = cursor.getColumnIndex(AndroidDownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                val downloadedColumn = cursor.getColumnIndex(AndroidDownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)

                if (idColumn != -1) {
                    val id = cursor.getLong(idColumn)
                    systemDownloadsFound.add(id)
                    
                    val title = if (titleColumn != -1) cursor.getString(titleColumn) else ""
                    val description = if (descColumn != -1) cursor.getString(descColumn) else ""
                    
                    // Filter: Only process downloads that look like ours
                    val isOurs = description.startsWith("Downloading ") || title.isNotEmpty()
                    
                    var entity = downloadsMap.values.find { it.systemDownloadId == id }
                    android.util.Log.d("DownloadManager", "Found system download: id=$id, title=$title, entity=${entity!=null}")
                    
                    if (entity == null && isOurs) {
                        // New/Lost download found! Add it.
                        val downloadId = System.currentTimeMillis().toString() + "_" + id
                        val url = if (uriColumn != -1) cursor.getString(uriColumn) else ""
                        val localUri = if (localUriColumn != -1) cursor.getString(localUriColumn) else ""
                        // Fix VLC Error: Decode the path properly
                        val filePath = if (localUri != null) {
                            val path = Uri.parse(localUri).path
                            if (path != null) path else ""
                        } else ""
                        
                        entity = DownloadEntity(
                            id = downloadId,
                            title = title,
                            url = url,
                            filePath = filePath,
                            systemDownloadId = id,
                            status = DownloadStatus.QUEUED
                        )
                        downloadsMap[downloadId] = entity
                    }

                    if (entity != null) {
                        val status = cursor.getInt(statusColumn)
                        val totalSize = if (totalSizeColumn != -1) cursor.getLong(totalSizeColumn) else 0L
                        val downloaded = if (downloadedColumn != -1) cursor.getLong(downloadedColumn) else 0L
                        
                        val newStatus = when (status) {
                            AndroidDownloadManager.STATUS_RUNNING -> DownloadStatus.DOWNLOADING
                            AndroidDownloadManager.STATUS_PAUSED -> DownloadStatus.PAUSED
                            AndroidDownloadManager.STATUS_PENDING -> DownloadStatus.QUEUED
                            AndroidDownloadManager.STATUS_SUCCESSFUL -> DownloadStatus.COMPLETED
                            AndroidDownloadManager.STATUS_FAILED -> DownloadStatus.FAILED
                            else -> entity.status
                        }

                        if (newStatus != entity.status || downloaded != entity.downloadedBytes) {
                             updateProgress(entity.id, downloaded, totalSize, 0L, newStatus)
                             
                             if (newStatus == DownloadStatus.COMPLETED) {
                                 val uriString = if (localUriColumn != -1) cursor.getString(localUriColumn) else null
                                 var pathToSave = entity.filePath // Use existing path as fallback
                                 
                                 if (uriString != null && uriString.isNotEmpty()) {
                                     try {
                                         val parsedPath = Uri.parse(uriString).path
                                         if (parsedPath != null && parsedPath.isNotEmpty()) {
                                             pathToSave = parsedPath
                                         }
                                     } catch (e: Exception) {
                                         android.util.Log.e("DownloadManager", "Failed to parse URI: $uriString", e)
                                     }
                                 }
                                 
                                 // ALWAYS update filePath on completion, even if it's the same
                                 downloadsMap[entity.id] = downloadsMap[entity.id]!!.copy(
                                     filePath = pathToSave,
                                     status = DownloadStatus.COMPLETED,
                                     progress = 100,
                                     downloadedBytes = totalSize,
                                     fileSize = totalSize
                                 )
                                 android.util.Log.d("DownloadManager", "System download completed: ${entity.title}, filePath=$pathToSave")
                             }
                        }
                    }
                }
            }
            cursor.close()
        } catch (e: Exception) {
            android.util.Log.e("DownloadManager", "Error syncing system downloads: ${e.message}")
        }
    }

    override suspend fun startDownload(url: String, title: String, filePath: String, thumbnailUrl: String): DownloadEntity {
        val settings = SettingsRepository.getInstance(context)
        val algorithm = settings.downloadAlgorithm.first()

        val downloadId = System.currentTimeMillis().toString()
        var systemId = -1L
        var sanitizedPath = filePath

        // Ensure directory exists
        val file = File(filePath)
        if (file.parentFile?.exists() == false) {
            file.parentFile?.mkdirs()
        }

        if (algorithm == DownloadAlgorithm.SYSTEM_DOWNLOAD_MANAGER) {
            val request = AndroidDownloadManager.Request(Uri.parse(url))
                .setTitle(title)
                .setDescription("Downloading $title")
                .setNotificationVisibility(AndroidDownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)
            
            // System Download Manager requires a subpath within standard directories
            // We need to extract the relative path from the absolute filePath
            // Expected filePath: /storage/emulated/0/Download/MaterialTV/Movies/Title.mp4
            // Relative path: MaterialTV/Movies/Title.mp4
            
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val relativePath = if (filePath.startsWith(downloadDir.absolutePath)) {
                filePath.substring(downloadDir.absolutePath.length + 1)
            } else {
                "MaterialTV/${file.name}"
            }

            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, relativePath)
            
            // Update sanitizedPath to what the system will likely use
            sanitizedPath = File(downloadDir, relativePath).absolutePath
            
            try {
                systemId = androidDownloadManager.enqueue(request)
            } catch (e: Exception) {
                android.util.Log.e("DownloadManager", "Failed to enqueue system download: ${e.message}")
                // Fallback or error handling? For now, we'll let it fail but log it.
            }
        } else {
            sanitizedPath = ensureDownloadPath(filePath)
        }

        val entity = DownloadEntity(
            id = downloadId,
            title = title,
            url = url,
            filePath = sanitizedPath,
            thumbnailUrl = thumbnailUrl,
            status = DownloadStatus.QUEUED,
            progress = 0,
            downloadedBytes = 0L,
            fileSize = 0L,
            downloadSpeed = 0L,
            systemDownloadId = systemId
        )
        downloadsMap[downloadId] = entity
        
        if (algorithm == DownloadAlgorithm.OKHTTP) {
            synchronized(downloadQueue) {
                downloadQueue.add(downloadId)
            }
            processQueue()
        }
        
        emitDownloadsState()
        return entity
    }

    override suspend fun downloadThumbnail(url: String, filePath: String): String {
        return try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IllegalStateException("Thumbnail download failed: ${response.code}")
                val body = response.body ?: throw IllegalStateException("Empty response body")
                
                val file = File(filePath)
                file.parentFile?.mkdirs()
                
                file.sink().buffer().use { sink ->
                    body.source().use { source ->
                        sink.writeAll(source)
                    }
                }
                file.absolutePath
            }
        } catch (e: Exception) {
            android.util.Log.e("DownloadManager", "Error downloading thumbnail: ${e.message}")
            "" // Hata durumunda boş string döndür
        }
    }

    override suspend fun pauseDownload(downloadId: String) {
        val entity = downloadsMap[downloadId] ?: return
        
        if (entity.systemDownloadId != -1L) {
            androidDownloadManager.remove(entity.systemDownloadId)
            downloadsMap[downloadId] = entity.copy(status = DownloadStatus.PAUSED, systemDownloadId = -1L)
            emitDownloadsState()
        } else {
            jobs[downloadId]?.cancel()
            jobs.remove(downloadId)
            downloadsMap[downloadId]?.let {
                downloadsMap[downloadId] = it.copy(
                    status = DownloadStatus.PAUSED,
                    downloadSpeed = 0L
                )
            }
            synchronized(downloadQueue) {
                if (!downloadQueue.contains(downloadId)) {
                    downloadQueue.add(0, downloadId)
                }
            }
            processQueue()
        }
        emitDownloadsState()
    }

    override suspend fun resumeDownload(downloadId: String) {
        val entity = downloadsMap[downloadId] ?: return
        
        if (entity.systemDownloadId == -1L && entity.status == DownloadStatus.PAUSED && SettingsRepository.getInstance(context).downloadAlgorithm.first() == DownloadAlgorithm.SYSTEM_DOWNLOAD_MANAGER) {
             val request = AndroidDownloadManager.Request(Uri.parse(entity.url))
                .setTitle(entity.title)
                .setDescription("Downloading ${entity.title}")
                .setNotificationVisibility(AndroidDownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)
            
             val fileName = File(entity.filePath).name
             request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "MaterialTV/$fileName")
             
             val newSystemId = androidDownloadManager.enqueue(request)
             downloadsMap[downloadId] = entity.copy(status = DownloadStatus.QUEUED, systemDownloadId = newSystemId)
             emitDownloadsState()
             return
        }

        if (jobs[downloadId]?.isActive == true) return
        
        if (entity.systemDownloadId == -1L) {
            synchronized(downloadQueue) {
                if (!downloadQueue.contains(downloadId)) {
                    downloadQueue.add(0, downloadId)
                }
            }
            
            downloadsMap[downloadId] = entity.copy(status = DownloadStatus.QUEUED)
            emitDownloadsState()
            processQueue()
        }
    }

    override suspend fun cancelDownload(downloadId: String) {
        val entity = downloadsMap[downloadId]
        
        if (entity?.systemDownloadId != -1L) {
            androidDownloadManager.remove(entity!!.systemDownloadId)
        } else {
            jobs[downloadId]?.cancel()
            jobs.remove(downloadId)
        }

        downloadsMap[downloadId]?.let {
            downloadsMap[downloadId] = it.copy(status = DownloadStatus.CANCELLED)
            File(it.filePath).delete()
        }
        _downloadProgress.update { it - downloadId }
        synchronized(downloadQueue) {
            downloadQueue.remove(downloadId)
        }
        emitDownloadsState()
        processQueue()
    }

    override suspend fun deleteDownload(downloadId: String) {
        cancelDownload(downloadId)
        downloadsMap.remove(downloadId)
        emitDownloadsState()
    }

    override suspend fun retryDownload(downloadId: String) {
        val entity = downloadsMap[downloadId] ?: return
        
        if (entity.systemDownloadId != -1L) {
             androidDownloadManager.remove(entity.systemDownloadId)
             val request = AndroidDownloadManager.Request(Uri.parse(entity.url))
                .setTitle(entity.title)
                .setDescription("Downloading ${entity.title}")
                .setNotificationVisibility(AndroidDownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            
             val fileName = File(entity.filePath).name
             request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "MaterialTV/$fileName")
             
             val newSystemId = androidDownloadManager.enqueue(request)
             downloadsMap[downloadId] = entity.copy(status = DownloadStatus.QUEUED, systemDownloadId = newSystemId, progress = 0, downloadedBytes = 0)
             emitDownloadsState()
             return
        }

        jobs[downloadId]?.cancelAndJoin()
        File(entity.filePath).delete()
        
        synchronized(downloadQueue) {
            if (!downloadQueue.contains(downloadId)) {
                downloadQueue.add(downloadId)
            }
        }
        
        downloadsMap[downloadId] = entity.copy(
            status = DownloadStatus.QUEUED,
            progress = 0,
            downloadedBytes = 0,
            fileSize = 0,
            downloadSpeed = 0
        )
        emitDownloadsState()
        processQueue()
    }

    private fun handleDownloadError(downloadId: String, e: Exception) {
        if (e is CancellationException || 
            e is IOException && (
                e.message == "Canceled" || 
                e.message?.contains("Socket closed") == true ||
                e.message?.contains("unexpected end of stream") == true ||
                e.message?.contains("Software caused connection abort") == true
            ) ||
            e is java.net.ProtocolException && e.message?.contains("unexpected end of stream") == true ||
            e is java.net.SocketException && e.message?.contains("Software caused connection abort") == true
        ) {
            return
        }
        
        android.util.Log.e("DownloadManager", "Download error: ${e.message}", e)
        
        val entity = downloadsMap[downloadId] ?: return
        
        scope.launch {
            val autoRetryEnabled = SettingsRepository.getInstance(context).autoRetryFailedDownloads.first()
            
            if (autoRetryEnabled && entity.retryCount < 3) {
                android.util.Log.d("DownloadManager", "Auto retrying download ($downloadId) attempt ${entity.retryCount + 1}/3")
                
                downloadsMap[downloadId] = entity.copy(
                    status = DownloadStatus.QUEUED,
                    retryCount = entity.retryCount + 1
                )
                jobs.remove(downloadId)
                emitDownloadsState()
                
                // Add back to queue at the front
                synchronized(downloadQueue) {
                    if (!downloadQueue.contains(downloadId)) {
                        downloadQueue.add(0, downloadId)
                    }
                }
                
                kotlinx.coroutines.delay(2000 * (entity.retryCount + 1).toLong()) // Backoff: 2s, 4s, 6s
                processQueue()
            } else {
                downloadsMap[downloadId] = entity.copy(status = DownloadStatus.FAILED)
                jobs.remove(downloadId)
                emitDownloadsState()
                processQueue() // Start next if any
            }
        }
    }

    private val notificationManager = DownloadNotificationManager(context)

    private fun startOkHttpDownload(downloadId: String, url: String, filePath: String, resume: Boolean = false) {
        val job = scope.launch(Dispatchers.IO) {
            var response: okhttp3.Response? = null
            var sink: okio.BufferedSink? = null
            var source: okio.BufferedSource? = null
            
            try {
                val file = File(filePath)
                val downloadedBytes = if (resume && file.exists()) file.length() else 0L
                
                val requestBuilder = Request.Builder().url(url)
                if (resume && downloadedBytes > 0) {
                    requestBuilder.header("Range", "bytes=$downloadedBytes-")
                }
                
                val request = requestBuilder.build()
                response = client.newCall(request).execute()
                
                if (!response.isSuccessful) {
                    throw IOException("Unexpected code $response")
                }
                
                val body = response.body ?: throw IOException("Empty body")
                val contentLength = body.contentLength()
                val totalSize = if (contentLength != -1L) contentLength + downloadedBytes else -1L
                
                // Prepare file sink
                val fileSink = if (resume) {
                    // HashingSink is a class, so we can use it if imported
                    HashingSink.sha256(file.appendingSink()) 
                    file.appendingSink()
                } else {
                    file.sink()
                }
                sink = fileSink.buffer()
                source = body.source()
                
                val buffer = okio.Buffer()
                var totalRead = downloadedBytes
                var lastUpdate = System.currentTimeMillis()
                var bytesSinceLastUpdate = 0L
                
                while (currentCoroutineContext()[Job]?.isActive == true) {
                    val read = source.read(buffer, DEFAULT_BUFFER_SIZE.toLong())
                    if (read == -1L) break
                    
                    sink.write(buffer, read)
                    sink.emit() // Flush frequently to ensure data is written
                    
                    totalRead += read
                    bytesSinceLastUpdate += read
                    
                    val now = System.currentTimeMillis()
                    if (now - lastUpdate >= 1000) {
                        val speed = (bytesSinceLastUpdate * 1000) / (now - lastUpdate)
                        updateProgress(downloadId, totalRead, totalSize, speed, DownloadStatus.DOWNLOADING)
                        lastUpdate = now
                        bytesSinceLastUpdate = 0
                    }
                }
                
                sink.flush()
                sink.close()
                sink = null
                
                if (totalSize != -1L && totalRead < totalSize) {
                     // Incomplete download
                     throw IOException("Download incomplete: $totalRead / $totalSize")
                }
                
                updateProgress(downloadId, totalRead, totalSize, 0, DownloadStatus.COMPLETED)
                
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    handleDownloadError(downloadId, e)
                }
            } finally {
                try {
                    sink?.close()
                    source?.close()
                    response?.close()
                } catch (e: Exception) {
                    android.util.Log.e("DownloadManager", "Error closing resources: ${e.message}")
                }
            }
        }
        jobs[downloadId] = job
    }

    private suspend fun processQueue() {
        val maxConcurrent = SettingsRepository.getInstance(context).maxConcurrentDownloads.first()
        val activeCount = jobs.size
        
        if (activeCount < maxConcurrent) {
            val toStart = mutableListOf<String>()
            
            synchronized(downloadQueue) {
                if (downloadQueue.isNotEmpty()) {
                    // Calculate how many slots are available
                    val slotsAvailable = maxConcurrent - activeCount
                    
                    // Take up to slotsAvailable items from the queue
                    // We need to manually iterate and remove to be safe
                    val iterator = downloadQueue.iterator()
                    var count = 0
                    while (iterator.hasNext() && count < slotsAvailable) {
                        toStart.add(iterator.next())
                        iterator.remove()
                        count++
                    }
                }
            }
            
            toStart.forEach { nextDownloadId ->
                val entity = downloadsMap[nextDownloadId]
                if (entity != null && entity.status == DownloadStatus.QUEUED) {
                    val shouldResume = entity.downloadedBytes > 0
                    startOkHttpDownload(nextDownloadId, entity.url, entity.filePath, resume = shouldResume)
                    downloadsMap[nextDownloadId] = entity.copy(status = DownloadStatus.DOWNLOADING)
                    emitDownloadsState()
                } else {
                    // Invalid entity or status, already removed from queue
                }
            }
        }
    }

    private fun updateProgress(downloadId: String, downloadedBytes: Long, totalBytes: Long, speed: Long, status: DownloadStatus? = null) {
        val progressPercent = if (totalBytes > 0) ((downloadedBytes * 100) / totalBytes).toInt() else 0
        
        downloadsMap[downloadId]?.let { current ->
            if (status == null && (current.status == DownloadStatus.PAUSED ||
                current.status == DownloadStatus.CANCELLED ||
                current.status == DownloadStatus.COMPLETED)
            ) {
                return
            }

            val newStatus = status ?: if (progressPercent >= 100) DownloadStatus.COMPLETED else DownloadStatus.DOWNLOADING

            downloadsMap[downloadId] = current.copy(
                progress = progressPercent.coerceAtMost(100),
                downloadSpeed = speed,
                fileSize = totalBytes,
                downloadedBytes = downloadedBytes,
                status = newStatus
            )
            emitDownloadsState()
            
            // Update Notification
            if (current.systemDownloadId == -1L) { // Only for OkHttp downloads
                if (newStatus == DownloadStatus.DOWNLOADING) {
                    notificationManager.showDownloadNotification(current.title, progressPercent)
                } else if (newStatus == DownloadStatus.COMPLETED || newStatus == DownloadStatus.FAILED || newStatus == DownloadStatus.CANCELLED) {
                    notificationManager.hideDownloadNotification(current.title)
                }
            }
        }
        _downloadProgress.update { current ->
            val updated = current.toMutableMap()
            updated[downloadId] = DownloadProgress(
                progress = progressPercent.coerceAtMost(100),
                downloadSpeed = speed,
                fileSize = totalBytes,
                downloadedBytes = downloadedBytes
            )
            updated
        }
    }

    private fun ensureDownloadPath(filePath: String): String {
        val file = File(filePath)
        val parent = file.parentFile
        if (parent != null && !parent.exists()) {
            parent.mkdirs()
        }
        return file.absolutePath
    }

    private fun emitDownloadsState() {
        val list = downloadsMap.values.toList()
        _downloadsState.value = list
        persistDownloads(list)
    }

	private fun persistDownloads(list: List<DownloadEntity>) {
		try {
            // Atomic write: Write to temp file first, then rename
			val file = File(context.filesDir, DOWNLOADS_FILE_NAME)
            val tempFile = File(context.filesDir, "$DOWNLOADS_FILE_NAME.tmp")
            
			val json = Json.encodeToString(ListSerializer(DownloadEntity.serializer()), list)
			tempFile.writeText(json)
            
            if (tempFile.renameTo(file)) {
                // Success
            } else {
                // Fallback if rename fails (e.g. different filesystems, though unlikely in internal storage)
                file.writeText(json)
                tempFile.delete()
            }
		} catch (e: Exception) {
            android.util.Log.e("DownloadManager", "Failed to persist downloads", e)
		}
	}

	internal fun loadPersistedDownloads() {
		try {
			val file = File(context.filesDir, DOWNLOADS_FILE_NAME)
			if (!file.exists()) return

			val json = file.readText()
			if (json.isBlank()) return

			val list = Json.decodeFromString(ListSerializer(DownloadEntity.serializer()), json)
			list.forEach { entity ->
				when (entity.status) {
					DownloadStatus.COMPLETED -> {
						downloadsMap[entity.id] = entity
					}
					DownloadStatus.DOWNLOADING, DownloadStatus.QUEUED -> {
                        if (entity.systemDownloadId != -1L) {
                            downloadsMap[entity.id] = entity
                        } else {
                            try {
                                if (entity.filePath.isNotBlank()) {
                                    File(entity.filePath).takeIf { it.exists() }?.delete()
                                }
                            } catch (_: Exception) {}
                            val fresh = entity.copy(
                                status = DownloadStatus.QUEUED,
                                progress = 0,
                                downloadedBytes = 0,
                                fileSize = 0,
                                downloadSpeed = 0
                            )
                            downloadsMap[fresh.id] = fresh
                            synchronized(downloadQueue) {
                                downloadQueue.add(fresh.id)
                            }
                        }
					}
					DownloadStatus.PAUSED -> {
						downloadsMap[entity.id] = entity.copy(downloadSpeed = 0)
					}
					DownloadStatus.FAILED, DownloadStatus.CANCELLED -> {
						downloadsMap[entity.id] = entity.copy(downloadSpeed = 0)
					}
				}
			}
			emitDownloadsState()
			scope.launch { processQueue() }
		} catch (_: Exception) {
		}
	}

    companion object {
        private const val DEFAULT_BUFFER_SIZE = 64 * 1024
        private const val DOWNLOADS_FILE_NAME = "downloads.json"

        @Volatile
        private var INSTANCE: DownloadManagerImpl? = null

        fun getInstance(context: Context): DownloadManagerImpl {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DownloadManagerImpl(context.applicationContext).also { instance ->
                    INSTANCE = instance
                    instance.loadPersistedDownloads()
                }
            }
        }
    }
}
