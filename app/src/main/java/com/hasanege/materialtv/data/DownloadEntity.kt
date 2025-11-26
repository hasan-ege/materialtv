package com.hasanege.materialtv.data

data class DownloadEntity(
    val id: Long = 0,
    val title: String = "",
    val url: String = "",
    val filePath: String = "",
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val progress: Int = 0,
    val downloadSpeed: Long = 0,
    val fileSize: Long = 0,
    val downloadedBytes: Long = 0
)
