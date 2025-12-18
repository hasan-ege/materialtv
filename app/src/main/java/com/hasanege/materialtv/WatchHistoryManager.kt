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

    private val _historyFlow = kotlinx.coroutines.flow.MutableStateFlow<List<ContinueWatchingItem>>(emptyList())
    val historyFlow: kotlinx.coroutines.flow.StateFlow<List<ContinueWatchingItem>> = _historyFlow

    fun initialize(context: Context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _historyFlow.value = getRawHistory()
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
        return _historyFlow.value.sortedByDescending { it.isPinned }
    }

    // Get only items that are not finished (for Continue Watching)
    fun getContinueWatching(thresholdMinutes: Int = 5): List<ContinueWatchingItem> {
        return _historyFlow.value
            .filter { item ->
                // Don't show dismissed items
                if (item.dismissedFromContinueWatching) return@filter false
                
                // For series, only show the latest episode per series
                if (item.type == "series" && item.seriesId != null) {
                    val seriesItems = _historyFlow.value.filter { 
                        it.seriesId == item.seriesId && 
                        it.type == "series" && 
                        !it.dismissedFromContinueWatching 
                    }
                    // Only show if this is the most recently watched episode of this series
                    val latestItem = seriesItems.maxByOrNull { it.position }
                    return@filter item.streamId == latestItem?.streamId
                }
                
                // Show if NOT finished based on threshold
                !isFinished(item, thresholdMinutes)
            }
            .sortedByDescending { it.isPinned }
    }

    // Get full watch history (all items)
    fun getFullHistory(): List<ContinueWatchingItem> {
        return _historyFlow.value
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
        _historyFlow.value = updatedHistory
    }

    // Dismiss from Continue Watching (but keep in history)
    fun dismissItem(item: ContinueWatchingItem) {
        val history = getRawHistory()
        val itemToUpdate = history.find { it.streamId == item.streamId && it.type == item.type }
        itemToUpdate?.let { it.dismissedFromContinueWatching = true }
        val jsonString = Json.encodeToString(history)
        sharedPreferences.edit().putString(KEY_WATCH_HISTORY, jsonString).apply()
        _historyFlow.value = history
    }

    // Completely remove from history
    fun removeItem(item: ContinueWatchingItem) {
        val history = getRawHistory()
        history.removeAll { it.streamId == item.streamId && it.type == item.type }
        val jsonString = Json.encodeToString(history)
        sharedPreferences.edit().putString(KEY_WATCH_HISTORY, jsonString).apply()
        _historyFlow.value = history
    }

    fun togglePin(item: ContinueWatchingItem) {
        val history = getRawHistory()
        val itemToUpdate = history.find { it.streamId == item.streamId && it.type == item.type }
        itemToUpdate?.let { it.isPinned = !it.isPinned }
        val jsonString = Json.encodeToString(history)
        sharedPreferences.edit().putString(KEY_WATCH_HISTORY, jsonString).apply()
        _historyFlow.value = history
    }

    fun clearHistory() {
        sharedPreferences.edit().remove(KEY_WATCH_HISTORY).apply()
        _historyFlow.value = emptyList()
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
        } else if (item.type == "downloaded") {
            history.find { it.streamId == item.streamId && it.type == "downloaded" }
        } else {
            history.find { it.streamId == item.streamId && it.type == item.type }
        }

        if (existingItem != null) {
            // Update existing item with accumulated watch time
            val updatedItem = item.copy(
                actualWatchTime = existingItem.actualWatchTime + additionalWatchTime,
                isPinned = existingItem.isPinned,
                dismissedFromContinueWatching = existingItem.dismissedFromContinueWatching,
                streamIcon = if (item.type == "downloaded") existingItem.streamIcon else item.streamIcon
            )
            
            if (item.type == "series" && item.seriesId != null) {
                history.removeAll { it.seriesId == item.seriesId }
            } else if (item.type == "downloaded") {
                history.removeAll { it.streamId == item.streamId && it.type == "downloaded" }
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
        _historyFlow.value = updatedHistory
    }

    // Helper to check if an item is considered "finished" based on threshold
    fun isFinished(item: ContinueWatchingItem, thresholdMinutes: Int): Boolean {
         if (item.duration <= 0) return false
         val remainingMillis = item.duration - item.position
         val thresholdMillis = thresholdMinutes * 60 * 1000L
         return remainingMillis <= thresholdMillis
    }
}
