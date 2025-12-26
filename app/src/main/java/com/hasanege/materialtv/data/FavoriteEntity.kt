package com.hasanege.materialtv.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contentId: Int, // streamId or seriesId
    val contentType: String, // "movie", "series", "live"
    val name: String,
    val thumbnailUrl: String?,
    val addedAt: Long, // timestamp
    val customThumbnail: String? = null, // local file path or URL
    val rating: Float = 0f, // 0-5 stars
    val notes: String = "",
    val listId: Long = 0, // 0 = default "All Favorites", otherwise custom list
    val orderIndex: Int = 0, // for drag-and-drop ordering
    val genre: String? = null,
    val year: String? = null,
    val isWatched: Boolean = false,
    val categoryId: String? = null, // original category
    val seriesId: Int? = null, // for series type
    val streamIcon: String? = null // backup icon
)
