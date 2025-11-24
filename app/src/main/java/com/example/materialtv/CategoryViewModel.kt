package com.example.materialtv

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.materialtv.model.Category
import com.example.materialtv.model.VodItem
import com.example.materialtv.network.SessionManager
import com.example.materialtv.repository.XtreamRepository
import kotlinx.coroutines.launch

sealed class CategoryData {
    data class Movies(val items: List<VodItem>) : CategoryData()
    data class Series(val items: List<com.example.materialtv.model.SeriesItem>) : CategoryData()
    data class LiveStreams(val items: List<com.example.materialtv.model.LiveStream>) : CategoryData()
}

class CategoryViewModel(private val repository: XtreamRepository) : ViewModel() {

    val uiState = mutableStateOf<UiState<CategoryData>>(UiState.Loading)

    fun loadCategoryItems(categoryId: String, categoryType: String) {
        viewModelScope.launch {
            uiState.value = UiState.Loading
            try {
                val username = SessionManager.username ?: ""
                val password = SessionManager.password ?: ""

                val result = when (categoryType) {
                    "movie" -> CategoryData.Movies(repository.getVodStreams(username, password, categoryId))
                    "series" -> CategoryData.Series(repository.getSeries(username, password, categoryId))
                    "live" -> CategoryData.LiveStreams(repository.getLiveStreams(username, password, categoryId))
                    else -> throw IllegalArgumentException("Invalid category type")
                }
                uiState.value = UiState.Success(result)
            } catch (e: Exception) {
                uiState.value = UiState.Error(e.message ?: "An unknown error occurred")
            }
        }
    }
}

object CategoryViewModelFactory : ViewModelProvider.Factory {
    private val apiService by lazy {
        SessionManager.serverUrl?.let { com.example.materialtv.network.RetrofitClient.getClient(it) }
            ?: throw IllegalStateException("Server URL not set")
    }

    private val repository by lazy {
        XtreamRepository(apiService)
    }

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CategoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CategoryViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}