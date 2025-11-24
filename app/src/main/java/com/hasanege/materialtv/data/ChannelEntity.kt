package com.hasanege.materialtv.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "channels")
data class ChannelEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val streamId: Int, // Hash of the URL or provided ID
    val name: String,
    val url: String,
    val logo: String?,
    val originalGroup: String,
    val sourceId: String, // ID of the playlist/provider
    val virtualCategoryId: String?, // For merged categories
    val isHidden: Boolean = false, // For Panic Mode
    val isAdult: Boolean = false, // Helper for Panic Mode
    val type: String // "LIVE", "MOVIE", "SERIES"
)
