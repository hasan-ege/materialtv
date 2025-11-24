package com.hasanege.materialtv

import android.app.DownloadManager
import android.content.Context
import android.os.Environment
import android.widget.Toast
import androidx.core.net.toUri
import com.hasanege.materialtv.model.Episode
import com.hasanege.materialtv.model.VodItem
import com.hasanege.materialtv.network.SessionManager

object DownloadHelper {

    fun startDownload(context: Context, movie: VodItem) {
        if (!canDownload(context)) return

        val url = movieUrl(movie)
        val fileName = "${movie.name}.mp4"
        val title = movie.name ?: "Downloading"

        download(context, url, title, fileName)
    }

    fun startDownload(context: Context, episode: Episode, seriesName: String) {
        if (!canDownload(context)) return

        val url = episodeUrl(episode)
        val fileName = "${seriesName} - ${episode.title}.mp4"
        val title = "${seriesName} - ${episode.title}"

        download(context, url, title, fileName)
    }

    fun downloadSeason(context: Context, episodes: List<Episode>, seriesName: String) {
        if (!canDownload(context)) return

        val max = SessionManager.userInfo?.maxConnections?.toIntOrNull() ?: 1
        
        if (episodes.size > max) {
             Toast.makeText(context, "Queueing ${episodes.size} episodes (Max concurrent: $max)", Toast.LENGTH_SHORT).show()
        }

        var startedCount = 0
        episodes.forEachIndexed { index, episode ->
             val url = episodeUrl(episode)
             val episodeNum = index + 1
             val fileName = "${seriesName} - S${episode.season ?: 1}E$episodeNum - ${episode.title}.mp4"
             val title = "${seriesName} - ${episode.title}"
             download(context, url, title, fileName)
             startedCount++
        }
        Toast.makeText(context, "Started $startedCount downloads", Toast.LENGTH_SHORT).show()
    }

    private fun canDownload(context: Context): Boolean {
        val maxConnections = SessionManager.userInfo?.maxConnections?.toIntOrNull() ?: 1
        
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query()
        query.setFilterByStatus(DownloadManager.STATUS_RUNNING or DownloadManager.STATUS_PENDING)
        val cursor = downloadManager.query(query)
        val activeCount = cursor.count
        cursor.close()

        if (activeCount >= maxConnections) {
            Toast.makeText(context, "Download limit reached (Max: $maxConnections)", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun download(context: Context, url: String, title: String, fileName: String) {
        try {
            val request = DownloadManager.Request(url.toUri())
                .setTitle(title)
                .setDescription("Downloading...")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)
                // Add headers to avoid throttling and 403s
                .addRequestHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .addRequestHeader("Accept", "*/*")
                .addRequestHeader("Connection", "keep-alive")

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(request)

            Toast.makeText(context, "Download started: $title", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun movieUrl(movie: VodItem): String {
        val extension = movie.containerExtension ?: "mp4"
        return "${SessionManager.serverUrl}/movie/${SessionManager.username}/${SessionManager.password}/${movie.streamId}.$extension"
    }

    private fun episodeUrl(episode: Episode): String {
        val extension = episode.containerExtension ?: "mkv"
        return "${SessionManager.serverUrl}/series/${SessionManager.username}/${SessionManager.password}/${episode.id}.$extension"
    }
}

