package com.example.materialtv

import android.app.DownloadManager
import android.content.Context
import android.os.Environment
import android.widget.Toast
import androidx.core.net.toUri
import com.example.materialtv.model.Episode
import com.example.materialtv.model.VodItem
import com.example.materialtv.network.SessionManager

object DownloadHelper {

    fun startDownload(context: Context, movie: VodItem) {
        val url = movieUrl(movie)
        val fileName = "${movie.name}.mp4"
        val title = movie.name ?: "Downloading"

        download(context, url, title, fileName)
    }

    fun startDownload(context: Context, episode: Episode, seriesName: String) {
        val url = episodeUrl(episode)
        val fileName = "${seriesName} - ${episode.title}.mp4"
        val title = "${seriesName} - ${episode.title}"

        download(context, url, title, fileName)
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
