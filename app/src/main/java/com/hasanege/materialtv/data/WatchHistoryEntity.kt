package com.hasanege.materialtv.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watch_history")
data class WatchHistoryEntity(
    @PrimaryKey val id: String, // unique identifier, e.g., media ID
    val name: String,
    val watchedAt: Long, // timestamp
    val durationMs: Long // total watch duration for this item
)
