package com.hasanege.materialtv

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hasanege.materialtv.model.SeriesInfoResponse
import com.hasanege.materialtv.repository.XtreamRepository
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class SeriesDetailViewModel(private val repository: XtreamRepository) : ViewModel() {

    var seriesInfoState by mutableStateOf<UiState<SeriesInfoResponse>>(UiState.Loading)
        private set

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun loadSeriesInfo(username: String, password: String, seriesId: Int) {
        viewModelScope.launch {
            seriesInfoState = UiState.Loading
            try {
                val seriesInfoResponse = repository.getSeriesInfo(username, password, seriesId)
                if (seriesInfoResponse != null) {
                    seriesInfoState = UiState.Success(seriesInfoResponse)
                } else {
                    seriesInfoState = UiState.Error("Series not found")
                }
            } catch (e: Exception) {
                seriesInfoState = UiState.Error("Failed to load series details: ${e.message}")
            }
        }
    }
}

object SeriesDetailViewModelFactory : androidx.lifecycle.ViewModelProvider.Factory {
    private val apiService by lazy {
        com.hasanege.materialtv.network.SessionManager.serverUrl?.let { 
            com.hasanege.materialtv.network.RetrofitClient.getClient(it) 
        }
    }

    private val repository by lazy {
        XtreamRepository(apiService)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return SeriesDetailViewModel(repository) as T
    }
}
