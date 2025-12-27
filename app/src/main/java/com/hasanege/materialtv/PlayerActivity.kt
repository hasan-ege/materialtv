
package com.hasanege.materialtv

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.RadioButton
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.PlayerView
import com.hasanege.materialtv.model.ContinueWatchingItem
import com.hasanege.materialtv.model.Episode
import com.hasanege.materialtv.model.VodItem
import com.hasanege.materialtv.network.SessionManager
import com.hasanege.materialtv.ui.theme.MaterialTVTheme
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs

import com.hasanege.materialtv.player.PlayerEngine
import com.hasanege.materialtv.player.ExoPlayerEngine
import com.hasanege.materialtv.player.LibVlcEngine
import android.widget.FrameLayout
import android.view.ViewGroup
import androidx.compose.ui.text.font.FontFamily
import androidx.lifecycle.lifecycleScope
import com.hasanege.materialtv.R
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

private val json = Json {
    ignoreUnknownKeys = true
}

@UnstableApi
class PlayerActivity : ComponentActivity() {

    // Lazy detailViewModel - only initialized when needed (not for local files)
    private val detailViewModel: DetailViewModel by lazy { 
        androidx.lifecycle.ViewModelProvider(this, DetailViewModelFactory)[DetailViewModel::class.java]
    }
    private val snackbarHostState = androidx.compose.material3.SnackbarHostState()
    private var playerEngine by mutableStateOf<PlayerEngine?>(null)
    private var currentMovie: VodItem? = null
    private var currentSeriesEpisode: Episode? = null
    private var seriesId: Int = -1
    private var title by mutableStateOf<String?>(null)
    private var currentUrl: String? = null
    private var isVlc by mutableStateOf(false) // Default to false (ExoPlayer) initially
    private var lastPlaybackPosition: Long = 0L
    private var statsForNerds by mutableStateOf(false)
    private var liveStreamId: Int = -1
    private var liveStreamName: String? = null
    private var isLiveStream: Boolean = false
    private var isDownloadedFile: Boolean = false
    private var streamId: Int = -1
    private var uri: String? = null
    private var originalUrl: String? = null
    private var streamIcon: String? = null
    
    // Track actual watch time (excluding seeking/skipping)
    private var sessionStartTime: Long = 0L
    private var lastPosition: Long = 0L
    private var actualWatchTime: Long = 0L
    private var lastSavedActualWatchTime: Long = 0L
    private var isInPipMode by mutableStateOf(false)
    private var isEnteringPipMode by mutableStateOf(false)
    private var wasPlayingBeforePause: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Disable activity transition animations for VLC, ExoPlayer, and Hybrid modes
        overridePendingTransition(0, 0)
        
        // Keep screen on during playback
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Register PiP action receiver
        val filter = android.content.IntentFilter().apply {
            addAction(PIP_ACTION_PLAY_PAUSE_INTENT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(pipActionReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(pipActionReceiver, filter)
        }
        
        // Default Auto-Enter PiP to false. We will enable it only when playback actually starts.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            setPictureInPictureParams(
                PictureInPictureParams.Builder()
                    .setAutoEnterEnabled(false)
                    .build()
            )
        }

        streamId = intent.getIntExtra("STREAM_ID", -1)
        seriesId = intent.getIntExtra("SERIES_ID", -1)
        val episodeId = intent.getStringExtra("EPISODE_ID")
        this.title = intent.getStringExtra("TITLE")
        val position = intent.getLongExtra("position", 0L)
        val liveUrl = intent.getStringExtra("url")
        uri = intent.getStringExtra("URI")
        isDownloadedFile = intent.getBooleanExtra("IS_DOWNLOADED_FILE", false)
        originalUrl = intent.getStringExtra("ORIGINAL_URL")
        
        // Check if this is a live stream
        isLiveStream = liveUrl != null && streamId == -1 && seriesId == -1
        if (isLiveStream) {
            liveStreamId = intent.getIntExtra("LIVE_STREAM_ID", -1)
            liveStreamName = this.title ?: "Live Stream"
            streamIcon = intent.getStringExtra("STREAM_ICON")
        }

        // Read default player synchronously using singleton
        val settingsRepository = com.hasanege.materialtv.data.SettingsRepository.getInstance(this)
        var useVlcForDownloads = true
        // Read settings instantly from StateFlow (Memory)
        val player = settingsRepository.defaultPlayer.value
        // Check if we're being forced to use VLC due to ExoPlayer failure
        val forceVlc = intent.getBooleanExtra("forceVlc", false)
        isVlc = forceVlc || (player == "VLC")
        statsForNerds = settingsRepository.statsForNerds.value
        useVlcForDownloads = settingsRepository.useVlcForDownloads.value

        // Indirilmis icerik (yerel dosya) aciliyorsa, ayara gore VLC zorla
        val currentUri = uri
        if (currentUri != null && useVlcForDownloads) {
            isVlc = true
        }

        if (currentUri != null) {
            initializePlayer(currentUri, position)
            setContent {
                MaterialTVTheme {
                    Box(modifier = Modifier.fillMaxSize()) {
                        playerEngine?.let {
                            FullscreenPlayer(
                                engine = it, 
                                title = this@PlayerActivity.title, 
                                showStats = statsForNerds,
                                inPipMode = isInPipMode,
                                onNext = {}, 
                                onPrevious = {}, 
                                onSwitchEngine = { switchEngine() }
                            )
                        }
                        androidx.compose.material3.SnackbarHost(
                            hostState = snackbarHostState,
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                        BackHandler {
                            finish()
                        }
                    }
                }
            }
            return
        }

        if (liveUrl != null) {
            android.util.Log.d("PlayerActivity", "Playing live URL: $liveUrl")
            if (liveUrl.isEmpty()) {
                android.util.Log.e("PlayerActivity", "URL is empty!")
                android.widget.Toast.makeText(this, "Stream URL not found", android.widget.Toast.LENGTH_LONG).show()
                finish()
                return
            }
            initializePlayer(liveUrl, position)
            setContent {
                MaterialTVTheme {
                    Box(modifier = Modifier.fillMaxSize()) {
                        playerEngine?.let {
                            FullscreenPlayer(
                                engine = it, 
                                title = this@PlayerActivity.title,
                                showStats = statsForNerds,
                                inPipMode = isInPipMode,
                                onNext = {}, 
                                onPrevious = {}, 
                                onSwitchEngine = { switchEngine() }
                            )
                        }
                        androidx.compose.material3.SnackbarHost(
                            hostState = snackbarHostState,
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                        BackHandler {
                            finish()
                        }
                    }
                }
            }
            return
        }


        val username = SessionManager.username ?: ""
        val password = SessionManager.password ?: ""

        if (streamId != -1) {
            detailViewModel.loadMovieDetails(username, password, streamId)
        } else if (seriesId != -1) {
            detailViewModel.loadSeriesDetails(username, password, seriesId, episodeId)
        }

        setContent {
            MaterialTVTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    val movieState = detailViewModel.movie
                    val seriesState = detailViewModel.series
                    val watchHistory: List<ContinueWatchingItem> by WatchHistoryManager.historyFlow.collectAsState()
                    val nextEpisodeThreshold by com.hasanege.materialtv.data.SettingsRepository.getInstance(LocalContext.current)
                        .nextEpisodeThresholdMinutes.collectAsState(initial = 5)
                    var hasPlayed by remember { mutableStateOf(intent.getBooleanExtra("AUTO_PLAY", false)) }

                // Auto Play Logic
                LaunchedEffect(movieState, seriesState) {
                    if (intent.getBooleanExtra("AUTO_PLAY", false) && playerEngine == null) {
                        if (movieState is UiState.Success) {
                            val response = movieState.data
                             val vodItem = VodItem(
                                streamId = response.movieData?.streamId?.toIntOrNull() ?: 0,
                                name = response.info?.name ?: "",
                                streamIcon = response.info?.movieImage,
                                rating5Based = response.info?.rating5based?.toDouble() ?: 0.0,
                                categoryId = response.movieData?.categoryId,
                                containerExtension = response.movieData?.containerExtension,
                                year = response.info?.year
                            )
                            // Initialize logic copied from onPlayMovie
                            this@PlayerActivity.currentMovie = vodItem
                            this@PlayerActivity.currentSeriesEpisode = null
                            val historyItem = watchHistory
                                .find { it.streamId == vodItem.streamId }
                            val startPosition = if (historyItem != null && !WatchHistoryManager.isFinished(historyItem, nextEpisodeThreshold)) {
                                 historyItem.position
                            } else 0L
                            initializePlayer(movieUrl(vodItem), startPosition)
                        } else if (seriesState is UiState.Success) {
                             // Series AutoPlay logic if needed (Play first episode or resume)
                             // For now, focusing on Movie as requested
                             // But we should at least not hang if it's a series Intent
                             val seriesData = seriesState.data
                             // .. Logic to pick episode ..
                        }
                    }
                }

                if (hasPlayed) {
                    if (playerEngine != null) {
                        playerEngine?.let {
                            FullscreenPlayer(
                                engine = it,
                                title = this@PlayerActivity.title,
                                showStats = statsForNerds,
                                inPipMode = isInPipMode,
                                onNext = { playNextEpisode() },
                                onPrevious = { playPreviousEpisode() },
                                onSwitchEngine = { switchEngine() }
                            )
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    
                    BackHandler {
                        savePlaybackPosition() // Ensure save happens before finish/transition
                        if (intent.getBooleanExtra("AUTO_PLAY", false)) {
                            finish()
                        } else {
                            hasPlayed = false
                            playerEngine?.release()
                            playerEngine = null
                            // Disable Auto-Enter PiP when back to details
                            setPipAutoEnterEnabled(false)
                        }
                    }
                } else {
                    when {
                        movieState is UiState.Success -> {
                            val response = movieState.data
                            val vodItem = VodItem(
                                streamId = response.movieData?.streamId?.toIntOrNull() ?: 0,
                                name = response.info?.name ?: "",
                                streamIcon = response.info?.movieImage,
                                rating5Based = response.info?.rating5based?.toDouble() ?: 0.0,
                                categoryId = response.movieData?.categoryId,
                                containerExtension = response.movieData?.containerExtension,
                                year = response.info?.year
                            )

                            
                            val historyItem = watchHistory.find { it.streamId == vodItem.streamId }
                            val resumePosition = if (historyItem != null && !WatchHistoryManager.isFinished(historyItem, nextEpisodeThreshold)) {
                                 historyItem.position
                            } else 0L
                            
                            val progress = if (historyItem != null && historyItem.duration > 0) {
                                 historyItem.position.toFloat() / historyItem.duration.toFloat()
                            } else 0f

                            DetailScreen(
                                movie = vodItem,
                                movieDetails = response.info,
                                watchProgress = progress,
                                resumePosition = resumePosition,
                                nextEpisodeThresholdMinutes = nextEpisodeThreshold,
                                onBack = { finish() },
                                onPlayMovie = { movie ->
                                    this@PlayerActivity.currentMovie = movie
                                    this@PlayerActivity.currentSeriesEpisode = null
                                    playerEngine?.release()
                                    // Use 'watchHistory' state to get latest
                                    val hItem = watchHistory.find { it.streamId == movie.streamId }
                                    val startPosition = if (hItem != null && !WatchHistoryManager.isFinished(hItem, nextEpisodeThreshold)) {
                                         hItem.position
                                    } else 0L
                                    initializePlayer(movieUrl(movie), startPosition)
                                    hasPlayed = true
                                },
                                onDownloadMovie = { movie ->
                                    com.hasanege.materialtv.download.DownloadManagerImpl.getInstance(applicationContext).startDownload(movie)
                                }
                            )
                        }

                        seriesState is UiState.Success -> {
                            val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true; isLenient = true }
                            val seriesData = seriesState.data
                            val epElement = seriesData.episodes
                            
                            val episodesMapRaw: Map<String, List<Episode>> = try {
                                if (epElement is kotlinx.serialization.json.JsonObject) {
                                    epElement.entries.associate { (key, element) ->
                                        val list: List<Episode> = try {
                                            json.decodeFromJsonElement(element)
                                        } catch (e: Exception) { 
                                            emptyList() 
                                        }
                                        key to list
                                    }
                                } else {
                                    emptyMap()
                                }
                            } catch (e: Exception) { 
                                emptyMap() 
                            }
                            
                            // Flatten and sort episodes
                            val allEpisodes: List<Episode> = episodesMapRaw.flatMap { (seasonKey, list) -> 
                                val sNum = seasonKey.toIntOrNull() ?: 0
                                list.map { it.copy(season = sNum) }
                            }.sortedWith(compareBy({ it.season ?: 0 }, { it.episodeNum?.toIntOrNull() ?: 0 }))

                            val lastHistoryItem = watchHistory.find { 
                                it.seriesId == seriesId && it.type == "series" && !it.dismissedFromContinueWatching
                            }

                            var resumeEpisode: Episode? = null
                            var resumePosition: Long = 0L

                            if (lastHistoryItem != null) {
                                val currentEp = allEpisodes.find { it.id == lastHistoryItem.streamId.toString() }
                                if (currentEp != null) {
                                    val isFinished = WatchHistoryManager.isFinished(lastHistoryItem, nextEpisodeThreshold)
                                    
                                    if (isFinished) {
                                        // Find next episode
                                        val idx = allEpisodes.indexOf(currentEp)
                                        if (idx >= 0 && idx < allEpisodes.size - 1) {
                                            resumeEpisode = allEpisodes[idx + 1]
                                            resumePosition = 0L
                                        } else {
                                            // Logic for when the LAST episode is finished. 
                                            // Maybe show the first episode? Or just keep showing last one?
                                            // Let's show the last one, reset to 0.
                                            resumeEpisode = currentEp
                                            resumePosition = 0L
                                        }
                                    } else {
                                        resumeEpisode = currentEp
                                        resumePosition = lastHistoryItem.position
                                    }
                                }
                            }

                            DetailScreen(
                                series = seriesState.data,
                                lastWatchedEpisode = resumeEpisode,
                                resumePosition = resumePosition,
                                nextEpisodeThresholdMinutes = nextEpisodeThreshold,
                                onBack = { finish() },
                                onPlayEpisode = { episode ->
                                    this@PlayerActivity.currentMovie = null
                                    this@PlayerActivity.currentSeriesEpisode = episode
                                    this@PlayerActivity.title = episode.title
                                    playerEngine?.release()
                                    val historyItem = watchHistory
                                        .find { it.streamId.toString() == episode.id }
                                    // Only resume if it is the EXACT same episode AND not finished (though user might force resume via list, so maybe just check history)
                                    // If user clicks "Play Next", historyItem refers to specific episode (likely none).
                                    // If user clicks "Resume", episode is the one with history.
                                    // But wait, if user clicks from the list below, we should respect that episode's history.
                                    val startPosition = if (historyItem != null && !WatchHistoryManager.isFinished(historyItem, nextEpisodeThreshold)) {
                                         historyItem.position
                                    } else 0L
                                    
                                    initializePlayer(episodeUrl(episode), startPosition)
                                    hasPlayed = true
                                },
                                onDownloadEpisode = { episode ->
                                    val seriesName = seriesData.info?.name ?: "Unknown Series"
                                    val sNum = episode.season ?: 1
                                    val epNum = episode.episodeNum?.toIntOrNull() ?: 0
                                    
                                    com.hasanege.materialtv.download.DownloadManagerImpl.getInstance(applicationContext).startDownload(
                                        episode, 
                                        seriesName,
                                        sNum,
                                        epNum,
                                        seriesData.info?.cover
                                    )
                                },
                                onDownloadSeason = { seasonNum, episodes ->
                                    val seriesName = seriesData.info?.name ?: "Unknown Series"
                                    episodes.forEach { ep ->
                                        val epNum = ep.episodeNum?.toIntOrNull() ?: 0
                                        com.hasanege.materialtv.download.DownloadManagerImpl.getInstance(applicationContext).startDownload(
                                            ep, 
                                            seriesName,
                                            seasonNum,
                                            epNum,
                                            seriesData.info?.cover
                                        )
                                    }
                                },
                                seriesId = seriesId
                            )
                        }

                        else -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.background),
                                contentAlignment = Alignment.Center
                            ) {
                                when {
                                    movieState is UiState.Loading || seriesState is UiState.Loading -> CircularProgressIndicator()
                                    movieState is UiState.Error -> Text(
                                        movieState.message,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )

                                    seriesState is UiState.Error -> Text(
                                        seriesState.message,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            }
                        }
                    }
                }
                androidx.compose.material3.SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}

    private fun initializePlayer(url: String, position: Long) {

        currentUrl = url
        playerEngine?.release()

        // Force VLC for downloaded files if configured
        val useVlcForDownloads = runBlocking { 
             com.hasanege.materialtv.data.SettingsRepository.getInstance(this@PlayerActivity).useVlcForDownloads.first() 
        }
        
        if (isDownloadedFile && useVlcForDownloads) {
            isVlc = true
        }

        val settingsRepo = com.hasanege.materialtv.data.SettingsRepository.getInstance(this@PlayerActivity)
        val newEngine = if (isVlc) LibVlcEngine() else ExoPlayerEngine()

        newEngine.apply {
            initialize(this@PlayerActivity)
            setSubtitleSize("Normal")
            setOnErrorCallback { error ->
                 if (!isVlc) {
                     lifecycleScope.launch {
                         val pref = settingsRepo.defaultPlayerPreference.first()
                         if (pref == com.hasanege.materialtv.data.PlayerPreference.HYBRID) {
                             val currentPos = this@apply.getCurrentPosition()
                             // Recreate the activity with VLC forced
                             finish()
                             startActivity(intent.apply {
                                 putExtra("URI", url)
                                 putExtra("position", currentPos)
                                 putExtra("forceVlc", true) // Force VLC to prevent loop
                                 putExtra("IS_DOWNLOADED_FILE", isDownloadedFile)
                             })
                             overridePendingTransition(0, 0)
                         } else {
                             android.widget.Toast.makeText(this@PlayerActivity, "Playback error: ${error.message}", android.widget.Toast.LENGTH_LONG).show()
                         }
                     }
                } else {
                    android.widget.Toast.makeText(this@PlayerActivity, "VLC playback error: ${error.message}", android.widget.Toast.LENGTH_LONG).show()
                }
            }
            
            // For local files, ensure we pass a valid URI string
            // If it's a file path, prefix with file:// if needed (ExoPlayer handles paths, but VLC might prefer URI)
            val prepareUrl = if (isDownloadedFile && !url.contains("://")) {
                "file://$url"
            } else {
                url
            }
            
            prepare(prepareUrl, position)
            
            // Update PiP actions when playback state changes
            setOnPlaybackStateChanged { isPlaying ->
                if (isInPipMode) {
                    updatePipActions()
                }
            }
            
            play()
        }
        playerEngine = newEngine
        
        // Enable Auto-Enter PiP now that we are playing
        setPipAutoEnterEnabled(true)
        
        // Start tracking actual watch time
        startWatchSession()
    }

    private fun switchEngine() {
        val currentPos = playerEngine?.getCurrentPosition() ?: 0L
        val currentUrl = this.currentUrl ?: return
        
        // Release current player immediately
        playerEngine?.release()
        playerEngine = null
        
        // Switch engine type
        isVlc = !isVlc
        
        // Initialize new player instantly
        initializePlayer(currentUrl, currentPos)
    }

    private fun playNextEpisode() {
        val seriesData = (detailViewModel.series as? UiState.Success)?.data ?: return
        val episodesElement = seriesData.episodes ?: return
        val episodesMap = json.decodeFromJsonElement<Map<String, List<Episode>>>(episodesElement)
        val allEpisodes = episodesMap.values.flatten()
        val currentIndex = allEpisodes.indexOf(currentSeriesEpisode)
        if (currentIndex < allEpisodes.size - 1) {
            val nextEpisode = allEpisodes[currentIndex + 1]
            playEpisode(nextEpisode)
        }
    }

    private fun playPreviousEpisode() {
        val seriesData = (detailViewModel.series as? UiState.Success)?.data ?: return
        val episodesElement = seriesData.episodes ?: return
        val episodesMap = json.decodeFromJsonElement<Map<String, List<Episode>>>(episodesElement)
        val allEpisodes = episodesMap.values.flatten()
        val currentIndex = allEpisodes.indexOf(currentSeriesEpisode)
        if (currentIndex > 0) {
            val previousEpisode = allEpisodes[currentIndex - 1]
            playEpisode(previousEpisode)
        }
    }

    private fun playEpisode(episode: Episode) {
        savePlaybackPosition()

        currentSeriesEpisode = episode
        this.title = episode.title

        playerEngine?.let { player ->
            val historyItem = WatchHistoryManager.getHistory()
                .find { it.streamId.toString() == episode.id }
            val startPosition = historyItem?.position ?: 0L
            player.prepare(episodeUrl(episode))
            player.seekTo(startPosition)
            player.play()
        }
    }


    private fun movieUrl(movie: VodItem): String {
        // For M3U, get URL from repository; for Xtream, construct it
        if (SessionManager.loginType == SessionManager.LoginType.M3U) {
            return com.hasanege.materialtv.data.M3uRepository.getStreamUrl(movie.streamId ?: 0) ?: ""
        }
        val extension = movie.containerExtension ?: "mp4"
        return "${SessionManager.serverUrl}/movie/${SessionManager.username}/${SessionManager.password}/${movie.streamId}.$extension"
    }

    private fun episodeUrl(episode: Episode): String {
        // For M3U, episodes would use their ID as stream ID
        if (SessionManager.loginType == SessionManager.LoginType.M3U) {
            val streamId = episode.id?.toIntOrNull() ?: 0
            return com.hasanege.materialtv.data.M3uRepository.getStreamUrl(streamId) ?: ""
        }
        val extension = episode.containerExtension ?: "mkv"
        return "${SessionManager.serverUrl}/series/${SessionManager.username}/${SessionManager.password}/${episode.id}.$extension"
    }

    private fun movieSubtitleUrl(movie: VodItem): String {
        return "${SessionManager.serverUrl}/movie/${SessionManager.username}/${SessionManager.password}/${movie.streamId}.srt"
    }

    private fun episodeSubtitleUrl(episode: Episode): String {
        return "${SessionManager.serverUrl}/series/${SessionManager.username}/${SessionManager.password}/${episode.id}.srt"
    }



    override fun onPause() {
        super.onPause()
        try {
            // Notify engine about pause (important for SurfaceView/PlayerView handling)
            playerEngine?.onPauseLifecycle()
            
            // Background Playback Enabled:
            // We do NOT pause here anymore. 
            // This fixes PiP transitions and allows background audio playback.
            savePlaybackPosition()
        } catch (e: Exception) {
            android.util.Log.e("PlayerActivity", "Error in onPause: ${e.message}")
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // Enter PiP when user presses home button
        // For Android 12+ (S), Auto-Enter is enabled, so we don't need to call this manually
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && playerEngine?.isPlaying() == true) {
            isEnteringPipMode = true
            enterPipMode()
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: android.content.res.Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        
        val wasInPipMode = isInPipMode
        isInPipMode = isInPictureInPictureMode
        isEnteringPipMode = false
        
        if (isInPictureInPictureMode) {
            // Entering PiP: Hide controls, update actions
            updatePipActions()
        } else if (wasInPipMode) {
            // Exiting PiP: Either user closed PiP window (X button) OR expanded back to fullscreen
            // If lifecycle is not at least STARTED, user clicked X to close
            // This check runs after the system updates lifecycle
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                if (!lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)) {
                    // User closed PiP with X button - activity is being destroyed
                    android.util.Log.d("PlayerActivity", "PiP closed - cleaning up player")
                    savePlaybackPosition()
                    playerEngine?.stop()
                    playerEngine?.release()
                    playerEngine = null
                    finish()
                }
                // If STARTED or above, user expanded PiP back to fullscreen - keep playing
            }, 100)
        }
    }

    private fun enterPipMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                // Set flag BEFORE entering PiP so lifecycle methods don't pause player
                isEnteringPipMode = true
                isInPipMode = true
                
                // Calculate aspect ratio from video format
                var aspectRatio = Rational(16, 9)
                try {
                    val videoFormat = playerEngine?.getVideoFormat()
                    if (videoFormat != null) {
                        // Format is usually "WxH" or "WxH codecs"
                        val parts = videoFormat.split(" ")[0].split("x")
                        if (parts.size == 2) {
                            val width = parts[0].toInt()
                            val height = parts[1].toInt()
                            if (width > 0 && height > 0) {
                                aspectRatio = Rational(width, height)
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Fallback to 16:9
                }

                val actions = buildPipActions()
                val params = PictureInPictureParams.Builder()
                    .setAspectRatio(aspectRatio)
                    .setActions(actions)
                    .build()
                val success = enterPictureInPictureMode(params)
                
                if (!success) {
                    // Failed to enter PiP, reset flags
                    isEnteringPipMode = false
                    isInPipMode = false
                }
            } catch (e: Exception) {
                android.util.Log.e("PlayerActivity", "Error entering PiP: ${e.message}")
                isEnteringPipMode = false
                isInPipMode = false
            }
        }
    }

    private fun setPipAutoEnterEnabled(enabled: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val params = PictureInPictureParams.Builder()
                    .setAutoEnterEnabled(enabled)
                    .build()
                setPictureInPictureParams(params)
            } catch (e: Exception) {
                android.util.Log.e("PlayerActivity", "Error setting auto-enter PiP: ${e.message}")
            }
        }
    }

    private fun updatePipActions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isInPipMode) {
            try {
                val actions = buildPipActions()
                val params = PictureInPictureParams.Builder()
                    .setActions(actions)
                    .build()
                setPictureInPictureParams(params)
            } catch (e: Exception) {
                android.util.Log.e("PlayerActivity", "Error updating PiP actions: ${e.message}")
            }
        }
    }

    private fun buildPipActions(): List<android.app.RemoteAction> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return emptyList()
        
        val actions = mutableListOf<android.app.RemoteAction>()

        // Play/Pause
        val isPlaying = playerEngine?.isPlaying() == true
        val intent = android.content.Intent(PIP_ACTION_PLAY_PAUSE_INTENT).setPackage(packageName)
        val playPauseIntent = android.app.PendingIntent.getBroadcast(
            this,
            PIP_ACTION_PLAY_PAUSE,
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        val playPauseIcon = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        val playPauseTitle = if (isPlaying) "Pause" else "Play"
        val playPauseAction = android.app.RemoteAction(
            android.graphics.drawable.Icon.createWithResource(this, playPauseIcon),
            playPauseTitle,
            playPauseTitle,
            playPauseIntent
        )
        actions.add(playPauseAction)

        return actions
    }

    private val pipActionReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: android.content.Intent?) {
            when (intent?.action) {
                PIP_ACTION_PLAY_PAUSE_INTENT -> {
                    if (playerEngine?.isPlaying() == true) {
                        playerEngine?.pause()
                    } else {
                        playerEngine?.play()
                    }
                    // updatePipActions() is called by the state change listener
                }
            }
        }
    }

    companion object {
        private const val PIP_ACTION_PLAY_PAUSE = 1
        private const val PIP_ACTION_PLAY_PAUSE_INTENT = "com.hasanege.materialtv.PIP_PLAY_PAUSE"
    }







    override fun onResume() {
        super.onResume()
        try {
            // Fix: Sync PiP state with system
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                isInPipMode = isInPictureInPictureMode
            } else {
                isInPipMode = false
            }
            // Ensure we are not stuck in "entering" state
            isEnteringPipMode = false

            if (playerEngine == null && currentUrl != null && !isFinishing) {
                initializePlayer(currentUrl!!, lastPlaybackPosition)
            } else if (playerEngine != null) {
                // Force reattach logic to fix black screen after screen off/on
                // IMPORTANT: Reattach first to create fresh PlayerView/Surface
                playerEngine?.reattach()
                
                // Then notify engine about resume (so new SurfaceView gets updated)
                playerEngine?.onResume()
                
                if (wasPlayingBeforePause) {
                    // Resume playback if it was playing before
                    playerEngine?.play()
                    wasPlayingBeforePause = false
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("PlayerActivity", "Error in onResume: ${e.message}")
        }
    }

    override fun onStop() {
        super.onStop()
        try {
            android.util.Log.d("PlayerActivity", "onStop: isInPipMode=$isInPipMode, isEnteringPipMode=$isEnteringPipMode, isFinishing=$isFinishing")
            
            // If in PiP mode and activity is stopping (user closed PiP window)
            if (isInPipMode && !isEnteringPipMode) {
                // User closed PiP window with X button
                android.util.Log.d("PlayerActivity", "onStop: PiP mode active, cleaning up")
                savePlaybackPosition()
                playerEngine?.stop()
                playerEngine?.release()
                playerEngine = null
            } else if (!isInPipMode && !isEnteringPipMode) {
                // Not in PiP mode, user navigated away or app backgrounded
                savePlaybackPosition()
                wasPlayingBeforePause = playerEngine?.isPlaying() == true
                playerEngine?.pause()
            } else {
                // Entering PiP, just save position
                savePlaybackPosition()
            }
        } catch (e: Exception) {
            android.util.Log.e("PlayerActivity", "Error in onStop: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {

            
            // Unregister PiP action receiver
            try {
                unregisterReceiver(pipActionReceiver)
            } catch (e: Exception) {
                // Receiver may not be registered
            }
            
            // Clear screen wake lock
            window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            
            savePlaybackPosition()
            playerEngine?.stop()
            playerEngine?.release()
            playerEngine = null
            currentUrl = null
        } catch (e: Exception) {
            android.util.Log.e("PlayerActivity", "Error in onDestroy: ${e.message}")
        }
    }

    private fun savePlaybackPosition() {
        android.util.Log.d("PlayerActivity", "savePlaybackPosition called. Engine: $playerEngine")
        try {
            playerEngine?.let { player ->
            val position = player.getCurrentPosition()
            val duration = player.getDuration()
            
            // Format for verification logs
            val posStr = String.format("%02d:%02d:%02d", 
               java.util.concurrent.TimeUnit.MILLISECONDS.toHours(position),
               java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(position) % 60,
               java.util.concurrent.TimeUnit.MILLISECONDS.toSeconds(position) % 60)
            val durStr = String.format("%02d:%02d:%02d", 
               java.util.concurrent.TimeUnit.MILLISECONDS.toHours(duration),
               java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(duration) % 60,
               java.util.concurrent.TimeUnit.MILLISECONDS.toSeconds(duration) % 60)
               
            android.util.Log.d("PlayerActivity", "Raw Position: $posStr, Duration: $durStr")
            lastPlaybackPosition = position
            
            // Calculate actual watch time for this session segment
            val currentTime = System.currentTimeMillis()
            val sessionWatchTime = calculateActualWatchTime(position)
            actualWatchTime += sessionWatchTime
            
            // Reset session tracking to current point to avoid double counting
            sessionStartTime = currentTime
            lastPosition = position
            
            // Calculate delta watch time to send (difference since last save)
            val deltaWatchTime = actualWatchTime - lastSavedActualWatchTime
            lastSavedActualWatchTime = actualWatchTime

            // Save if played for more than a second
            if (position > 1000) {
                // Save live stream watch time
                if (isLiveStream && liveStreamId > 0) {
                    val liveItem = ContinueWatchingItem(
                        streamId = liveStreamId,
                        name = liveStreamName ?: "Live Stream",
                        streamIcon = streamIcon,
                        duration = 0, // Live streams have no duration
                        position = position,
                        type = "live",
                        seriesId = null,
                        episodeId = null,
                        actualWatchTime = actualWatchTime // Store current session total just in case
                    )
                    WatchHistoryManager.saveItemWithWatchTime(liveItem, deltaWatchTime)
                }
                // Save downloaded file watch time
                else if (isDownloadedFile && uri != null) {
                    val currentUri = uri!! // Captured because uri is a mutable property
                    // Treat downloaded files exactly like regular content
                    val currentOriginalUrl = originalUrl
                    if (currentOriginalUrl != null && currentOriginalUrl.isNotEmpty()) {
                        // This was originally a series episode, save as series
                        val episodeInfo = com.hasanege.materialtv.data.EpisodeGroupingHelper.extractEpisodeInfo(this.title ?: "")
                        val downloadedItem = ContinueWatchingItem(
                            streamId = WatchHistoryManager.getDownloadId(currentUri),
                            name = this.title ?: "Downloaded File",
                            streamIcon = null, // Use thumbnail instead of file path
                            duration = duration,
                            position = position,
                            type = if (episodeInfo != null) "series" else "movie",
                            seriesId = episodeInfo?.seriesName?.hashCode(),
                            episodeId = currentOriginalUrl, // Store original URL
                            containerExtension = "file",
                            isDownloaded = true,
                            localPath = currentUri,
                            actualWatchTime = actualWatchTime
                        )
                        WatchHistoryManager.saveItemWithWatchTime(downloadedItem, deltaWatchTime)
                    } else {
                        // Regular downloaded file
                        val downloadedItem = ContinueWatchingItem(
                            streamId = WatchHistoryManager.getDownloadId(currentUri),
                            name = this.title ?: "Downloaded File",
                            streamIcon = currentUri, // Store file path for playback
                            duration = duration,
                            position = position,
                            type = "downloaded",
                            seriesId = null,
                            episodeId = currentOriginalUrl,
                            containerExtension = "file",
                            isDownloaded = true,
                            localPath = currentUri,
                            actualWatchTime = actualWatchTime
                        )
                        WatchHistoryManager.saveItemWithWatchTime(downloadedItem, deltaWatchTime)
                    }
                }
                // Save VoD content watch time
                else if (seriesId != -1) {
                    android.util.Log.d("PlayerActivity", "Saving Episode Progress -> Title: $title, SeriesID: $seriesId, StreamID: $streamId, Position: $posStr, Duration: $durStr")
                    val episodeItem = ContinueWatchingItem(
                        streamId = streamId,
                        name = title ?: "Episode",
                        streamIcon = streamIcon,
                        duration = duration,
                        position = position,
                        type = "series",
                        seriesId = seriesId,
                        episodeId = null, // Can be added if needed
                        actualWatchTime = actualWatchTime
                    )
                    WatchHistoryManager.saveItemWithWatchTime(episodeItem, deltaWatchTime)
                } else {
                    android.util.Log.d("PlayerActivity", "Saving Movie Progress -> Title: $title, StreamID: $streamId, Position: $posStr, Duration: $durStr")
                    val movieItem = ContinueWatchingItem(
                        streamId = streamId,
                        name = title ?: "Movie",
                        streamIcon = streamIcon,
                        duration = duration,
                        position = position,
                        type = "movie",
                        seriesId = null,
                        episodeId = null,
                        actualWatchTime = actualWatchTime
                    )
                    WatchHistoryManager.saveItemWithWatchTime(movieItem, deltaWatchTime)
                }
            }
        }
        } catch (e: Exception) {
            android.util.Log.e("PlayerActivity", "Error saving playback position: ${e.message}")
        }
    }

    private fun calculateActualWatchTime(currentPosition: Long): Long {
        val currentTime = System.currentTimeMillis()
        val elapsedSessionTime = currentTime - sessionStartTime
        
        // Calculate actual forward progress (excluding seeking backwards)
        val forwardProgress = if (currentPosition > lastPosition) {
            currentPosition - lastPosition
        } else {
            0L // User seeked backwards, don't count this time
        }
        
        // Use the minimum of elapsed time and forward progress to exclude seeking
        return minOf(elapsedSessionTime, forwardProgress)
    }

    private fun startWatchSession() {
        sessionStartTime = System.currentTimeMillis()
        lastPosition = playerEngine?.getCurrentPosition() ?: 0L
        actualWatchTime = 0L
    }

    private fun endWatchSession() {
        if (sessionStartTime > 0) {
            val currentPosition = playerEngine?.getCurrentPosition() ?: 0L
            val sessionWatchTime = calculateActualWatchTime(currentPosition)
            actualWatchTime += sessionWatchTime
            sessionStartTime = 0L
        }
    }
}

private fun formatDuration(millis: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(millis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % TimeUnit.HOURS.toMinutes(1)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % TimeUnit.MINUTES.toSeconds(1)
    return if (hours > 0) {
        String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}


@UnstableApi
@Composable
fun FullscreenPlayer(
    engine: PlayerEngine,
    title: String?,
    showStats: Boolean,
    inPipMode: Boolean = false,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSwitchEngine: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as Activity
    val window = activity.window

    // NORMAL MODE: Full UI with controls
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var controlsVisible by remember { mutableStateOf(true) }
    var showTrackSelectionDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isBuffering by remember { mutableStateOf(false) }

    // New Features State
    var resizeMode by remember { mutableStateOf(androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    var isLocked by remember { mutableStateOf(false) }
    var doubleTapState by remember { mutableStateOf<DoubleTapState?>(null) } // Left or Right

    // Slider state
    var isSeeking by remember { mutableStateOf(false) }
    var sliderValue by remember { mutableFloatStateOf(0f) }
    var showAudioDialog by remember { mutableStateOf(false) }
    var showSubtitleDialog by remember { mutableStateOf(false) }

    // Gesture control states
    var showGestureIndicator by remember { mutableStateOf(false) }
    var gestureIcon by remember { mutableStateOf<ImageVector?>(null) }
    var gestureValue by remember { mutableFloatStateOf(0f) }
    var isVolumeGesture by remember { mutableStateOf(false) }
    var currentVolume by remember { mutableFloatStateOf(0f) }
    var currentBrightness by remember { mutableFloatStateOf(0.5f) }

    LaunchedEffect(engine) {
        while(true) {
            isPlaying = engine.isPlaying()
            duration = engine.getDuration()
            currentPosition = engine.getCurrentPosition()
            
            // Only update slider if not seeking
            if (!isSeeking) {
                sliderValue = currentPosition.toFloat()
            }
            
            delay(500)
        }
    }

    LaunchedEffect(controlsVisible) {
        if (controlsVisible) {
            delay(5000L)
            controlsVisible = false
        }
    }

    // Hide indicator after a delay
    LaunchedEffect(showGestureIndicator) {
        if (showGestureIndicator) {
            delay(1000L)
            showGestureIndicator = false
        }
    }
    
    // Hide double tap animation
    LaunchedEffect(doubleTapState) {
        if (doubleTapState != null) {
            delay(600L)
            doubleTapState = null
        }
    }

    // No specific lifecycle observer needed for pausing, as we want background playback.
    // Cleanup is handled by Activity's onDestroy.


    LaunchedEffect(Unit) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let {
            it.hide(WindowInsetsCompat.Type.systemBars())
            it.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { 
                        if (!isLocked) controlsVisible = !controlsVisible 
                        else if (controlsVisible) controlsVisible = false // Allow hiding if stuck
                        else {
                             // Show lock icon briefly?
                             controlsVisible = true
                        }
                    },
                    onDoubleTap = { offset ->
                        if (!isLocked) {
                            val screenWidth = size.width
                            if (offset.x < screenWidth / 2) {
                                // Rewind
                                engine.seekBack()
                                doubleTapState = DoubleTapState.Rewind
                            } else {
                                // Forward
                                engine.seekForward()
                                doubleTapState = DoubleTapState.Forward
                            }
                        }
                    }
                )
            }
    ) {
        Box(modifier = Modifier
            .fillMaxSize()
            .pointerInput(isLocked) {
                if (!isLocked) {
                    detectDragGestures(
                        onDragStart = {
                            val screenWidth = size.width
                            isVolumeGesture = it.x > screenWidth / 2
                            showGestureIndicator = true
                            if (isVolumeGesture) {
                                currentVolume =
                                    audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
                            } else {
                                currentBrightness =
                                    window.attributes.screenBrightness.takeIf { br -> br > 0 } ?: 0.5f
                            }
                        },
                        onDragEnd = {
                            showGestureIndicator = false
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        val (x, y) = dragAmount
                        val screenHeight = size.height

                        if (abs(x) > abs(y)) return@detectDragGestures // Ignore horizontal drags

                        if (isVolumeGesture) {
                            val delta = (-y / screenHeight) * maxVolume
                            currentVolume = (currentVolume + delta).coerceIn(0f, maxVolume.toFloat())
                            audioManager.setStreamVolume(
                                AudioManager.STREAM_MUSIC,
                                currentVolume.toInt(),
                                0
                            )

                            gestureIcon = Icons.AutoMirrored.Filled.VolumeUp
                            gestureValue = (currentVolume / maxVolume) * 100
                        } else { // Brightness
                            val delta = -y / screenHeight
                            currentBrightness = (currentBrightness + delta).coerceIn(0f, 1f)

                            window.attributes =
                                window.attributes.apply { screenBrightness = currentBrightness }

                            gestureIcon = Icons.Default.BrightnessMedium
                            gestureValue = currentBrightness * 100
                        }
                    }
                }
            })
        {
            AndroidView(
                factory = { ctx ->
                    FrameLayout(ctx).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT
                        )
                        engine.attach(this)
                    }
                },
                update = { view ->
                    // Force layout update when PiP mode changes or window resizes
                    view.requestLayout()
                },
                modifier = Modifier.fillMaxSize()
            )
            
            // NOTE: Do NOT use DisposableEffect to detach engine here!
            // It causes black screen during PiP transitions because recomposition triggers onDispose
            
            // Double Tap Animation Overlay
            doubleTapState?.let { state ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.2f)),
                    contentAlignment = if (state == DoubleTapState.Rewind) Alignment.CenterStart else Alignment.CenterEnd
                ) {
                     Column(
                         modifier = Modifier.padding(50.dp),
                         horizontalAlignment = Alignment.CenterHorizontally
                     ) {
                         Icon(
                             imageVector = if (state == DoubleTapState.Rewind) Icons.Default.Replay10 else Icons.Default.Forward10,
                             contentDescription = null,
                             tint = Color.White,
                             modifier = Modifier.size(50.dp)
                         )
                         Text(stringResource(R.string.player_10s), color = Color.White, fontWeight = FontWeight.Bold)
                     }
                }
            }

            AnimatedVisibility(
                visible = controlsVisible && !inPipMode,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(modifier = Modifier.background(Color.Black.copy(alpha = 0.4f))) {
                    
                    // Top Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopStart)
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                         title?.let {
                            Text(
                                text = it,
                                color = Color.White,
                                fontSize = 18.sp,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        
                        Row {
                             // Audio Track Button
                             IconButton(onClick = { showAudioDialog = true }) {
                                 Icon(
                                     imageVector = Icons.Default.GraphicEq,
                                     contentDescription = "Audio Track",
                                     tint = Color.White
                                 )
                             }
                             
                             // Subtitle Button
                             IconButton(onClick = { showSubtitleDialog = true }) {
                                 Icon(
                                     imageVector = Icons.Default.Subtitles,
                                     contentDescription = "Subtitles",
                                     tint = Color.White
                                 )
                             }
                             
                             // Lock Button
                             IconButton(onClick = { isLocked = !isLocked }) {
                                 Icon(
                                     imageVector = if (isLocked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                                     contentDescription = "Lock",
                                     tint = if (isLocked) MaterialTheme.colorScheme.primary else Color.White
                                 )
                             }
                        }

                        }

                    if (!isLocked) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { engine.seekBack() }) {
                                Icon(
                                    modifier = Modifier.size(48.dp),
                                    imageVector = Icons.Default.Replay10,
                                    contentDescription = "Replay 10 seconds",
                                    tint = Color.White
                                )
                            }

                            IconButton(onClick = { if (isPlaying) engine.pause() else engine.play() }) {
                                Icon(
                                    modifier = Modifier.size(64.dp),
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = Color.White
                                )
                            }

                            IconButton(onClick = { engine.seekForward() }) {
                                Icon(
                                    modifier = Modifier.size(48.dp),
                                    imageVector = Icons.Default.Forward10,
                                    contentDescription = "Forward 10 seconds",
                                    tint = Color.White
                                )
                            }
                        }

                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(text = formatDuration(sliderValue.toLong()), color = Color.White)
                                if (isBuffering) {
                                    LinearProgressIndicator(modifier = Modifier.weight(1f))
                                } else {
                                    androidx.compose.foundation.layout.BoxWithConstraints(modifier = Modifier.weight(1f)) {
                                        val sliderWidth = maxWidth
                                        
                                        Slider(
                                            value = sliderValue,
                                            onValueChange = {
                                                isSeeking = true
                                                sliderValue = it
                                            },
                                            onValueChangeFinished = {
                                                engine.seekTo(sliderValue.toLong())
                                                kotlinx.coroutines.GlobalScope.launch {
                                                    kotlinx.coroutines.delay(50)
                                                    isSeeking = false
                                                }
                                            },
                                            valueRange = 0f..duration.toFloat().coerceAtLeast(0f),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        
                                        // Seek Preview Bubble
                                        if (isSeeking) {
                                            val progress = sliderValue / duration.toFloat().coerceAtLeast(1f)
                                            val offsetX = sliderWidth * progress
                                            
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopStart)
                                                    .padding(bottom = 30.dp)
                                                    .offset(x = offsetX - 20.dp) // Center the bubble
                                                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                                                    .padding(4.dp)
                                            ) {
                                                Text(
                                                    text = formatDuration(sliderValue.toLong()),
                                                    color = MaterialTheme.colorScheme.onPrimary,
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            }
                                        }
                                    }
                                }
                                Text(text = formatDuration(duration), color = Color.White)
                            }
                        }
                    } else {
                        // Locked State Indicator
                        Box(modifier = Modifier.align(Alignment.Center)) {
                             Icon(
                                 imageVector = Icons.Filled.Lock,
                                 contentDescription = "Locked",
                                 tint = Color.White.copy(alpha = 0.5f),
                                 modifier = Modifier.size(64.dp)
                             )
                        }
                    }
                }
            }
            
            // Stats Overlay
            if (showStats) {
                StatsOverlay(engine)
            }

            // Gesture Indicator
            AnimatedVisibility(
                visible = showGestureIndicator,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Box(
                    modifier = Modifier
                        .clip(com.hasanege.materialtv.ui.theme.ExpressiveShapes.Medium)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        gestureIcon?.let {
                            Icon(
                                imageVector = it,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${gestureValue.toInt()}%",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        
        // Track Selection Dialog
        if (showTrackSelectionDialog) {
            TrackSelectionDialog(
                engine = engine,
                onDismiss = { showTrackSelectionDialog = false }
            )
        }
        
        // Error message overlay
        errorMessage?.let { message ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = message,
                    color = Color.White,
                    fontSize = 18.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
        
        // Audio Track Dialog
        if (showAudioDialog) {
            AlertDialog(
                onDismissRequest = { showAudioDialog = false },
                title = { Text(stringResource(R.string.player_audio_tracks)) },
                text = {
                    Column {
                        val tracks = engine.getAudioTracks()
                        val currentTrackId = engine.getCurrentAudioTrack()
                        
                        if (tracks.isEmpty()) {
                            Text(stringResource(R.string.player_no_audio_tracks))
                        } else {
                            tracks.forEach { (id, label) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            engine.setAudioTrack(id)
                                            showAudioDialog = false
                                        }
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = (id == currentTrackId),
                                        onClick = {
                                            engine.setAudioTrack(id)
                                            showAudioDialog = false
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(label)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAudioDialog = false }) {
                        Text(stringResource(R.string.player_close))
                    }
                }
            )
        }
        
        // Subtitle Dialog  
        if (showSubtitleDialog) {
            AlertDialog(
                onDismissRequest = { showSubtitleDialog = false },
                title = { Text(stringResource(R.string.player_subtitles)) },
                text = {
                    Column {
                        val tracks = engine.getSubtitleTracks()
                        val currentTrackId = engine.getCurrentSubtitleTrack()
                        
                        // Add "None" option
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    engine.setSubtitleTrack(-1)
                                    showSubtitleDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (currentTrackId == -1),
                                onClick = {
                                    engine.setSubtitleTrack(-1)
                                    showSubtitleDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.player_none))
                        }
                        
                        if (tracks.isNotEmpty()) {
                            tracks.forEach { (id, label) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            engine.setSubtitleTrack(id)
                                            showSubtitleDialog = false
                                        }
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = (id == currentTrackId),
                                        onClick = {
                                            engine.setSubtitleTrack(id)
                                            showSubtitleDialog = false
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(label)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSubtitleDialog = false }) {
                        Text(stringResource(R.string.player_close))
                    }
                }
            )
        }
    }
}

enum class DoubleTapState {
    Rewind, Forward
}

/**
 * PlayerControlsOverlay - Renders only the player controls without the video surface.
 * Used with XML-based video container for better PiP support.
 */
@UnstableApi
@Composable
fun PlayerControlsOverlay(
    engine: PlayerEngine,
    title: String?,
    showStats: Boolean,
    inPipMode: Boolean = false,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSwitchEngine: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as Activity
    val window = activity.window

    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var controlsVisible by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isBuffering by remember { mutableStateOf(false) }

    // State
    var resizeMode by remember { mutableStateOf(androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    var isLocked by remember { mutableStateOf(false) }
    var doubleTapState by remember { mutableStateOf<DoubleTapState?>(null) }

    // Slider state
    var isSeeking by remember { mutableStateOf(false) }
    var sliderValue by remember { mutableFloatStateOf(0f) }
    var showAudioDialog by remember { mutableStateOf(false) }
    var showSubtitleDialog by remember { mutableStateOf(false) }

    // Gesture control states
    var showGestureIndicator by remember { mutableStateOf(false) }
    var gestureIcon by remember { mutableStateOf<ImageVector?>(null) }
    var gestureValue by remember { mutableFloatStateOf(0f) }
    var currentVolume by remember { mutableFloatStateOf(0f) }
    var currentBrightness by remember { mutableFloatStateOf(0.5f) }

    LaunchedEffect(engine) {
        while(true) {
            isPlaying = engine.isPlaying()
            duration = engine.getDuration()
            currentPosition = engine.getCurrentPosition()
            
            if (!isSeeking) {
                sliderValue = currentPosition.toFloat()
            }
            
            delay(500)
        }
    }

    LaunchedEffect(controlsVisible) {
        if (controlsVisible) {
            delay(5000L)
            controlsVisible = false
        }
    }

    LaunchedEffect(showGestureIndicator) {
        if (showGestureIndicator) {
            delay(1000L)
            showGestureIndicator = false
        }
    }
    
    LaunchedEffect(doubleTapState) {
        if (doubleTapState != null) {
            delay(600L)
            doubleTapState = null
        }
    }

    // Hide controls in PiP mode
    if (inPipMode) {
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(isLocked) {
                if (!isLocked) {
                    detectTapGestures(
                        onTap = { controlsVisible = !controlsVisible },
                        onDoubleTap = { offset ->
                            val screenWidth = size.width
                            if (offset.x < screenWidth / 2) {
                                engine.seekTo((engine.getCurrentPosition() - 10000).coerceAtLeast(0))
                                doubleTapState = DoubleTapState.Rewind
                            } else {
                                engine.seekTo((engine.getCurrentPosition() + 10000).coerceAtMost(engine.getDuration()))
                                doubleTapState = DoubleTapState.Forward
                            }
                        }
                    )
                }
            }
            .pointerInput(isLocked) {
                if (!isLocked) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val screenWidth = size.width
                        val isLeftSide = change.position.x < screenWidth / 2
                        
                        showGestureIndicator = true
                        
                        if (isLeftSide) {
                            // Brightness control
                            currentBrightness = (currentBrightness - (dragAmount.y / 500f)).coerceIn(0.01f, 1f)
                            window.attributes = window.attributes.apply { screenBrightness = currentBrightness }
                            gestureIcon = Icons.Default.BrightnessMedium
                            gestureValue = currentBrightness * 100
                        } else {
                            // Volume control
                            currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVolume
                            currentVolume = (currentVolume - (dragAmount.y / 500f)).coerceIn(0f, 1f)
                            audioManager.setStreamVolume(
                                AudioManager.STREAM_MUSIC,
                                (currentVolume * maxVolume).toInt(),
                                0
                            )
                            gestureIcon = Icons.AutoMirrored.Filled.VolumeUp
                            gestureValue = currentVolume * 100
                        }
                    }
                }
            }
    ) {
        // Double Tap Animation Overlay
        doubleTapState?.let { state ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.2f)),
                contentAlignment = if (state == DoubleTapState.Rewind) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                Column(
                    modifier = Modifier.padding(50.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = if (state == DoubleTapState.Rewind) Icons.Default.Replay10 else Icons.Default.Forward10,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(50.dp)
                    )
                    Text(stringResource(R.string.player_10s), color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.background(Color.Black.copy(alpha = 0.4f))) {
                
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopStart)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    title?.let {
                        Text(
                            text = it,
                            color = Color.White,
                            fontSize = 18.sp,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    Row {
                        IconButton(onClick = { showAudioDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = "Audio Track",
                                tint = Color.White
                            )
                        }
                        
                        IconButton(onClick = { showSubtitleDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Subtitles,
                                contentDescription = "Subtitles",
                                tint = Color.White
                            )
                        }
                        
                        IconButton(onClick = { onSwitchEngine() }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Switch Engine",
                                tint = Color.White
                            )
                        }
                        
                        IconButton(onClick = { isLocked = !isLocked }) {
                            Icon(
                                imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = "Lock/Unlock",
                                tint = Color.White
                            )
                        }
                    }
                }

                // Center Controls
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(48.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { onPrevious() }, modifier = Modifier.size(56.dp)) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    IconButton(onClick = { engine.seekTo(engine.getCurrentPosition() - 10000) }, modifier = Modifier.size(56.dp)) {
                        Icon(
                            imageVector = Icons.Default.Replay10,
                            contentDescription = "Replay 10s",
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                            .clickable { if (isPlaying) engine.pause() else engine.play() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isBuffering) {
                            CircularProgressIndicator(color = Color.White)
                        } else {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }

                    IconButton(onClick = { engine.seekTo(engine.getCurrentPosition() + 10000) }, modifier = Modifier.size(56.dp)) {
                        Icon(
                            imageVector = Icons.Default.Forward10,
                            contentDescription = "Forward 10s",
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    IconButton(onClick = { onNext() }, modifier = Modifier.size(56.dp)) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next",
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                // Bottom Controls
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Progress Slider
                    if (duration > 0) {
                        Slider(
                            value = sliderValue,
                            onValueChange = { 
                                isSeeking = true
                                sliderValue = it 
                            },
                            onValueChangeFinished = {
                                engine.seekTo(sliderValue.toLong())
                                isSeeking = false
                            },
                            valueRange = 0f..duration.toFloat(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatDuration(currentPosition),
                            color = Color.White,
                            fontSize = 12.sp
                        )
                        Text(
                            text = formatDuration(duration),
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                }

                // Gesture Indicator
                if (showGestureIndicator) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.7f))
                            .padding(16.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            gestureIcon?.let {
                                Icon(
                                    imageVector = it,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Text(
                                text = "${gestureValue.toInt()}%",
                                color = Color.White,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
                
                // Stats for Nerds
                if (showStats) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(8.dp)
                    ) {
                        Text(stringResource(R.string.player_engine_label, engine.javaClass.simpleName), color = Color.White, fontSize = 10.sp)
                        Text(stringResource(R.string.player_position_label, formatDuration(currentPosition), formatDuration(duration)), color = Color.White, fontSize = 10.sp)
                    }
                }
            }
        }
        
        // Audio Track Dialog
        if (showAudioDialog) {
            AlertDialog(
                onDismissRequest = { showAudioDialog = false },
                title = { Text(stringResource(R.string.player_audio_tracks)) },
                text = {
                    Column {
                        val tracks = engine.getAudioTracks()
                        val currentTrackId = engine.getCurrentAudioTrack()
                        
                        if (tracks.isEmpty()) {
                            Text(stringResource(R.string.player_no_audio_tracks))
                        } else {
                            tracks.forEach { (id, label) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            engine.setAudioTrack(id)
                                            showAudioDialog = false
                                        }
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = (id == currentTrackId),
                                        onClick = {
                                            engine.setAudioTrack(id)
                                            showAudioDialog = false
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(label)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAudioDialog = false }) {
                        Text(stringResource(R.string.player_close))
                    }
                }
            )
        }
        
        // Subtitle Dialog
        if (showSubtitleDialog) {
            AlertDialog(
                onDismissRequest = { showSubtitleDialog = false },
                title = { Text(stringResource(R.string.player_subtitles)) },
                text = {
                    Column {
                        val tracks = engine.getSubtitleTracks()
                        val currentTrackId = engine.getCurrentSubtitleTrack()
                        
                        if (tracks.isEmpty()) {
                            Text(stringResource(R.string.player_no_subtitles))
                        } else {
                            tracks.forEach { (id, label) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            engine.setSubtitleTrack(id)
                                            showSubtitleDialog = false
                                        }
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = (id == currentTrackId),
                                        onClick = {
                                            engine.setSubtitleTrack(id)
                                            showSubtitleDialog = false
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(label)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSubtitleDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }
    }
}

@Composable
private fun TrackSelectionDialog(
    engine: PlayerEngine,
    onDismiss: () -> Unit
) {
    val audioTracks = remember(engine) {
        engine.getAudioTracks()
    }

    val subtitleTracks = remember(engine) {
        engine.getSubtitleTracks()
    }
    
    val currentAudioTrack = remember(engine) {
        engine.getCurrentAudioTrack()
    }
    
    val currentSubtitleTrack = remember(engine) {
        engine.getCurrentSubtitleTrack()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.player_audio_subtitle_selection)) },
        text = {
            // Yatay düzen için Row kullanıyoruz
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Audio Tracks Section (Sol taraf)
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stringResource(R.string.tracks_audio),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    if (audioTracks.isEmpty()) {
                        Text(
                            text = stringResource(R.string.tracks_no_tracks),
                            color = Color.Gray,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        LazyColumn(modifier = Modifier.height(200.dp)) {
                            items(audioTracks) { (trackId, label) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            engine.setAudioTrack(trackId)
                                            onDismiss()
                                        }
                                        .padding(vertical = 8.dp, horizontal = 4.dp)
                                        .background(
                                            if (trackId == currentAudioTrack) 
                                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                            else 
                                                Color.Transparent,
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 14.sp,
                                        fontWeight = if (trackId == currentAudioTrack) FontWeight.Bold else FontWeight.Normal,
                                        color = if (trackId == currentAudioTrack) 
                                            MaterialTheme.colorScheme.primary 
                                        else 
                                            MaterialTheme.colorScheme.onSurface,
                                        maxLines = 2
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Subtitle Tracks Section (Sağ taraf)
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stringResource(R.string.tracks_subtitles),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    if (subtitleTracks.isEmpty()) {
                        Text(
                            text = stringResource(R.string.tracks_no_tracks),
                            color = Color.Gray,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        LazyColumn(modifier = Modifier.height(200.dp)) {
                            items(subtitleTracks) { (trackId, label) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            engine.setSubtitleTrack(trackId)
                                            onDismiss()
                                        }
                                        .padding(vertical = 8.dp, horizontal = 4.dp)
                                        .background(
                                            if (trackId == currentSubtitleTrack) 
                                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                            else 
                                                Color.Transparent,
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 14.sp,
                                        fontWeight = if (trackId == currentSubtitleTrack) FontWeight.Bold else FontWeight.Normal,
                                        color = if (trackId == currentSubtitleTrack) 
                                            MaterialTheme.colorScheme.primary 
                                        else 
                                            MaterialTheme.colorScheme.onSurface,
                                        maxLines = 2
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.player_close))
            }
        }
    )
}

@Composable
fun StatsOverlay(engine: PlayerEngine) {
    var bitrate by remember { mutableStateOf("N/A") }
    var droppedFrames by remember { mutableStateOf("N/A") }
    var videoFormat by remember { mutableStateOf("N/A") }

    LaunchedEffect(engine) {
        while (true) {
            try {
                val bitrateValue = engine.getBitrate()
                bitrate = if (bitrateValue > 0) "${bitrateValue / 1000} kbps" else "N/A"
                
                val droppedValue = engine.getDroppedFrames()
                droppedFrames = droppedValue.toString()
                
                videoFormat = engine.getVideoFormat() ?: "N/A"
            } catch (e: Exception) {
                android.util.Log.e("StatsOverlay", "Error getting stats: ${e.message}")
            }
            delay(1000)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.TopEnd
    ) {
        Column(
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            Text(
                text = "Bitrate: $bitrate",
                color = Color.White,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "Dropped: $droppedFrames",
                color = Color.White,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "Format: $videoFormat",
                color = Color.White,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

