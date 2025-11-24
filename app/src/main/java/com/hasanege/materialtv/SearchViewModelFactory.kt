package com.hasanege.materialtv

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.hasanege.materialtv.network.RetrofitClient
import com.hasanege.materialtv.network.SessionManager
import com.hasanege.materialtv.repository.XtreamRepository

object SearchViewModelFactory : ViewModelProvider.Factory {
    private val apiService by lazy {
        SessionManager.serverUrl?.let { RetrofitClient.getClient(it) }
            ?: throw IllegalStateException("Server URL not set")
    }

    private val repository by lazy {
        XtreamRepository(apiService)
    }

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SearchViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SearchViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
