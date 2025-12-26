package com.hasanege.materialtv.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_lists")
data class FavoriteListEntity(
    @PrimaryKey(autoGenerate = true) val listId: Long = 0,
    val listName: String,
    val createdAt: Long, // timestamp
    val orderIndex: Int = 0, // for ordering lists
    val iconName: String? = null, // icon identifier
    val colorHex: String? = null // custom color for the list
)
