package com.hasanege.materialtv.data

import kotlinx.serialization.Serializable

@Serializable
data class DownloadEntity(
    val id: String = "",
    val title: String = "",
    val url: String = "",
    val filePath: String = "",
    val thumbnailUrl: String = "",
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val progress: Int = 0,
    val downloadSpeed: Long = 0,
    val fileSize: Long = 0,
    val downloadedBytes: Long = 0,
    val systemDownloadId: Long = -1L,
    val retryCount: Int = 0
)
