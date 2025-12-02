package com.hasanege.materialtv.download

import android.content.Context
import android.widget.Toast
import com.hasanege.materialtv.data.DownloadAlgorithm
import com.hasanege.materialtv.data.M3uRepository
import com.hasanege.materialtv.data.SettingsRepository
import com.hasanege.materialtv.model.Episode
import com.hasanege.materialtv.model.VodItem
import com.hasanege.materialtv.network.SessionManager
import com.hasanege.materialtv.utils.startDownload
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object DownloadHelper {

    fun startDownload(context: Context, movie: VodItem) {
        val title = movie.name ?: "download"
        val url = movieUrl(movie)
        val thumbnailUrl = movie.streamIcon ?: ""
        android.util.Log.d("DownloadHelper", "Movie thumbnail URL: $thumbnailUrl")
        if (url.isEmpty()) {
            Toast.makeText(context, "Stream URL not found", Toast.LENGTH_LONG).show()
            return
        }
        
        // Use CoroutineScope to avoid blocking UI
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
             handleDownload(context, url, title, subpath = "Movies", thumbnailUrl)
        }
    }

    fun startDownload(context: Context, episode: Episode, seriesName: String) {
        val safeSeries = seriesName.ifBlank { "Series" }
        val episodeTitle = episode.title ?: "Episode"
        val url = episodeUrl(episode)
        val thumbnailUrl = episode.info?.movieImage ?: ""
        android.util.Log.d("DownloadHelper", "Episode thumbnail URL: $thumbnailUrl")
        if (url.isEmpty()) {
            Toast.makeText(context, "Stream URL not found", Toast.LENGTH_LONG).show()
            return
        }
        
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            handleDownload(context, url, episodeTitle, subpath = "Series/$safeSeries", thumbnailUrl, episode)
        }
    }

    private suspend fun handleDownload(context: Context, url: String, title: String, subpath: String, thumbnailUrl: String, episode: Episode? = null) {
        val settingsRepository = SettingsRepository.getInstance(context)
        val algorithm = settingsRepository.downloadAlgorithm.first()
        
        withContext(kotlinx.coroutines.Dispatchers.Main) {
             // Show toast on Main thread
        }

        when (algorithm) {
            DownloadAlgorithm.SYSTEM_DOWNLOAD_MANAGER -> {
                val systemManager = SystemDownloadManager.getInstance(context)
                val downloadId = systemManager.startDownload(url, title, subpath)
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    if (downloadId > 0) {
                        Toast.makeText(context, "System download started: $title", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Failed to start system download", Toast.LENGTH_LONG).show()
                    }
                }
            }
            DownloadAlgorithm.OKHTTP -> {
                // Use central DownloadManagerImpl with chunk reconnect support
                val manager = DownloadManagerImpl.getInstance(context)
                val safeTitle = sanitizedFileName(title)
                
                val downloadDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                val baseDir = java.io.File(downloadDir, "MaterialTV")
                
                val filePath = if (subpath.startsWith("Series/")) {
                    // Dizi bölümleri için daha açıklayıcı isimlendirme
                    val seriesName = subpath.substring(7) // "Series/" kısmını çıkar
                    val season = episode?.season ?: 1
                    val episodeNum = episode?.id?.toIntOrNull() ?: 1
                    val episodeTitle = episode?.title ?: "Bölüm"
                    val formattedTitle = "S.$season E.$episodeNum - $episodeTitle"
                    
                    java.io.File(baseDir, "$subpath/${formattedTitle}.mp4").absolutePath
                } else {
                    // Filmler için normal isimlendirme
                    java.io.File(baseDir, "$subpath/${safeTitle}.mp4").absolutePath
                }
                
                manager.startDownload(url, safeTitle, filePath, thumbnailUrl)
                
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    Toast.makeText(context, "App download started: $safeTitle", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun movieUrl(movie: VodItem): String {
        if (SessionManager.loginType == SessionManager.LoginType.M3U) {
            return M3uRepository.getStreamUrl(movie.streamId ?: 0) ?: ""
        }
        val extension = movie.containerExtension ?: "mp4"
        return "${SessionManager.serverUrl}/movie/${SessionManager.username}/${SessionManager.password}/${movie.streamId}.$extension"
    }

    private fun episodeUrl(episode: Episode): String {
        if (SessionManager.loginType == SessionManager.LoginType.M3U) {
            val streamId = episode.id?.toIntOrNull() ?: 0
            return M3uRepository.getStreamUrl(streamId) ?: ""
        }
        val extension = episode.containerExtension ?: "mkv"
        return "${SessionManager.serverUrl}/series/${SessionManager.username}/${SessionManager.password}/${episode.id}.$extension"
    }

    private fun sanitizedFileName(title: String): String {
        return title.replace(Regex("[\\\\/:*?\"<>|]"), "_")
    }

    private fun guessExtension(url: String): String {
        val lowered = url.lowercase()
        return when {
            lowered.endsWith(".mp4") -> ".mp4"
            lowered.endsWith(".mkv") -> ".mkv"
            lowered.endsWith(".avi") -> ".avi"
            else -> ""
        }
    }

    private const val DEFAULT_BUFFER_SIZE = 8 * 1024
}
