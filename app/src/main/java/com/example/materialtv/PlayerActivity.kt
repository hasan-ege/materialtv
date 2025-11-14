
package com.example.materialtv

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
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.materialtv.model.ContinueWatchingItem
import com.example.materialtv.model.Episode
import com.example.materialtv.model.VodItem
import com.example.materialtv.network.SessionManager
import com.example.materialtv.ui.theme.MaterialTVTheme
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs

private val json = Json {
    ignoreUnknownKeys = true
}

@UnstableApi
class PlayerActivity : ComponentActivity() {

    private val detailViewModel: DetailViewModel by viewModels { DetailViewModelFactory }
    private var exoPlayer: ExoPlayer? = null
    private var currentMovie: VodItem? = null
    private var currentSeriesEpisode: Episode? = null
    private var seriesId: Int = -1
    private var title by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val streamId = intent.getIntExtra("STREAM_ID", -1)
        seriesId = intent.getIntExtra("SERIES_ID", -1)
        val episodeId = intent.getStringExtra("EPISODE_ID")
        this.title = intent.getStringExtra("TITLE")
        val position = intent.getLongExtra("position", 0L)
        val liveUrl = intent.getStringExtra("url")
        val uri = intent.getStringExtra("URI")

        if (uri != null) {
            exoPlayer = buildExoPlayer(uri, position)
            setContent {
                MaterialTVTheme {
                    exoPlayer?.let {
                        FullscreenPlayer(player = it, title = this.title, onNext = {}, onPrevious = {})
                        BackHandler {
                            finish()
                        }
                    }
                }
            }
            return
        }

        if (liveUrl != null) {
            exoPlayer = buildExoPlayer(liveUrl, position)
            setContent {
                MaterialTVTheme {
                    exoPlayer?.let {
                        FullscreenPlayer(player = it, title = this.title, onNext = {}, onPrevious = {})
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
                    exoPlayer?.let {
                        FullscreenPlayer(
                            player = it,
                            title = this.title,
                            onNext = { playNextEpisode() },
                            onPrevious = { playPreviousEpisode() }
                        )
                    }
                    BackHandler {
                        savePlaybackPosition()
                        hasPlayed = false
                        exoPlayer?.release()
                        exoPlayer = null
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
                                    exoPlayer?.release()
                                    val historyItem = WatchHistoryManager.getHistory()
                                        .find { it.streamId == movie.streamId }
                                    val startPosition = historyItem?.position ?: position
                                    val subtitleUrl = movieSubtitleUrl(movie)
                                    exoPlayer =
                                        buildExoPlayer(movieUrl(movie), startPosition, subtitleUrl)
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
                                    exoPlayer?.release()
                                    val historyItem = WatchHistoryManager.getHistory()
                                        .find { it.streamId.toString() == episode.id }
                                    val startPosition = historyItem?.position ?: position
                                    val subtitleUrl = episodeSubtitleUrl(episode)
                                    exoPlayer =
                                        buildExoPlayer(episodeUrl(episode), startPosition, subtitleUrl)
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

        exoPlayer?.let { player ->
            val historyItem = WatchHistoryManager.getHistory()
                .find { it.streamId.toString() == episode.id }
            val startPosition = historyItem?.position ?: 0L
            val subtitleUrl = episodeSubtitleUrl(episode)
            val mediaItem = MediaItem.Builder()
                .setUri(episodeUrl(episode))
                .apply {
                    val subtitle = MediaItem.SubtitleConfiguration.Builder(subtitleUrl.toUri())
                        .setMimeType(MimeTypes.APPLICATION_SUBRIP)
                        .setLanguage("en")
                        .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                        .build()
                    setSubtitleConfigurations(listOf(subtitle))
                }
                .build()

            player.setMediaItem(mediaItem, startPosition)
            player.prepare()
            player.playWhenReady = true
        }
    }


    private fun movieUrl(movie: VodItem): String {
        val extension = movie.containerExtension ?: "mp4"
        return "${SessionManager.serverUrl}/movie/${SessionManager.username}/${SessionManager.password}/${movie.streamId}.$extension"
    }

    private fun episodeUrl(episode: Episode): String {
        val extension = episode.containerExtension ?: "mkv"
        return "${SessionManager.serverUrl}/series/${SessionManager.username}/${SessionManager.password}/${episode.id}.$extension"
    }

    private fun movieSubtitleUrl(movie: VodItem): String {
        return "${SessionManager.serverUrl}/movie/${SessionManager.username}/${SessionManager.password}/${movie.streamId}.srt"
    }

    private fun episodeSubtitleUrl(episode: Episode): String {
        return "${SessionManager.serverUrl}/series/${SessionManager.username}/${SessionManager.password}/${episode.id}.srt"
    }

    private fun buildExoPlayer(
        url: String,
        startPos: Long,
        subtitleUrl: String? = null
    ): ExoPlayer {
        val context = this
        val renderersFactory = DefaultRenderersFactory(context).apply {
            setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
            setEnableDecoderFallback(true)
        }

        val trackSelector = DefaultTrackSelector(context).apply {
            setParameters(buildUponParameters().setAllowAudioMixedMimeTypeAdaptiveness(true))
        }

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        val mediaItem = MediaItem.Builder()
            .setUri(url)
            .apply {
                subtitleUrl?.let {
                    val subtitle = MediaItem.SubtitleConfiguration.Builder(it.toUri())
                        .setMimeType(MimeTypes.APPLICATION_SUBRIP)
                        .setLanguage("en")
                        .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                        .build()
                    setSubtitleConfigurations(listOf(subtitle))
                }
            }
            .build()

        return ExoPlayer.Builder(context, renderersFactory)
            .setTrackSelector(trackSelector)
            .setAudioAttributes(audioAttributes, true)
            .build().apply {
                setMediaItem(mediaItem, startPos)
                prepare()
                playWhenReady = true
            }
    }

    override fun onPause() {
        super.onPause()
        savePlaybackPosition()
        exoPlayer?.pause()
    }

    override fun onResume() {
        super.onResume()
        exoPlayer?.play()
    }

    override fun onDestroy() {
        super.onDestroy()
        savePlaybackPosition()
        exoPlayer?.release()
    }

    private fun savePlaybackPosition() {
        exoPlayer?.let { player ->
            val position = player.currentPosition
            val duration = player.duration

            // Save if played for more than a second and not finished
            if (position > 1000) {
                if (duration <= 0 || (position.toFloat() / duration.toFloat()) < 0.95f) {
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
                        WatchHistoryManager.saveItem(item)
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
                        WatchHistoryManager.saveItem(item)
                    }
                }
            }
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
    player: Player,
    title: String?,
    onNext: () -> Unit,
    onPrevious: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as Activity
    val window = activity.window

    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

    var isPlaying by remember { mutableStateOf(player.isPlaying) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var controlsVisible by remember { mutableStateOf(true) }
    var showTrackSelectionDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isBuffering by remember { mutableStateOf(false) }

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


    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlayingValue: Boolean) {
                isPlaying = isPlayingValue
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    duration = player.duration
                }
                isBuffering = playbackState == Player.STATE_BUFFERING
            }

            override fun onPlayerError(error: PlaybackException) {
                super.onPlayerError(error)
                errorMessage = error.message
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
        }
    }

    LaunchedEffect(isPlaying, isSeeking) {
        if (!isSeeking) {
            while (isPlaying) {
                currentPosition = player.currentPosition
                sliderValue = currentPosition.toFloat()
                delay(1000L)
            }
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

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                player.pause()
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
            .clickable { controlsVisible = !controlsVisible }
    ) {
        Box(modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
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
            })
        {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = player
                        useController = false
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            AnimatedVisibility(
                visible = controlsVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(modifier = Modifier.background(Color.Black.copy(alpha = 0.4f))) {
                    title?.let {
                        Text(
                            text = it,
                            color = Color.White,
                            fontSize = 18.sp,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(16.dp)
                        )
                    }
                    Row(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { player.seekBack() }) {
                            Icon(
                                modifier = Modifier.size(48.dp),
                                imageVector = Icons.Default.Replay10,
                                contentDescription = "Replay 10 seconds",
                                tint = Color.White
                            )
                        }

                        IconButton(onClick = { if (player.isPlaying) player.pause() else player.play() }) {
                            Icon(
                                modifier = Modifier.size(64.dp),
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = Color.White
                            )
                        }

                        IconButton(onClick = { player.seekForward() }) {
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
                                        player.seekTo(sliderValue.toLong())
                                        isSeeking = false
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
                }
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
        if (showTrackSelectionDialog) {
            (player as? ExoPlayer)?.let { exoPlayer ->
                (exoPlayer.trackSelector as? DefaultTrackSelector)?.let {
                    TrackSelectionDialog(
                        trackSelector = it,
                    ) { showTrackSelectionDialog = false }
                }
            }
        }
        errorMessage?.let {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.75f))
                    .clickable { errorMessage = null }, // dismiss on click
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

@UnstableApi
@Composable
private fun TrackSelectionDialog(
    trackSelector: DefaultTrackSelector,
    onDismiss: () -> Unit
) {
    val mappedTrackInfo = trackSelector.currentMappedTrackInfo ?: return

    val audioTracks = remember {
        val tracks = mutableListOf<Pair<String, DefaultTrackSelector.Parameters>>()
        for (i in 0 until mappedTrackInfo.rendererCount) {
            if (mappedTrackInfo.getRendererType(i) == C.TRACK_TYPE_AUDIO) {
                val trackGroupArray = mappedTrackInfo.getTrackGroups(i)
                for (j in 0 until trackGroupArray.length) {
                    val trackGroup = trackGroupArray.get(j)
                    for (k in 0 until trackGroup.length) {
                        val format = trackGroup.getFormat(k)
                        val language =
                            format.language?.let { Locale.forLanguageTag(it).displayLanguage } ?: "Unknown"
                        val label = format.label ?: "$language, ${format.sampleRate}Hz"
                        val parameters = trackSelector.buildUponParameters()
                            .setRendererDisabled(i, false)
                            .setSelectionOverride(
                                i,
                                trackGroupArray,
                                DefaultTrackSelector.SelectionOverride(j, k)
                            )
                        tracks.add(label to parameters.build())
                    }
                }
            }
        }
        tracks
    }

    val subtitleTracks = remember {
        val tracks = mutableListOf<Pair<String, DefaultTrackSelector.Parameters>>()
        var textRendererIndex = -1
        for (i in 0 until mappedTrackInfo.rendererCount) {
            if (mappedTrackInfo.getRendererType(i) == C.TRACK_TYPE_TEXT) {
                textRendererIndex = i
                break
            }
        }

        val disableSubtitlesParams = if (textRendererIndex != -1) {
            trackSelector.buildUponParameters()
                .clearSelectionOverrides(textRendererIndex)
                .setRendererDisabled(textRendererIndex, true)
                .build()
        } else {
            trackSelector.parameters // No change
        }
        tracks.add("None" to disableSubtitlesParams)

        if (textRendererIndex != -1) {
            val i = textRendererIndex
            val trackGroupArray = mappedTrackInfo.getTrackGroups(i)
            for (j in 0 until trackGroupArray.length) {
                val trackGroup = trackGroupArray.get(j)
                for (k in 0 until trackGroup.length) {
                    val format = trackGroup.getFormat(k)
                    val language = format.language?.let { Locale.forLanguageTag(it).displayLanguage } ?: "Unknown"
                    val label = format.label ?: language
                    val parameters = trackSelector.buildUponParameters()
                        .setRendererDisabled(i, false)
                        .setSelectionOverride(
                            i,
                            trackGroupArray,
                            DefaultTrackSelector.SelectionOverride(j, k)
                        )
                    tracks.add(label to parameters.build())
                }
            }
        }
        tracks
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Tracks") },
        text = {
            Column {
                Text("Audio", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                LazyColumn {
                    items(audioTracks) { (label, params) ->
                        Text(
                            text = label,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    trackSelector.parameters = params
                                    onDismiss()
                                }
                                .padding(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("Subtitles", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                LazyColumn {
                    items(subtitleTracks) { (label, params) ->
                        Text(
                            text = label,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    trackSelector.parameters = params
                                    onDismiss()
                                }
                                .padding(16.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
