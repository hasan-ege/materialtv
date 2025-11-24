package com.hasanege.materialtv

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hasanege.materialtv.model.Episode
import com.hasanege.materialtv.model.SeriesDetailScreenData
import com.hasanege.materialtv.repository.XtreamRepository
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement

class SeriesDetailViewModel(private val repository: XtreamRepository) : ViewModel() {

    var seriesInfoState by mutableStateOf<UiState<SeriesDetailScreenData>>(UiState.Loading)
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
                    val episodes: Map<String, List<Episode>>? = seriesInfoResponse.episodes?.let {
                        json.decodeFromJsonElement(
                            MapSerializer(String.serializer(), ListSerializer(Episode.serializer())),
                            it
                        )
                    }
                    seriesInfoState = UiState.Success(SeriesDetailScreenData(seriesInfoResponse.info, episodes))
                } else {
                    seriesInfoState = UiState.Error("Series not found")
                }
            } catch (e: Exception) {
                seriesInfoState = UiState.Error("Failed to load series details: ${e.message}")
            }
        }
    }
}
