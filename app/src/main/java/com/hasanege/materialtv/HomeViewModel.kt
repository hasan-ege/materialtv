package com.hasanege.materialtv

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hasanege.materialtv.model.Category
import com.hasanege.materialtv.model.ContinueWatchingItem
import com.hasanege.materialtv.model.LiveStream
import com.hasanege.materialtv.model.SeriesItem
import com.hasanege.materialtv.model.VodItem
import com.hasanege.materialtv.data.M3uRepository
import com.hasanege.materialtv.network.SessionManager
import com.hasanege.materialtv.repository.XtreamRepository
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

class HomeViewModel(private val repository: XtreamRepository) : ViewModel() {

    private var _allMovies: List<VodItem> = emptyList()
    private var _allSeries: List<SeriesItem> = emptyList()
    private var _allLiveStreams: List<LiveStream> = emptyList()

    var moviesState by mutableStateOf<UiState<List<VodItem>>>(UiState.Loading)
    var seriesState by mutableStateOf<UiState<List<SeriesItem>>>(UiState.Loading)
    var liveState by mutableStateOf<UiState<List<LiveStream>>>(UiState.Loading)
    var continueWatchingState by mutableStateOf<UiState<List<ContinueWatchingItem>>>(UiState.Loading)

    var moviesByCategoriesState by mutableStateOf<UiState<Map<Category, List<VodItem>>>>(UiState.Loading)
    var seriesByCategoriesState by mutableStateOf<UiState<Map<Category, List<SeriesItem>>>>(UiState.Loading)
    var liveByCategoriesState by mutableStateOf<UiState<Map<Category, List<LiveStream>>>>(UiState.Loading)

    var movieCategories by mutableStateOf<List<Category>>(emptyList())
    var seriesCategories by mutableStateOf<List<Category>>(emptyList())
    var liveCategories by mutableStateOf<List<Category>>(emptyList())

    var selectedMovieCategoryId by mutableStateOf<String?>(null)
    var selectedSeriesCategoryId by mutableStateOf<String?>(null)
    var selectedLiveCategoryId by mutableStateOf<String?>(null)

    var searchQuery by mutableStateOf("")
    var isRefreshing by mutableStateOf(false)

    private var isInitialDataLoaded = false

    init {
        loadContinueWatching()
    }

    fun loadInitialData(username: String, password: String, forceRefresh: Boolean = false) {
        if (isInitialDataLoaded && !forceRefresh) return

        viewModelScope.launch {
            isRefreshing = true

            moviesState = UiState.Loading
            seriesState = UiState.Loading
            liveState = UiState.Loading
            moviesByCategoriesState = UiState.Loading
            seriesByCategoriesState = UiState.Loading
            liveByCategoriesState = UiState.Loading

            loadContinueWatching()

            if (SessionManager.loginType == SessionManager.LoginType.M3U) {
                try {
                    android.util.Log.d("HomeViewModel", "Loading M3U data...")
                    
                    withContext(Dispatchers.Default) {
                        // Ensure playlist is fetched if it's empty
                        if (M3uRepository.getPlaylistSize() == 0) {
                            android.util.Log.d("HomeViewModel", "Playlist is empty, fetching from URL...")
                            val m3uUrl = SessionManager.m3uUrl
                            if (m3uUrl != null) {
                                M3uRepository.fetchPlaylist(m3uUrl)
                                android.util.Log.d("HomeViewModel", "Playlist fetched, size: ${M3uRepository.getPlaylistSize()}")
                            } else {
                                android.util.Log.e("HomeViewModel", "M3U URL is null!")
                                throw IllegalStateException("M3U URL not found")
                            }
                        }
                        
                        val movies = M3uRepository.getMovies()
                        val series = M3uRepository.getSeries()
                        val live = M3uRepository.getLiveStreams()

                        android.util.Log.d("HomeViewModel", "M3U data loaded - Movies: ${movies.size} groups, Series: ${series.size} groups, Live: ${live.size} groups")

                        val movieCats = movies.keys.map { Category(it.hashCode().toString(), it, 0) }
                        val seriesCats = series.keys.map { Category(it.hashCode().toString(), it, 0) }
                        val liveCats = live.keys.map { Category(it.hashCode().toString(), it, 0) }

                        withContext(Dispatchers.Main) {
                            movieCategories = movieCats
                            seriesCategories = seriesCats
                            liveCategories = liveCats
                        }

                        _allMovies = movies.values.flatten()
                        _allSeries = series.values.flatten()
                        _allLiveStreams = live.values.flatten()

                        android.util.Log.d("HomeViewModel", "Total items - Movies: ${_allMovies.size}, Series: ${_allSeries.size}, Live: ${_allLiveStreams.size}")

                        applyFilters()
                        android.util.Log.d("HomeViewModel", "M3U data loaded successfully")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("HomeViewModel", "Failed to load M3U data", e)
                    val errorMsg = "Failed to load M3U data: ${e.javaClass.simpleName}: ${e.message}"
                    moviesState = UiState.Error(errorMsg)
                    seriesState = UiState.Error(errorMsg)
                    liveState = UiState.Error(errorMsg)
                    moviesByCategoriesState = UiState.Success(emptyMap())
                    seriesByCategoriesState = UiState.Success(emptyMap())
                    liveByCategoriesState = UiState.Success(emptyMap())
                }
            } else {
                coroutineScope {
                    launch {
                        loadMovieCategories(username, password)
                        loadAllMovies(username, password)
                    }
                    launch {
                        loadSeriesCategories(username, password)
                        loadAllSeries(username, password)
                    }
                    launch {
                        loadLiveCategories(username, password)
                        loadAllLiveStreams(username, password)
                    }
                }
            }

            isInitialDataLoaded = true
            isRefreshing = false
        }
    }

    fun loadContinueWatching() {
        viewModelScope.launch {
            continueWatchingState = UiState.Loading
            try {
                val history = WatchHistoryManager.getHistory()
                continueWatchingState = UiState.Success(history)
            } catch (e: Exception) {
                continueWatchingState = UiState.Error("Failed to load watch history")
            }
        }
    }

    private suspend fun applyFilters() {
        withContext(Dispatchers.Default) {
            // Movies
            val filteredMovies = _allMovies.filter { movie ->
                (selectedMovieCategoryId == null || movie.categoryId == selectedMovieCategoryId) && movie.name?.contains(searchQuery, ignoreCase = true) ?: false
            }
            withContext(Dispatchers.Main) {
                moviesState = UiState.Success(filteredMovies)
            }

            val moviesByCategory = filteredMovies
                .groupBy { movie -> movieCategories.find { it.categoryId == movie.categoryId } }
                .filterKeys { it != null }
                .mapKeys { it.key!! }
            withContext(Dispatchers.Main) {
                moviesByCategoriesState = UiState.Success(moviesByCategory)
            }

            // Series
            val filteredSeries = _allSeries.filter { series ->
                (selectedSeriesCategoryId == null || series.categoryId == selectedSeriesCategoryId) && series.name?.contains(searchQuery, ignoreCase = true) ?: false
            }
            withContext(Dispatchers.Main) {
                seriesState = UiState.Success(filteredSeries)
            }

            val seriesByCategory = filteredSeries
                .groupBy { series -> seriesCategories.find { it.categoryId == series.categoryId } }
                .filterKeys { it != null }
                .mapKeys { it.key!! }
            withContext(Dispatchers.Main) {
                seriesByCategoriesState = UiState.Success(seriesByCategory)
            }

            // Live TV
            val filteredLive = _allLiveStreams.filter { stream ->
                (selectedLiveCategoryId == null || stream.categoryId == selectedLiveCategoryId) && stream.name?.contains(searchQuery, ignoreCase = true) ?: false
            }
            withContext(Dispatchers.Main) {
                liveState = UiState.Success(filteredLive)
            }

            val liveByCategory = filteredLive
                .groupBy { stream -> liveCategories.find { it.categoryId == stream.categoryId } }
                .filterKeys { it != null }
                .mapKeys { it.key!! }
            withContext(Dispatchers.Main) {
                liveByCategoriesState = UiState.Success(liveByCategory)
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        searchQuery = query
        viewModelScope.launch {
            applyFilters()
        }
    }

    fun onMovieCategorySelected(categoryId: String?) {
        selectedMovieCategoryId = categoryId
        viewModelScope.launch {
            applyFilters()
        }
    }

    fun onSeriesCategorySelected(categoryId: String?) {
        selectedSeriesCategoryId = categoryId
        viewModelScope.launch {
            applyFilters()
        }
    }

    fun onLiveCategorySelected(categoryId: String?) {
        selectedLiveCategoryId = categoryId
        viewModelScope.launch {
            applyFilters()
        }
    }

    private suspend fun loadMovieCategories(username: String, password: String) {
        try {
            movieCategories = repository.getVodCategories(username, password)
        } catch (_: Exception) { /* Optional error handling */ }
    }

    private suspend fun loadSeriesCategories(username: String, password: String) {
        try {
            seriesCategories = repository.getSeriesCategories(username, password)
        } catch (_: Exception) { /* Optional error handling */ }
    }

    private suspend fun loadLiveCategories(username: String, password: String) {
        try {
            liveCategories = repository.getLiveCategories(username, password)
        } catch (_: Exception) { /* Optional error handling */ }
    }

    private suspend fun loadAllMovies(username: String, password: String) {
        try {
            _allMovies = repository.getVodStreams(username, password, null)
            applyFilters()
        } catch (e: Exception) {
            moviesState = UiState.Error("Failed to load movies: ${e.message}")
            moviesByCategoriesState = UiState.Error("Failed to load movies: ${e.message}")
        }
    }

    private suspend fun loadAllSeries(username: String, password: String) {
        try {
            _allSeries = repository.getSeries(username, password, null)
            applyFilters()
        } catch (e: Exception) {
            seriesState = UiState.Error("Failed to load series: ${e.message}")
            seriesByCategoriesState = UiState.Error("Failed to load series: ${e.message}")
        }
    }

    private suspend fun loadAllLiveStreams(username: String, password: String) {
        try {
            _allLiveStreams = repository.getLiveStreams(username, password, null)
            applyFilters()
        } catch (e: Exception) {
            liveState = UiState.Error("Failed to load live streams: ${e.message}")
            liveByCategoriesState = UiState.Error("Failed to load live streams: ${e.message}")
        }
    }
}
