package com.example.materialtv

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.materialtv.network.SessionManager
import com.example.materialtv.repository.XtreamRepository

object HomeViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            val apiService = SessionManager.getApiService()
            val repository = XtreamRepository(apiService)
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}