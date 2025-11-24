package com.hasanege.materialtv

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hasanege.materialtv.model.LiveStream
import com.hasanege.materialtv.model.SeriesItem
import com.hasanege.materialtv.model.VodItem
import com.hasanege.materialtv.network.SessionManager
import com.hasanege.materialtv.repository.XtreamRepository
import kotlinx.coroutines.launch

class SearchViewModel(private val repository: XtreamRepository) : ViewModel() {

    private var allMovies: List<VodItem> = emptyList()
    private var allSeries: List<SeriesItem> = emptyList()
    private var allLiveStreams: List<LiveStream> = emptyList()

    private val _movies = mutableStateOf<UiState<List<VodItem>>>(UiState.Success(emptyList()))
    val movies: State<UiState<List<VodItem>>> = _movies

    private val _series = mutableStateOf<UiState<List<SeriesItem>>>(UiState.Success(emptyList()))
    val series: State<UiState<List<SeriesItem>>> = _series

    private val _liveStreams = mutableStateOf<UiState<List<LiveStream>>>(UiState.Success(emptyList()))
    val liveStreams: State<UiState<List<LiveStream>>> = _liveStreams

    private val _isLoading = mutableStateOf(true)
    val isLoading: State<Boolean> = _isLoading

    init {
        loadAllContent()
    }

    private fun loadAllContent() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val username = SessionManager.username ?: ""
                val password = SessionManager.password ?: ""
                allMovies = repository.getVodStreams(username, password, null)
                allSeries = repository.getSeries(username, password, null)
                allLiveStreams = repository.getLiveStreams(username, password, null)
            } catch (e: Exception) {
                val error = UiState.Error(e.message ?: "An unknown error occurred")
                _movies.value = error
                _series.value = error
                _liveStreams.value = error
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun search(query: String) {
        if (_isLoading.value) return

        if (query.isBlank()) {
            _movies.value = UiState.Success(emptyList())
            _series.value = UiState.Success(emptyList())
            _liveStreams.value = UiState.Success(emptyList())
            return
        }

        _movies.value = UiState.Success(
            allMovies.filter { it.name?.contains(query, ignoreCase = true) == true }
        )
        _series.value = UiState.Success(
            allSeries.filter { it.name?.contains(query, ignoreCase = true) == true }
        )
        _liveStreams.value = UiState.Success(
            allLiveStreams.filter { it.name?.contains(query, ignoreCase = true) == true }
        )
    }
}
