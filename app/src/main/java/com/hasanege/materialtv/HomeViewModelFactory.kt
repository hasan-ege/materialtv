package com.hasanege.materialtv

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.hasanege.materialtv.network.SessionManager
import com.hasanege.materialtv.repository.XtreamRepository

object HomeViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            // For M3U logins, we don't need the Xtream API service
            val repository = if (SessionManager.loginType == SessionManager.LoginType.M3U) {
                // Create a dummy repository - M3U data is handled through M3uRepository
                XtreamRepository(null)
            } else {
                val apiService = SessionManager.getApiService()
                XtreamRepository(apiService)
            }
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
