package com.hasanege.materialtv

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.hasanege.materialtv.ui.theme.ExpressiveShapes
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Downloading
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.hasanege.materialtv.download.DownloadItem
import com.hasanege.materialtv.download.DownloadStatus
import com.hasanege.materialtv.model.Episode
import com.hasanege.materialtv.model.SeriesInfoResponse
import com.hasanege.materialtv.model.VodItem
import com.hasanege.materialtv.model.VodInfo
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject

@Composable
fun DetailScreen(
    movie: VodItem? = null,
    movieDetails: VodInfo? = null,
    series: SeriesInfoResponse? = null,
    lastWatchedEpisode: Episode? = null,
    watchProgress: Float = 0f, // 0f to 1f, or -1 if not started
    resumePosition: Long = 0L,
    nextEpisodeThresholdMinutes: Int = 5,
    activeDownloads: List<DownloadItem> = emptyList(),
    onBack: () -> Unit,
    onPlayMovie: ((VodItem) -> Unit)? = null,
    onPlayEpisode: ((Episode) -> Unit)? = null,
    onDownloadMovie: ((VodItem) -> Unit)? = null,
    onDownloadEpisode: ((Episode) -> Unit)? = null,
    onCancelDownload: ((String) -> Unit)? = null,
    onDownloadSeason: ((Int, List<Episode>) -> Unit)? = null,
    seriesId: Int = -1
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var seasonDownloadStarted by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    
    val downloadStartedMsg = stringResource(R.string.download_started)
    
    // Helper to format milliseconds to MM:SS or HH:MM:SS
    fun formatDuration(millis: Long): String {
        val seconds = millis / 1000
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) String.format("%d:%02d:%02d", h, m, s) else String.format("%02d:%02d", m, s)
    }

    // Helper for Turkish grammar: determines the correct ablative suffix ('dan, 'den, 'tan, 'ten)
    fun getTurkishAblativeSuffix(millis: Long): String {
        val seconds = (millis / 1000) % 60
        return if (seconds % 10 != 0L) {
            when (seconds % 10) {
                1L -> context.getString(R.string.detail_suffix_den) // bir-den
                2L -> context.getString(R.string.detail_suffix_den) // iki-den
                3L -> context.getString(R.string.detail_suffix_ten) // üç-ten
                4L -> context.getString(R.string.detail_suffix_ten) // dört-ten
                5L -> context.getString(R.string.detail_suffix_ten) // beş-ten
                6L -> context.getString(R.string.detail_suffix_dan) // altı-dan
                7L -> context.getString(R.string.detail_suffix_den) // yedi-den
                8L -> context.getString(R.string.detail_suffix_den) // sekiz-den
                9L -> context.getString(R.string.detail_suffix_dan) // dokuz-dan
                else -> context.getString(R.string.detail_suffix_dan)
            }
        } else {
            when (seconds) {
                0L  -> context.getString(R.string.detail_suffix_dan) // sıfır-dan
                10L -> context.getString(R.string.detail_suffix_dan) // on-dan
                20L -> context.getString(R.string.detail_suffix_den) // yirmi-den
                30L -> context.getString(R.string.detail_suffix_dan) // otuz-dan
                40L -> context.getString(R.string.detail_suffix_tan) // kırk-tan
                50L -> context.getString(R.string.detail_suffix_den) // elli-den
                else -> context.getString(R.string.detail_suffix_dan)
            }
        }
    }

    // Parsing Episodes for Series
    val episodesMap = remember(series) {
        if (series?.episodes != null) {
            try {
                val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }
                
                // Inspect the JsonElement type
                if (series.episodes is kotlinx.serialization.json.JsonObject) {
                    val entries = series.episodes.jsonObject.entries
                    entries.associate { (key, element) ->
                        val list = try {
                            json.decodeFromJsonElement<List<Episode>>(element)
                        } catch (e: Exception) {
                            emptyList()
                        }
                        
                        // Assign season number to episodes if missing
                        val seasonNum = key.toIntOrNull()
                        if (seasonNum != null) {
                             key to list.map { it.copy(season = seasonNum) }
                        } else {
                            key to list
                        }
                    }
                } else {
                    emptyMap()
                }
            } catch (e: Exception) {
                emptyMap<String, List<Episode>>()
            }
        } else {
            emptyMap()
        }
    }

    // State for Series
    val initialSeasonKey = remember(episodesMap, lastWatchedEpisode) {
        if (lastWatchedEpisode != null) {
             val foundSeason = episodesMap.entries.find { entry -> 
                 entry.value.any { it.id == lastWatchedEpisode.id } 
             }?.key
             
             foundSeason ?: lastWatchedEpisode.season?.toString() ?: "1"
        } else {
            if (episodesMap.containsKey("1")) "1" else episodesMap.keys.sortedBy { it.toIntOrNull() ?: 999 }.firstOrNull() ?: "1"
        }
    }

    var selectedSeasonKey by remember(initialSeasonKey) { 
        mutableStateOf(initialSeasonKey) 
    }
    
    val currentEpisodes = episodesMap[selectedSeasonKey] ?: emptyList()
    
    val title = movie?.name ?: series?.info?.name ?: stringResource(R.string.detail_unknown_title)
    val plot = movieDetails?.plot ?: series?.info?.plot ?: stringResource(R.string.detail_no_description)
    val rating = movieDetails?.rating ?: series?.info?.rating ?: stringResource(R.string.unknown)
    val backdropUrl = movieDetails?.backdropPath?.firstOrNull() 
        ?: series?.info?.backdropPath?.firstOrNull() 
        ?: series?.info?.cover 
        ?: movieDetails?.movieImage
        ?: movie?.streamIcon
    val genres = movieDetails?.genre ?: series?.info?.genre ?: ""
    val releaseDate = movieDetails?.releaseDate ?: series?.info?.releaseDate ?: ""
    val director = movieDetails?.director ?: series?.info?.director ?: ""
    val cast = movieDetails?.cast ?: series?.info?.cast ?: ""

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val safeTopPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    
    // Dynamic Hero Height - 50% of screen but at least 300dp
    val heroHeight = remember(screenHeight) { (screenHeight * 0.55f).coerceAtLeast(300.dp) }
    // Content starts below hero, adjusted for safe area
    val contentSpacerHeight = remember(heroHeight) { heroHeight - 80.dp }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 1. Background Image with Gradient
        // Fixed at top, not scrolling
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(heroHeight + 100.dp) // Extra for overlap
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(backdropUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            
            // Gradient Overlay - Top to Bottom
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.background.copy(alpha = 0.3f),
                                MaterialTheme.colorScheme.background.copy(alpha = 0.9f),
                                MaterialTheme.colorScheme.background
                            ),
                            startY = 0f,
                            endY = Float.POSITIVE_INFINITY
                        )
                    )
            )
        }

        // 2. Main Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // Spacer to push content down so image is visible
            Spacer(modifier = Modifier.height(contentSpacerHeight))
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    // Solid background for content to slide over image
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.background
                            ),
                            startY = 0f,
                            endY = 100f
                        )
                    )
                    .background(MaterialTheme.colorScheme.background)
                    .padding(bottom = 50.dp) // Bottom padding
            ) {
                 Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    // Title
                    Text(
                        text = title,
                        style = (if (configuration.screenWidthDp < 360) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.displaySmall).copy(
                            fontWeight = FontWeight.Bold,
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = Color.Black.copy(alpha = 0.5f),
                                blurRadius = 12f
                            )
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Metadata Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // IMDB Rating
                        if (rating != "N/A" && rating.isNotEmpty()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Star,
                                    contentDescription = stringResource(R.string.detail_rating),
                                    tint = Color(0xFFFFC107), // Amber for star
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = rating,
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        // Genre
                        if (genres.isNotEmpty()) {
                             Text(
                                text = genres.split(",").take(2).joinToString(", "),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                        }
    
                        // Date
                        if (releaseDate.isNotEmpty()) {
                             Text(
                                text = releaseDate.take(4),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Adaptive Button Layout - Column for phones (< 600dp), Row for tablets
                    val isPhoneScreen = configuration.screenWidthDp < 600
                    
                    // Responsive text style
                    val buttonTextStyle = when {
                        configuration.screenWidthDp < 400 -> MaterialTheme.typography.labelLarge
                        else -> MaterialTheme.typography.titleMedium
                    }
                    
                    // Play button text
                    val playButtonText = if (movie != null) {
                        if (resumePosition > 0) {
                            if (context.resources.configuration.locales[0].language == "tr") {
                                stringResource(
                                    R.string.detail_play_resume, 
                                    formatDuration(resumePosition), 
                                    getTurkishAblativeSuffix(resumePosition)
                                )
                            } else {
                                stringResource(R.string.detail_play_resume, formatDuration(resumePosition))
                            }
                        } else {
                            stringResource(R.string.detail_play)
                        }
                    } else if (lastWatchedEpisode != null) {
                        val sNum = lastWatchedEpisode.season?.toString() ?: initialSeasonKey
                        val eNum = lastWatchedEpisode.episodeNum ?: ""
                        if (resumePosition > 0) {
                            if (context.resources.configuration.locales[0].language == "tr") {
                                stringResource(
                                    R.string.detail_play_resume_episode, 
                                    sNum, 
                                    eNum, 
                                    formatDuration(resumePosition),
                                    getTurkishAblativeSuffix(resumePosition)
                                )
                            } else {
                                stringResource(R.string.detail_play_resume_episode, sNum, eNum, formatDuration(resumePosition))
                            }
                        } else {
                            stringResource(R.string.detail_play_episode, sNum, eNum)
                        }
                    } else {
                        stringResource(R.string.detail_play_episode, selectedSeasonKey, "1")
                    }
                    
                    // Play button onClick
                    val onPlayClick: () -> Unit = {
                        if (movie != null) onPlayMovie?.invoke(movie)
                        else if (lastWatchedEpisode != null) onPlayEpisode?.invoke(lastWatchedEpisode)
                        else if (currentEpisodes.isNotEmpty()) onPlayEpisode?.invoke(currentEpisodes.first())
                        else {
                            val firstSeason = episodesMap.keys.sortedBy { it.toIntOrNull() ?: 999 }.firstOrNull()
                            val firstEp = episodesMap[firstSeason]?.firstOrNull()
                            if (firstEp != null) onPlayEpisode?.invoke(firstEp)
                        }
                    }
                    
                    @Composable
                    fun PlayButtonContent() {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = playButtonText,
                                style = buttonTextStyle,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    
                    if (isPhoneScreen) {
                        // Vertical layout for phones
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Play Button - Full width
                            Button(
                                onClick = onPlayClick,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = com.hasanege.materialtv.ui.theme.ExpressiveShapes.Medium,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                PlayButtonContent()
                            }
                            
                            // Download Button - Full width
                            if (movie != null) {
                                val download = activeDownloads.find { 
                                    it.title == movie.name || (it.url.contains(movie.streamId.toString()))
                                }
                                val isDownloaded = download?.status == DownloadStatus.COMPLETED
                                val isDownloading = download?.status == DownloadStatus.DOWNLOADING || download?.status == DownloadStatus.PENDING
                                
                                 FilledTonalButton(
                                    onClick = { 
                                        if (isDownloaded) onPlayMovie?.invoke(movie)
                                        else if (!isDownloading) {
                                            onDownloadMovie?.invoke(movie)
                                            scope.launch { snackbarHostState.showSnackbar(downloadStartedMsg) }
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    shape = com.hasanege.materialtv.ui.theme.ExpressiveShapes.Medium,
                                    enabled = !isDownloading
                                ) {
                                    if (isDownloading) {
                                        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                            Text("${download?.progress ?: 0}%", style = MaterialTheme.typography.labelSmall)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            LinearProgressIndicator(
                                                progress = { (download?.progress ?: 0) / 100f },
                                                modifier = Modifier.width(60.dp).height(4.dp).clip(RoundedCornerShape(2.dp)),
                                            )
                                        }
                                    } else if (isDownloaded) {
                                        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Rounded.Check, contentDescription = null)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(stringResource(R.string.downloads_completed), style = buttonTextStyle)
                                        }
                                    } else {
                                        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Rounded.Download, contentDescription = null)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(stringResource(R.string.detail_download), style = buttonTextStyle)
                                        }
                                    }
                                }
                            } else if (series != null) {
                                FilledTonalButton(
                                    onClick = { 
                                        val seasonNum = selectedSeasonKey.toIntOrNull() ?: 1
                                        onDownloadSeason?.invoke(seasonNum, currentEpisodes)
                                        // Visual feedback
                                        seasonDownloadStarted = true
                                        scope.launch { snackbarHostState.showSnackbar(downloadStartedMsg) }
                                        scope.launch {
                                            delay(2000)
                                            seasonDownloadStarted = false
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    shape = ExpressiveShapes.Medium
                                ) {
                                    Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                        if (seasonDownloadStarted) {
                                            Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(stringResource(R.string.detail_download_season), style = buttonTextStyle)
                                        } else {
                                            Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(stringResource(R.string.detail_download_season), style = buttonTextStyle)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Horizontal layout for normal and large screens
                        val playButtonWeight = when {
                            configuration.screenWidthDp < 400 -> 0.65f
                            configuration.screenWidthDp < 600 -> 0.55f
                            else -> 0.5f
                        }
                        val downloadButtonWeight = 1f - playButtonWeight
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Play Button
                            Button(
                                onClick = onPlayClick,
                                modifier = Modifier
                                    .weight(playButtonWeight)
                                    .height(56.dp),
                                shape = com.hasanege.materialtv.ui.theme.ExpressiveShapes.Medium,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Start,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = playButtonText,
                                        style = buttonTextStyle,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                            
                            // Download Button
                            if (movie != null) {
                                val download = activeDownloads.find { 
                                    it.title == movie.name || (it.url.contains(movie.streamId.toString()))
                                }
                                val isDownloaded = download?.status == DownloadStatus.COMPLETED
                                val isDownloading = download?.status == DownloadStatus.DOWNLOADING || download?.status == DownloadStatus.PENDING
                                
                                FilledTonalButton(
                                    onClick = { 
                                        if (isDownloaded) onPlayMovie?.invoke(movie)
                                        else if (!isDownloading) {
                                            onDownloadMovie?.invoke(movie)
                                            scope.launch { snackbarHostState.showSnackbar(downloadStartedMsg) }
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(downloadButtonWeight)
                                        .height(56.dp),
                                    shape = com.hasanege.materialtv.ui.theme.ExpressiveShapes.Medium,
                                    enabled = !isDownloading
                                ) {
                                    if (isDownloading) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("${download?.progress ?: 0}%", style = MaterialTheme.typography.labelSmall)
                                            LinearProgressIndicator(
                                                progress = { (download?.progress ?: 0) / 100f },
                                                modifier = Modifier.width(60.dp).height(4.dp).clip(RoundedCornerShape(2.dp)),
                                            )
                                        }
                                    } else if (isDownloaded) {
                                        Icon(Icons.Rounded.Check, contentDescription = stringResource(R.string.downloads_completed))
                                    } else {
                                        Icon(Icons.Rounded.Download, contentDescription = stringResource(R.string.detail_download))
                                    }
                                }
                            } else if (series != null) {
                                FilledTonalButton(
                                    onClick = { 
                                        val seasonNum = selectedSeasonKey.toIntOrNull() ?: 1
                                        onDownloadSeason?.invoke(seasonNum, currentEpisodes)
                                        // Visual feedback
                                        seasonDownloadStarted = true
                                        scope.launch { snackbarHostState.showSnackbar(downloadStartedMsg) }
                                        scope.launch {
                                            delay(2000)
                                            seasonDownloadStarted = false
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(downloadButtonWeight)
                                        .height(56.dp),
                                    shape = ExpressiveShapes.Medium
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Start,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (seasonDownloadStarted) {
                                            Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = stringResource(R.string.detail_season_placeholder),
                                                style = buttonTextStyle,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                        } else {
                                            Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = stringResource(R.string.detail_season_placeholder),
                                                style = buttonTextStyle,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Storyline
                    Text(
                        text = stringResource(R.string.detail_description),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = plot,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 24.sp
                    )
                    
                    if (director.isNotEmpty() || cast.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        // ... Director/Cast ...
                         if (director.isNotEmpty()) {
                             Text(
                                text = stringResource(R.string.detail_director, director),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                         }
                         if (cast.isNotEmpty()) {
                             Text(
                                text = stringResource(R.string.detail_cast, cast),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                         }
                    }
                }
                
                // Series Specific Content
                if (series != null) {
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Season Selector with ExpressiveTabSlider
                    if (episodesMap.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.tab_seasons),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 24.dp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        val sortedSeasons = episodesMap.keys.sortedBy { it.toIntOrNull() ?: 999 }
                        val seasonTabs = sortedSeasons.map { stringResource(R.string.detail_season, it) }
                        val selectedIndex = sortedSeasons.indexOf(selectedSeasonKey).coerceAtLeast(0)
                        
                        com.hasanege.materialtv.ui.ExpressiveTabSlider(
                            tabs = seasonTabs,
                            selectedIndex = selectedIndex,
                            onTabSelected = { index ->
                                selectedSeasonKey = sortedSeasons.getOrNull(index) ?: selectedSeasonKey
                            },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                            scrollable = sortedSeasons.size > 4 // Scroll only if many seasons
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Episodes List
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        currentEpisodes.forEach { episode ->
                             EpisodeItem(
                                episode = episode,
                                seriesName = series?.info?.name,
                                activeDownloads = activeDownloads,
                                onPlay = { onPlayEpisode?.invoke(episode) },
                                onDownload = { onDownloadEpisode?.invoke(episode) },
                                onCancel = { downloadId -> onCancelDownload?.invoke(downloadId) }
                            )
                        }
                    }
                }
            }
        }
        
        // 3. Back Button (Fixed)
        FilledTonalIconButton(
            onClick = onBack,
            modifier = Modifier
                .padding(top = 16.dp + safeTopPadding, start = 24.dp)
                .size(48.dp),
            colors = androidx.compose.material3.IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Icon(Icons.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back))
        }

        // Snackbar Host
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp) // Avoid overlap with bottom nav if any
        )
    }
}

@Composable
fun EpisodeItem(
    episode: Episode,
    seriesName: String?,
    activeDownloads: List<DownloadItem>,
    onPlay: () -> Unit,
    onDownload: () -> Unit,
    onCancel: ((String) -> Unit)? = null
) {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale"
    )

    // Find download for this episode
    val download = activeDownloads.find { downloadItem ->
        val sameSeries = seriesName != null && downloadItem.seriesName == seriesName
        val matchBySeasonEp = sameSeries && 
                              downloadItem.seasonNumber == episode.season && 
                              downloadItem.episodeNumber == episode.episodeNum?.toIntOrNull()
        val matchById = downloadItem.url.contains("/${episode.id}.")
        matchBySeasonEp || matchById
    }
    
    val isDownloaded = download?.status == DownloadStatus.COMPLETED
    val isDownloading = download?.status == DownloadStatus.DOWNLOADING || download?.status == DownloadStatus.PENDING

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(),
                onClick = onPlay
            ),
        shape = com.hasanege.materialtv.ui.theme.ExpressiveShapes.Medium,
        colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val imageUrl = episode.info?.movieImage
            if (!imageUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .width(if (configuration.screenWidthDp < 360) 100.dp else 130.dp)
                        .aspectRatio(16f/9f)
                        .clip(ExpressiveShapes.Small)
                )
            } else {
                 Surface(modifier = Modifier.size(48.dp), shape = ExpressiveShapes.Medium, color = MaterialTheme.colorScheme.primaryContainer) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = episode.episodeNum ?: "?", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                if (!imageUrl.isNullOrEmpty()) {
                     Text(text = stringResource(R.string.detail_episode, episode.episodeNum ?: "?"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
                Text(text = episode.title ?: stringResource(R.string.detail_episode, episode.episodeNum ?: "?"), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), maxLines = 2, overflow = TextOverflow.Ellipsis)
                
                val duration = episode.duration
                if (duration != null) {
                    Text(
                        text = duration,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            IconButton(onClick = onPlay) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = stringResource(R.string.detail_play), tint = MaterialTheme.colorScheme.primary)
            }
            
            if (isDownloaded) {
                 IconButton(onClick = { /* Already downloaded */ }) {
                     Icon(Icons.Rounded.Check, contentDescription = stringResource(R.string.downloads_completed), tint = MaterialTheme.colorScheme.primary)
                 }
            } else if (isDownloading) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(48.dp)) {
                     CircularProgressIndicator(
                         progress = { (download?.progress ?: 0) / 100f },
                         modifier = Modifier.size(40.dp),
                         strokeWidth = 3.dp,
                         trackColor = MaterialTheme.colorScheme.surfaceVariant,
                     )
                     IconButton(onClick = { download?.id?.let { onCancel?.invoke(it) } }, modifier = Modifier.size(32.dp)) {
                         Icon(Icons.Rounded.Close, contentDescription = "Cancel", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                     }
                }
            } else {
                IconButton(onClick = onDownload) {
                   Icon(Icons.Rounded.Download, contentDescription = stringResource(R.string.detail_download), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
