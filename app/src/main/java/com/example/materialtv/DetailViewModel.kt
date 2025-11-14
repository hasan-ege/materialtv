package com.example.materialtv

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.materialtv.model.SeriesInfoResponse
import com.example.materialtv.model.VodItem
import com.example.materialtv.repository.XtreamRepository
import kotlinx.coroutines.launch

class DetailViewModel(private val repository: XtreamRepository) : ViewModel() {
    var movie by mutableStateOf<UiState<VodItem>>(UiState.Loading)
    var series by mutableStateOf<UiState<SeriesInfoResponse>>(UiState.Loading)

    fun loadMovieDetails(username: String, password: String, streamId: Int) {
        viewModelScope.launch {
            movie = UiState.Loading
            try {
                val foundMovie = repository.getVodInfo(username, password, streamId)
                if (foundMovie != null) {
                    movie = UiState.Success(foundMovie)
                } else {
                    movie = UiState.Error("Movie not found")
                }
            } catch (e: Exception) {
                movie = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun loadSeriesDetails(username: String, password: String, seriesId: Int, episodeId: String? = null) {
        viewModelScope.launch {
            series = UiState.Loading
            try {
                val seriesDetails = repository.getSeriesInfo(username, password, seriesId)
                series = UiState.Success(seriesDetails)
            } catch (e: Exception) {
                series = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
