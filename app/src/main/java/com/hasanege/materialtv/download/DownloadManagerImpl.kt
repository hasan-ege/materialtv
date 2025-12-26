package com.hasanege.materialtv.download

import android.content.Context
import com.hasanege.materialtv.model.Episode
import com.hasanege.materialtv.model.VodItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * DownloadManager implementasyonu
 */
class DownloadManagerImpl private constructor(private val context: Context) : DownloadManager {
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val repository = DownloadRepository(context)
    
    companion object {
        @Volatile
        private var INSTANCE: DownloadManagerImpl? = null
        
        fun getInstance(context: Context): DownloadManagerImpl {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DownloadManagerImpl(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    override val downloads: Flow<List<DownloadItem>> = repository.getAllDownloads()
    
    override val activeDownloads: Flow<List<DownloadItem>> = repository.getActiveDownloads()
    
    /**
     * Film indirme başlat
     */
    override fun startDownload(movie: VodItem) {
        scope.launch {
            val url = DownloadManager.getMovieStreamUrl(context, movie.streamId, movie.containerExtension)
            if (url == null) {
                android.util.Log.e("DownloadManager", "Could not generate URL for movie: ${movie.name}")
                return@launch
            }
            
            val filePath = DownloadManager.getMovieFilePath(movie.name ?: "Unknown")
            
            val downloadItem = DownloadItem(
                title = movie.name ?: "Unknown Movie",
                url = url,
                filePath = filePath,
                thumbnailUrl = movie.streamIcon,
                contentType = ContentType.MOVIE,
                status = DownloadStatus.PENDING
            )
            
            repository.insertDownload(downloadItem)
            
            // Servisi başlat
            DownloadService.start(context)
        }
    }
    
    /**
     * Bölüm indirme başlat
     */
    override fun startDownload(episode: Episode, seriesName: String, seasonNumber: Int, episodeNumber: Int, seriesCoverUrl: String?) {
        scope.launch {
            val url = DownloadManager.getEpisodeStreamUrl(context, episode.id, episode.containerExtension)
            if (url == null) {
                android.util.Log.e("DownloadManager", "Could not generate URL for episode: ${episode.title}")
                return@launch
            }
            
            val filePath = DownloadManager.getEpisodeFilePath(seriesName, seasonNumber, episodeNumber, episode.title)
            
            val downloadItem = DownloadItem(
                title = episode.title ?: "Episode $episodeNumber",
                url = url,
                filePath = filePath,
                thumbnailUrl = episode.info?.movieImage,
                seriesCoverUrl = seriesCoverUrl, // Dizi kapak fotoğrafı
                contentType = ContentType.EPISODE,
                seriesName = seriesName,
                seasonNumber = seasonNumber,
                episodeNumber = episodeNumber,
                status = DownloadStatus.PENDING
            )
            
            repository.insertDownload(downloadItem)
            
            // Servisi başlat
            DownloadService.start(context)
        }
    }
    
    /**
     * İndirmeyi duraklat
     */
    override fun pauseDownload(id: String) {
        DownloadService.pause(context, id)
    }
    
    /**
     * İndirmeye devam et
     */
    override fun resumeDownload(id: String) {
        DownloadService.resume(context, id)
    }
    
    /**
     * İndirmeyi iptal et
     */
    override fun cancelDownload(id: String) {
        DownloadService.cancel(context, id)
    }
    
    /**
     * İndirmeyi sil (thumbnail ve boş klasörler dahil)
     */
    override fun deleteDownload(id: String) {
        scope.launch {
            val download = repository.getDownloadById(id)
            if (download != null) {
                cleanupDownloadFiles(download)
            }
            repository.deleteDownload(id)
        }
    }
    
    /**
     * İndirme dosyalarını temizle (sadece ilgili bölümün dosyaları)
     * Klasör temizliği veritabanındaki kayıtlara göre yapılır
     */
    /**
     * İndirme dosyalarını temizle (sadece ilgili bölümün dosyaları)
     * Klasör temizliği veritabanındaki kayıtlara göre yapılır
     */
    private fun cleanupDownloadFiles(download: DownloadItem) {
        try {
            val videoFile = java.io.File(download.filePath)
            val parentDir = videoFile.parentFile // S01 klasörü
            val seriesDir = parentDir?.parentFile // DiziAdi klasörü
            
            // 1. Video dosyasını sil
            if (videoFile.exists()) {
                videoFile.delete()
                android.util.Log.d("DownloadManager", "Deleted video: ${videoFile.name}")
            }
            
            // 2. Sadece bu bölümün thumbnail dosyasını sil
            if (download.contentType == ContentType.EPISODE && download.episodeNumber != null) {
                val thumbnailFile = java.io.File(parentDir, "E${download.episodeNumber}_thumbnail.png")
                if (thumbnailFile.exists()) {
                    thumbnailFile.delete()
                    android.util.Log.d("DownloadManager", "Deleted thumbnail: ${thumbnailFile.name}")
                }
            }
            
            // 3. Veritabanında bu dizi için başka indirme var mı kontrol et
            scope.launch {
                val seriesName = download.seriesName
                if (seriesName != null && parentDir != null) {
                    val remainingDownloads = repository.getAllDownloads()
                        .first()
                        .filter { it.seriesName == seriesName && it.id != download.id }
                    
                    if (remainingDownloads.isEmpty()) {
                        // Bu dizi için başka indirme yok, klasörü temizle
                        android.util.Log.d("DownloadManager", "No more downloads for series: $seriesName, cleaning up folder")
                        
                        // Sezon klasörünü temizle (boş ise)
                        if (parentDir.exists() && parentDir.listFiles()?.isEmpty() == true) {
                            parentDir.delete()
                            android.util.Log.d("DownloadManager", "Deleted season folder: ${parentDir.name}")
                        }
                        
                        // Dizi klasörünü temizle (boş ise)
                        if (seriesDir != null && seriesDir.exists() && seriesDir.listFiles()?.isEmpty() == true) {
                            seriesDir.delete()
                            android.util.Log.d("DownloadManager", "Deleted series folder: ${seriesDir.name}")
                        }
                    } else {
                        // Diğer indirmeler var, ama bu sezon klasörü boşalmış olabilir
                         val remainingSeasonDownloads = remainingDownloads.filter { 
                             it.seasonNumber == download.seasonNumber 
                         }
                         if (remainingSeasonDownloads.isEmpty()) {
                             if (parentDir.exists() && parentDir.listFiles()?.isEmpty() == true) {
                                 parentDir.delete()
                                 android.util.Log.d("DownloadManager", "Deleted empty season folder: ${parentDir.name}")
                             }
                         }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("DownloadManager", "Error cleaning up files: ${e.message}")
        }
    }
    
    /**
     * Mevcut indirmeleri tara ve veritabanına ekle
     */
    fun scanExistingDownloads() {
        scope.launch {
            try {
                // Doğru yol: Downloads/MaterialTV
                val downloadsDir = java.io.File(
                    android.os.Environment.getExternalStoragePublicDirectory(
                        android.os.Environment.DIRECTORY_DOWNLOADS
                    ),
                    "MaterialTV"
                )
                
                if (!downloadsDir.exists()) {
                    android.util.Log.d("DownloadManager", "Downloads directory does not exist")
                    return@launch
                }
                
                // Video uzantıları
                val videoExtensions = listOf("mp4", "mkv", "avi", "webm", "m4v", "ts", "mov")
                
                // Tüm mevcut dosya yollarını al
                val existingPaths = repository.getAllDownloads()
                    .map { list -> list.map { it.filePath }.toSet() }
                    .first()
                
                // Klasörü recursive olarak tara
                scanDirectory(downloadsDir, videoExtensions, existingPaths)
                
                android.util.Log.d("DownloadManager", "Finished scanning existing downloads")
            } catch (e: Exception) {
                android.util.Log.e("DownloadManager", "Error scanning downloads: ${e.message}")
            }
        }
    }
    
    private suspend fun scanDirectory(
        dir: java.io.File,
        videoExtensions: List<String>,
        existingPaths: Set<String>
    ) {
        // Skip hidden folders
        if (dir.isHidden) return

        dir.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                // Alt klasörleri tara
                scanDirectory(file, videoExtensions, existingPaths)
            } else {
                val extension = file.extension.lowercase()
                if (extension in videoExtensions && file.absolutePath !in existingPaths) {
                    // Bu dosya veritabanında yok, ekle
                    addExistingFileToDatabase(file)
                }
            }
        }
    }
    
    private suspend fun addExistingFileToDatabase(file: java.io.File) {
        try {
            val parentDir = file.parentFile ?: return
            val parentName = parentDir.name
            val fileName = file.nameWithoutExtension
            
            // Analyze path to determine content type
            // Formats:
            // 1. MaterialTV/Movies/MovieName.mp4
            // 2. MaterialTV/SeriesName/S01/E01.mp4 (New simple format)
            // 3. MaterialTV/Series/SeriesName/S01/E01.mp4 (Old complex format)
            
            val contentType: ContentType
            var seriesName: String? = null
            var seasonNumber: Int? = null
            var episodeNumber: Int? = null
            var title: String = fileName.replace("_", " ")

            // Check if parent is a Season folder (S01, S1, etc.)
            val seasonMatch = Regex("""^S(\d+)$""", RegexOption.IGNORE_CASE).find(parentName)
            
            if (seasonMatch != null) {
                // It's likely a series episode
                contentType = ContentType.EPISODE
                seasonNumber = seasonMatch.groupValues[1].toIntOrNull()
                
                // Series name is the parent folder of Season folder
                seriesName = parentDir.parentFile?.name
                
                // If grandparent is "Series" (Old format), use great-grandparent? 
                // Wait, if path is MaterialTV/Series/BreakingBad/S01...
                // parent=S01, parent.parent=BreakingBad.
                // If path is MaterialTV/BreakingBad/S01...
                // parent=S01, parent.parent=BreakingBad.
                // So parsing logic is actually the same for both! 
                // The only difference is where 'Series' folder is, but we just take parent of S01 as series name.
                
                // Detect Episode Number from filename (E01 or S01E01)
                val episodePattern = Regex("""E(\d+)""", RegexOption.IGNORE_CASE)
                val episodeMatch = episodePattern.find(fileName)
                episodeNumber = episodeMatch?.groupValues?.get(1)?.toIntOrNull()
                
                if (episodeNumber != null) {
                    title = if (seriesName != null) {
                        "$seriesName - S${seasonNumber}E${episodeNumber}"
                    } else {
                        "S${seasonNumber}E${episodeNumber}"
                    }
                }
            } else if (parentName.equals("Movies", ignoreCase = true)) {
                // It's a movie in Movies folder
                contentType = ContentType.MOVIE
            } else {
                // Fallback: Check filename for SxxExx
                val sxeMatch = Regex("""S(\d+)E(\d+)""", RegexOption.IGNORE_CASE).find(fileName)
                if (sxeMatch != null) {
                     contentType = ContentType.EPISODE
                     seasonNumber = sxeMatch.groupValues[1].toIntOrNull()
                     episodeNumber = sxeMatch.groupValues[2].toIntOrNull()
                     // Assume parent folder is Series Name if not in Downloads root
                     if (parentDir.name != "MaterialTV" && parentDir.name != "Downloads") {
                         seriesName = parentName
                     }
                } else {
                     // Default to Movie
                     contentType = ContentType.MOVIE
                }
            }
            
            // Kapak resmi var mı kontrol et
            // Check in parent (S01) and parent's parent (Series)
            val coverFile = java.io.File(parentDir, "cover.png")
            val seriesCoverFile = if (parentDir.parentFile != null) java.io.File(parentDir.parentFile, "cover.png") else null
            
            val thumbnailUrl = if (coverFile.exists()) {
                "file://${coverFile.absolutePath}"
            } else if (seriesCoverFile?.exists() == true) {
                "file://${seriesCoverFile.absolutePath}"
            } else null
            
            val downloadItem = DownloadItem(
                title = title,
                url = "", // Mevcut dosya, URL yok
                filePath = file.absolutePath,
                thumbnailUrl = thumbnailUrl,
                contentType = contentType,
                seriesName = seriesName,
                seasonNumber = seasonNumber,
                episodeNumber = episodeNumber,
                status = DownloadStatus.COMPLETED,
                progress = 100,
                downloadedBytes = file.length(),
                totalBytes = file.length()
            )
            
            repository.insertDownload(downloadItem)
            android.util.Log.d("DownloadManager", "Added existing file to database: ${file.name}")
        } catch (e: Exception) {
            android.util.Log.e("DownloadManager", "Error adding file to database: ${e.message}")
        }
    }
}
