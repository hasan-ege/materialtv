package com.hasanege.materialtv

import android.content.Context
import android.content.SharedPreferences
import com.hasanege.materialtv.model.ContinueWatchingItem
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object WatchHistoryManager {
    private const val PREFS_NAME = "watch_history_prefs"
    private const val KEY_WATCH_HISTORY = "watch_history"

    private lateinit var sharedPreferences: SharedPreferences

    fun initialize(context: Context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun getRawHistory(): MutableList<ContinueWatchingItem> {
        val jsonString = sharedPreferences.getString(KEY_WATCH_HISTORY, null) ?: return mutableListOf()
        return try {
            Json.decodeFromString<List<ContinueWatchingItem>>(jsonString).toMutableList()
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    fun getHistory(): List<ContinueWatchingItem> {
        return getRawHistory().sortedByDescending { it.isPinned }
    }

    // Get only items that are not finished (for Continue Watching)
    fun getContinueWatching(): List<ContinueWatchingItem> {
        return getRawHistory()
            .filter { item ->
                // Don't show dismissed items
                if (item.dismissedFromContinueWatching) return@filter false
                
                // Show if progress is less than 95%
                val progress = if (item.duration > 0) {
                    (item.position.toFloat() / item.duration.toFloat())
                } else {
                    0f
                }
                progress < 0.95f && progress > 0.01f
            }
            .sortedByDescending { it.isPinned }
    }

    // Get full watch history (all items)
    fun getFullHistory(): List<ContinueWatchingItem> {
        return getRawHistory()
    }

    fun saveItem(item: ContinueWatchingItem) {
        val history = getRawHistory()

        if (item.type == "series" && item.seriesId != null) {
            val existingItem = history.find { it.seriesId == item.seriesId }
            if (existingItem != null) {
                item.isPinned = existingItem.isPinned
                item.dismissedFromContinueWatching = existingItem.dismissedFromContinueWatching
                history.removeAll { it.seriesId == item.seriesId }
            }
        } else { // For movies or other types
            val existingItem = history.find { it.streamId == item.streamId && it.type == item.type }
            if (existingItem != null) {
                item.isPinned = existingItem.isPinned
                item.dismissedFromContinueWatching = existingItem.dismissedFromContinueWatching
                history.remove(existingItem)
            }
        }

        history.add(0, item)
        val updatedHistory = history.take(20)

        val jsonString = Json.encodeToString(updatedHistory)
        sharedPreferences.edit().putString(KEY_WATCH_HISTORY, jsonString).apply()
    }

    // Dismiss from Continue Watching (but keep in history)
    fun dismissItem(item: ContinueWatchingItem) {
        val history = getRawHistory()
        val itemToUpdate = history.find { it.streamId == item.streamId && it.type == item.type }
        itemToUpdate?.let { it.dismissedFromContinueWatching = true }
        val jsonString = Json.encodeToString(history)
        sharedPreferences.edit().putString(KEY_WATCH_HISTORY, jsonString).apply()
    }

    // Completely remove from history
    fun removeItem(item: ContinueWatchingItem) {
        val history = getRawHistory()
        history.removeAll { it.streamId == item.streamId && it.type == item.type }
        val jsonString = Json.encodeToString(history)
        sharedPreferences.edit().putString(KEY_WATCH_HISTORY, jsonString).apply()
    }

    fun togglePin(item: ContinueWatchingItem) {
        val history = getRawHistory()
        val itemToUpdate = history.find { it.streamId == item.streamId && it.type == item.type }
        itemToUpdate?.let { it.isPinned = !it.isPinned }
        val jsonString = Json.encodeToString(history)
        sharedPreferences.edit().putString(KEY_WATCH_HISTORY, jsonString).apply()
    }

    fun clearHistory() {
        sharedPreferences.edit().remove(KEY_WATCH_HISTORY).apply()
    }

    // Get total actual watch time (excluding seeking/skipping)
    fun getTotalActualWatchTime(): Long {
        return getFullHistory().sumOf { it.actualWatchTime }
    }

    // Update item with actual watch time tracking
    fun saveItemWithWatchTime(item: ContinueWatchingItem, additionalWatchTime: Long) {
        val history = getRawHistory()
        val existingItem = if (item.type == "series" && item.seriesId != null) {
            history.find { it.seriesId == item.seriesId }
        } else {
            history.find { it.streamId == item.streamId && it.type == item.type }
        }

        if (existingItem != null) {
            // Update existing item with accumulated watch time
            val updatedItem = item.copy(
                actualWatchTime = existingItem.actualWatchTime + additionalWatchTime,
                isPinned = existingItem.isPinned,
                dismissedFromContinueWatching = existingItem.dismissedFromContinueWatching
            )
            
            if (item.type == "series" && item.seriesId != null) {
                history.removeAll { it.seriesId == item.seriesId }
            } else {
                history.removeAll { it.streamId == item.streamId && it.type == item.type }
            }
            history.add(0, updatedItem)
        } else {
            // New item
            history.add(0, item.copy(actualWatchTime = additionalWatchTime))
        }

        val updatedHistory = history.take(20)
        val jsonString = Json.encodeToString(updatedHistory)
        sharedPreferences.edit().putString(KEY_WATCH_HISTORY, jsonString).apply()
    }
}
