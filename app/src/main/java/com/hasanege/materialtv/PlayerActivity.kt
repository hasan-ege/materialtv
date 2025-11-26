
package com.hasanege.materialtv

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.os.Bundle
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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

    private val detailViewModel: DetailViewModel by viewModels { DetailViewModelFactory }
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
    
    // Track actual watch time (excluding seeking/skipping)
    private var sessionStartTime: Long = 0L
    private var lastPosition: Long = 0L
    private var actualWatchTime: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Disable activity transition animations for VLC, ExoPlayer, and Hybrid modes
        overridePendingTransition(0, 0)
        
        // Keep screen on during playback
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val streamId = intent.getIntExtra("STREAM_ID", -1)
        seriesId = intent.getIntExtra("SERIES_ID", -1)
        val episodeId = intent.getStringExtra("EPISODE_ID")
        this.title = intent.getStringExtra("TITLE")
        val position = intent.getLongExtra("position", 0L)
        val liveUrl = intent.getStringExtra("url")
        val uri = intent.getStringExtra("URI")
        
        // Check if this is a live stream
        isLiveStream = liveUrl != null && streamId == -1 && seriesId == -1
        if (isLiveStream) {
            liveStreamId = intent.getIntExtra("LIVE_STREAM_ID", -1)
            liveStreamName = this.title ?: "Live Stream"
        }

        // Read default player synchronously using singleton
        val settingsRepository = com.hasanege.materialtv.data.SettingsRepository.getInstance(this)
        var useVlcForDownloads = true
        runBlocking {
            val player = settingsRepository.defaultPlayer.first()
            // Check if we're being forced to use VLC due to ExoPlayer failure
            val forceVlc = intent.getBooleanExtra("forceVlc", false)
            isVlc = forceVlc || (player == "VLC")
            statsForNerds = settingsRepository.statsForNerds.first()
            useVlcForDownloads = settingsRepository.useVlcForDownloads.first()
        }

        // Indirilmis icerik (yerel dosya) aciliyorsa, ayara gore VLC zorla
        if (uri != null && useVlcForDownloads) {
            isVlc = true
        }

        if (uri != null) {
            initializePlayer(uri, position)
            setContent {
                MaterialTVTheme {
                    playerEngine?.let {
                        FullscreenPlayer(
                            engine = it, 
                            title = this.title, 
                            showStats = statsForNerds,
                            onNext = {}, 
                            onPrevious = {}, 
                            onSwitchEngine = { switchEngine() }
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
                    playerEngine?.let {
                        FullscreenPlayer(
                            engine = it, 
                            title = this.title,
                            showStats = statsForNerds,
                            onNext = {}, 
                            onPrevious = {}, 
                            onSwitchEngine = { switchEngine() }
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
                val movieState = detailViewModel.movie
                val seriesState = detailViewModel.series
                var hasPlayed by remember { mutableStateOf(false) }

                if (hasPlayed) {
                    playerEngine?.let {
                        FullscreenPlayer(
                            engine = it,
                            title = this.title,
                            showStats = statsForNerds,
                            onNext = { playNextEpisode() },
                            onPrevious = { playPreviousEpisode() },
                            onSwitchEngine = { switchEngine() }
                        )
                    }
                    BackHandler {
                        savePlaybackPosition()
                        hasPlayed = false
                        playerEngine?.release()
                        playerEngine = null
                    }
                } else {
                    when {
                        movieState is UiState.Success -> {
                            DetailScreen(
                                movie = movieState.data,
                                onBack = { finish() },
                                onPlayMovie = { movie ->
                                    this.currentMovie = movie
                                    this.currentSeriesEpisode = null
                                    playerEngine?.release()
                                    val historyItem = WatchHistoryManager.getHistory()
                                        .find { it.streamId == movie.streamId }
                                    val startPosition = historyItem?.position ?: position
                                    initializePlayer(movieUrl(movie), startPosition)
                                    hasPlayed = true
                                }
                            )
                        }

                        seriesState is UiState.Success -> {
                            DetailScreen(
                                series = seriesState.data,
                                onBack = { finish() },
                                onPlayEpisode = { episode ->
                                    this.currentMovie = null
                                    this.currentSeriesEpisode = episode
                                    this.title = episode.title
                                    playerEngine?.release()
                                    val historyItem = WatchHistoryManager.getHistory()
                                        .find { it.streamId.toString() == episode.id }
                                    val startPosition = historyItem?.position ?: position
                                    initializePlayer(episodeUrl(episode), startPosition)
                                    hasPlayed = true
                                },
                                seriesId = seriesId
                            )
                        }

                        else -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black),
                                contentAlignment = Alignment.Center
                            ) {
                                when {
                                    movieState is UiState.Loading || seriesState is UiState.Loading -> CircularProgressIndicator()
                                    movieState is UiState.Error -> Text(
                                        movieState.message,
                                        color = Color.White
                                    )

                                    seriesState is UiState.Error -> Text(
                                        seriesState.message,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }


    }

    private fun initializePlayer(url: String, position: Long) {
        currentUrl = url
        playerEngine?.release()

        val newEngine = if (isVlc) LibVlcEngine() else ExoPlayerEngine()

        newEngine.apply {
            initialize(this@PlayerActivity)
            setOnErrorCallback { error ->
                 if (!isVlc) {
                     lifecycleScope.launch {
                         val settingsRepo = com.hasanege.materialtv.data.SettingsRepository.getInstance(this@PlayerActivity)
                         val pref = settingsRepo.defaultPlayerPreference.first()
                         if (pref == com.hasanege.materialtv.data.PlayerPreference.HYBRID) {
                             val currentPos = getCurrentPosition()
                             // Recreate the activity with VLC forced
                             finish()
                             startActivity(intent.apply {
                                 putExtra("URI", url)
                                 putExtra("position", currentPos)
                                 putExtra("forceVlc", true) // Force VLC to prevent loop
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
            
            prepare(url)
            if (position > 0) seekTo(position)
            play()
        }
        playerEngine = newEngine
        
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
        val episodesMap = json.decodeFromJsonElement<Map<String, List<Episode>>>(seriesData.episodes!!)
        val allEpisodes = episodesMap.values.flatten()
        val currentIndex = allEpisodes.indexOf(currentSeriesEpisode)
        if (currentIndex < allEpisodes.size - 1) {
            val nextEpisode = allEpisodes[currentIndex + 1]
            playEpisode(nextEpisode)
        }
    }

    private fun playPreviousEpisode() {
        val seriesData = (detailViewModel.series as? UiState.Success)?.data ?: return
        val episodesMap = json.decodeFromJsonElement<Map<String, List<Episode>>>(seriesData.episodes!!)
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
            savePlaybackPosition()
            playerEngine?.pause()
        } catch (e: Exception) {
            android.util.Log.e("PlayerActivity", "Error in onPause: ${e.message}")
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            if (playerEngine == null && currentUrl != null && !isFinishing) {
                initializePlayer(currentUrl!!, lastPlaybackPosition)
            } else {
                playerEngine?.play()
            }
        } catch (e: Exception) {
            android.util.Log.e("PlayerActivity", "Error in onResume: ${e.message}")
        }
    }

    override fun onStop() {
        super.onStop()
        try {
            // Only save position, don't release player yet
            // This allows resuming if user comes back
            savePlaybackPosition()
            playerEngine?.pause()
        } catch (e: Exception) {
            android.util.Log.e("PlayerActivity", "Error in onStop: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
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
        try {
            playerEngine?.let { player ->
            val position = player.getCurrentPosition()
            lastPlaybackPosition = position
            val duration = player.getDuration()

            // Calculate actual watch time for this session
            val sessionWatchTime = calculateActualWatchTime(position)
            actualWatchTime += sessionWatchTime

            // Save if played for more than a second
            if (position > 1000) {
                // Save live stream watch time
                if (isLiveStream && liveStreamId > 0) {
                    val liveItem = ContinueWatchingItem(
                        streamId = liveStreamId,
                        name = liveStreamName ?: "Live Stream",
                        streamIcon = null,
                        duration = 0, // Live streams have no duration
                        position = position,
                        type = "live",
                        seriesId = null,
                        episodeId = null
                    )
                    WatchHistoryManager.saveItemWithWatchTime(liveItem, actualWatchTime)
                }
                // Save movie/series watch time
                else if (duration <= 0 || (position.toFloat() / duration.toFloat()) < 0.95f) {
                    currentMovie?.let {
                        val item = ContinueWatchingItem(
                            streamId = it.streamId ?: 0,
                            name = it.name ?: "",
                            streamIcon = it.streamIcon,
                            duration = duration,
                            position = position,
                            type = if (it.seriesId != null) "series" else "movie",
                            seriesId = it.seriesId,
                            episodeId = null
                        )
                        WatchHistoryManager.saveItemWithWatchTime(item, actualWatchTime)
                    }
                    currentSeriesEpisode?.let { ep ->
                        val seriesInfo = (detailViewModel.series as? UiState.Success)?.data?.info
                        val item = ContinueWatchingItem(
                            streamId = ep.id.toIntOrNull() ?: 0,
                            name = seriesInfo?.name ?: ep.title ?: "",
                            streamIcon = seriesInfo?.cover,
                            duration = duration,
                            position = position,
                            type = "series",
                            seriesId = this.seriesId,
                            episodeId = ep.id
                        )
                        WatchHistoryManager.saveItemWithWatchTime(item, actualWatchTime)
                    }
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

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                engine.pause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

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
                update = { 
                    // No update needed
                },
                modifier = Modifier.fillMaxSize()
            )
            
            DisposableEffect(engine) {
                onDispose { engine.detach() }
            }
            
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
                         Text("10s", color = Color.White, fontWeight = FontWeight.Bold)
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
                             // Switch Engine Button
                             IconButton(onClick = onSwitchEngine) {
                                 Icon(
                                     imageVector = Icons.Default.Refresh,
                                     contentDescription = "Switch Engine",
                                     tint = Color.White
                                 )
                             }

                             // Resize Mode Button
                             if (!isLocked) {
                                 IconButton(onClick = {
                                     resizeMode = when (resizeMode) {
                                         androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL
                                         androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                         else -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                                     }
                                 }) {
                                     Icon(
                                         imageVector = Icons.Filled.AspectRatio,
                                         contentDescription = "Resize",
                                         tint = Color.White
                                     )
                                 }
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
                                    Slider(
                                        value = sliderValue,
                                        onValueChange = {
                                            isSeeking = true
                                            sliderValue = it
                                        },
                                        onValueChangeFinished = {
                                            // Seek and reset flag
                                            engine.seekTo(sliderValue.toLong())
                                            // Reset seeking flag after a short delay to prevent conflicts
                                            kotlinx.coroutines.GlobalScope.launch {
                                                kotlinx.coroutines.delay(50)
                                                isSeeking = false
                                            }
                                        },
                                        valueRange = 0f..duration.toFloat().coerceAtLeast(0f),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Text(text = formatDuration(duration), color = Color.White)
                                IconButton(onClick = { showTrackSelectionDialog = true }) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Settings",
                                        tint = Color.White
                                    )
                                }
                                IconButton(
                                    onClick = onPrevious,
                                    modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SkipPrevious,
                                        contentDescription = "Previous Episode",
                                        tint = Color.White
                                    )
                                }
                                IconButton(
                                    onClick = onNext,
                                    modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SkipNext,
                                        contentDescription = "Next Episode",
                                        tint = Color.White
                                    )
                                }
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
                        .clip(RoundedCornerShape(16.dp))
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
        
        // Error Message Display
        errorMessage?.let {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.75f))
                    .clickable { errorMessage = null },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "An error occurred: $it",
                    color = Color.White,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

enum class DoubleTapState {
    Rewind, Forward
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
        title = { Text("Audio & Subtitle Selection") },
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
                        text = "Audio",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    if (audioTracks.isEmpty()) {
                        Text(
                            text = "No tracks",
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
                        text = "Subtitles",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    if (subtitleTracks.isEmpty()) {
                        Text(
                            text = "No tracks",
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
                Text("Close")
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

