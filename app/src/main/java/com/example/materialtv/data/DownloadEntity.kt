package com.example.materialtv.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val filePath: String,
    val title: String,
    val status: String, // QUEUED, DOWNLOADING, COMPLETED, FAILED
    val progress: Int = 0,
    val playlistId: Long? = null
)
