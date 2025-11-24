
package com.example.materialtv

import android.content.Context
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

class DownloadsViewModel(private val context: Context) : ViewModel() {

    private val _downloadedFiles = MutableStateFlow<List<File>>(emptyList())
    val downloadedFiles: StateFlow<List<File>> = _downloadedFiles

    fun fetchDownloadedFiles() {
        viewModelScope.launch {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (downloadsDir.exists() && downloadsDir.isDirectory) {
                _downloadedFiles.value = downloadsDir.listFiles()
                    ?.filter { it.isFile && it.name.endsWith(".mp4") }
                    ?: emptyList()
            }
        }
    }

    fun deleteFile(file: java.io.File) {
        viewModelScope.launch {
            // Delete the physical file
            if (file.delete()) {
                // Refresh the list after deletion
                fetchDownloadedFiles()
            }
        }
    }
}

object DownloadsViewModelFactory : ViewModelProvider.Factory {
    private lateinit var context: Context

    fun initialize(context: Context) {
        this.context = context.applicationContext
    }

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DownloadsViewModel::class.java)) {
            return DownloadsViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
