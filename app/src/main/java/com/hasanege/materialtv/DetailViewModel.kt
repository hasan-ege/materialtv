package com.hasanege.materialtv

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hasanege.materialtv.model.SeriesInfoResponse
import com.hasanege.materialtv.model.VodInfoResponse
import com.hasanege.materialtv.repository.XtreamRepository
import kotlinx.coroutines.launch

class DetailViewModel(private val repository: XtreamRepository) : ViewModel() {
    var movie by mutableStateOf<UiState<VodInfoResponse>>(UiState.Loading)
    var series by mutableStateOf<UiState<SeriesInfoResponse>>(UiState.Loading)

    fun loadMovieDetails(username: String, password: String, streamId: Int) {
        viewModelScope.launch {
            movie = UiState.Loading
            try {
                val foundMovie = repository.getVodDetails(username, password, streamId)
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
                if (seriesDetails != null) {
                    series = UiState.Success(seriesDetails)
                } else {
                    series = UiState.Error("Series not found")
                }
            } catch (e: Exception) {
                series = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
