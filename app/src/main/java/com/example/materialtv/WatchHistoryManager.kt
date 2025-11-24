package com.example.materialtv

import android.content.Context
import android.content.SharedPreferences
import com.example.materialtv.model.ContinueWatchingItem
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

    fun saveItem(item: ContinueWatchingItem) {
        val history = getRawHistory()

        if (item.type == "series" && item.seriesId != null) {
            val existingItem = history.find { it.seriesId == item.seriesId }
            if (existingItem != null) {
                item.isPinned = existingItem.isPinned
                history.removeAll { it.seriesId == item.seriesId }
            }
        } else { // For movies or other types
            val existingItem = history.find { it.streamId == item.streamId && it.type == item.type }
            if (existingItem != null) {
                item.isPinned = existingItem.isPinned
                history.remove(existingItem)
            }
        }

        history.add(0, item)
        val updatedHistory = history.take(20)

        val jsonString = Json.encodeToString(updatedHistory)
        sharedPreferences.edit().putString(KEY_WATCH_HISTORY, jsonString).apply()
    }

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
}
