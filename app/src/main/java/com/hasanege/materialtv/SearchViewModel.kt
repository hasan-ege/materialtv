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
import kotlinx.coroutines.Dispatchers
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
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val username = SessionManager.username ?: ""
                val password = SessionManager.password ?: ""
                allMovies = repository.getVodStreams(username, password, null)
                allSeries = repository.getSeries(username, password, null)
                allLiveStreams = repository.getLiveStreams(username, password, null)

                // Baslangicta tum icerigi goster
                _movies.value = UiState.Success(allMovies)
                _series.value = UiState.Success(allSeries)
                _liveStreams.value = UiState.Success(allLiveStreams)
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
        viewModelScope.launch(Dispatchers.Default) {
            if (query.isBlank()) {
                _movies.value = UiState.Success(allMovies)
                _series.value = UiState.Success(allSeries)
                _liveStreams.value = UiState.Success(allLiveStreams)
                return@launch
            }

            val filteredMovies = allMovies.filter { it.name?.contains(query, ignoreCase = true) == true }
            val filteredSeries = allSeries.filter { it.name?.contains(query, ignoreCase = true) == true }
            val filteredLiveStreams = allLiveStreams.filter { it.name?.contains(query, ignoreCase = true) == true }

            _movies.value = UiState.Success(filteredMovies)
            _series.value = UiState.Success(filteredSeries)
            _liveStreams.value = UiState.Success(filteredLiveStreams)
        }
    }
}

object SearchViewModelFactory : androidx.lifecycle.ViewModelProvider.Factory {
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
        return SearchViewModel(repository) as T
    }
}
