package com.hasanege.materialtv.download

import android.content.Context
import com.hasanege.materialtv.data.DownloadEntity
import com.hasanege.materialtv.data.DownloadStatus
import java.io.File

object LocalFileScanner {
    
    fun scanDownloadedFiles(context: Context): List<DownloadEntity> {
        val downloads = mutableListOf<DownloadEntity>()
        
        val baseDir = File("/storage/emulated/0/Download")
        if (!baseDir.exists()) return downloads
        
        // Scan Series folder
        val seriesDir = File(baseDir, "Series")
        if (seriesDir.exists() && seriesDir.isDirectory) {
            seriesDir.listFiles()?.forEach { seriesFolder ->
                if (seriesFolder.isDirectory) {
                    seriesFolder.listFiles { file ->
                        file.extension.lowercase() in listOf("mp4", "mkv", "avi", "mov")
                    }?.forEach { videoFile ->
                        downloads.add(createDownloadEntity(videoFile, "series"))
                    }
                }
            }
        }
        
        // Scan Movies folder
        val moviesDir = File(baseDir, "Movies")
        if (moviesDir.exists() && moviesDir.isDirectory) {
            moviesDir.listFiles { file ->
                file.extension.lowercase() in listOf("mp4", "mkv", "avi", "mov")
            }?.forEach { videoFile ->
                downloads.add(createDownloadEntity(videoFile, "movie"))
            }
        }
        
        return downloads
    }
    
    private fun createDownloadEntity(file: File, type: String): DownloadEntity {
        return DownloadEntity(
            id = "local_${file.absolutePath.hashCode()}",
            title = file.nameWithoutExtension,
            url = "", // No URL for local files
            filePath = file.absolutePath,
            thumbnailUrl = "",
            status = DownloadStatus.COMPLETED,
            progress = 100,
            downloadSpeed = 0L,
            fileSize = file.length(),
            downloadedBytes = file.length(),
            systemDownloadId = -1L
        )
    }
}
