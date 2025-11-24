package com.hasanege.materialtv.utils

import android.util.Log

data class M3uEntry(
    val title: String,
    val url: String,
    val logo: String? = null,
    val group: String? = null,
    val duration: Long = 0
)

object M3uParser {
    private const val TAG = "M3uParser"

    fun parse(content: String): List<M3uEntry> {
        Log.d(TAG, "=== Starting M3U Parse ===")
        Log.d(TAG, "Content length: ${content.length} characters")
        Log.d(TAG, "First 200 chars: ${content.take(200)}")
        
        val entries = mutableListOf<M3uEntry>()
        val lines = content.lines()
        var currentTitle: String? = null
        var currentLogo: String? = null
        var currentGroup: String? = null

        Log.d(TAG, "Total lines to process: ${lines.size}")
        
        for (line in lines) {
            val trimmedLine = line.trim()
            if (trimmedLine.startsWith("#EXTINF:")) {
                // Parse metadata
                try {
                    // Split duration/attributes from title
                    // Format: #EXTINF:-1 key="value" key2="value", Title
                    val commaIndex = trimmedLine.indexOf(',')
                    if (commaIndex != -1) {
                        val attributesPart = trimmedLine.substring(8, commaIndex) // Skip #EXTINF:
                        currentTitle = trimmedLine.substring(commaIndex + 1).trim()

                        // Extract logo
                        // Regex to match tvg-logo="value" or tvg-logo=value
                        val logoMatch = Regex("tvg-logo=[\"']?([^\"']*)[\"']?").find(attributesPart)
                        currentLogo = logoMatch?.groupValues?.get(1)

                        // Extract group
                        // Regex to match group-title="value" or group-title=value
                        val groupMatch = Regex("group-title=[\"']?([^\"']*)[\"']?").find(attributesPart)
                        currentGroup = groupMatch?.groupValues?.get(1)
                        
                        Log.v(TAG, "Parsed metadata - Title: $currentTitle, Group: $currentGroup")
                    } else {
                        // Fallback if no comma found (rare but possible)
                        currentTitle = "Unknown Channel"
                        Log.w(TAG, "No comma found in EXTINF line: $trimmedLine")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing EXTINF line: $trimmedLine", e)
                }
            } else if (trimmedLine.isNotEmpty() && !trimmedLine.startsWith("#")) {
                // URL line
                val url = trimmedLine
                val title = currentTitle ?: "Unknown Channel"
                entries.add(M3uEntry(title, url, currentLogo, currentGroup))
                
                Log.v(TAG, "Added entry: $title -> ${url.take(50)}...")
                
                // Reset for next entry
                currentTitle = null
                currentLogo = null
                currentGroup = null
            }
        }
        
        Log.d(TAG, "=== M3U Parse Complete ===")
        Log.d(TAG, "Total entries parsed: ${entries.size}")
        
        // Group by category for summary
        val groups = entries.groupBy { it.group ?: "Uncategorized" }
        groups.forEach { (group, items) ->
            Log.d(TAG, "  Group '$group': ${items.size} entries")
        }
        
        return entries
    }
}
