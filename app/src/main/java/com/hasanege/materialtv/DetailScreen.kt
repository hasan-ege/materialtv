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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
                1L -> "'den" // bir-den
                2L -> "'den" // iki-den
                3L -> "'ten" // üç-ten
                4L -> "'ten" // dört-ten
                5L -> "'ten" // beş-ten
                6L -> "'dan" // altı-dan
                7L -> "'den" // yedi-den
                8L -> "'den" // sekiz-den
                9L -> "'dan" // dokuz-dan
                else -> "'dan"
            }
        } else {
            when (seconds) {
                0L  -> "'dan" // sıfır-dan
                10L -> "'dan" // on-dan
                20L -> "'den" // yirmi-den
                30L -> "'dan" // otuz-dan
                40L -> "'tan" // kırk-tan
                50L -> "'den" // elli-den
                else -> "'dan"
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
    
    val title = movie?.name ?: series?.info?.name ?: "Unknown Title"
    val plot = movieDetails?.plot ?: series?.info?.plot ?: "No description available."
    val rating = movieDetails?.rating ?: series?.info?.rating ?: "N/A"
    val backdropUrl = movieDetails?.backdropPath?.firstOrNull() 
        ?: series?.info?.backdropPath?.firstOrNull() 
        ?: series?.info?.cover 
        ?: movieDetails?.movieImage
        ?: movie?.streamIcon
    val genres = movieDetails?.genre ?: series?.info?.genre ?: ""
    val releaseDate = movieDetails?.releaseDate ?: series?.info?.releaseDate ?: ""
    val director = movieDetails?.director ?: series?.info?.director ?: ""
    val cast = movieDetails?.cast ?: series?.info?.cast ?: ""

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
                .height(600.dp) // Taller to ensure coverage
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
            Spacer(modifier = Modifier.height(420.dp))
            
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
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.Bold,
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = Color.Black.copy(alpha = 0.5f),
                                blurRadius = 12f
                            )
                        ),
                        color = MaterialTheme.colorScheme.onBackground
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
                                    contentDescription = "Rating",
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
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
    
                        // Date
                        if (releaseDate.isNotEmpty()) {
                             Text(
                                text = releaseDate.take(4),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Play Button
                        Button(
                            onClick = {
                                 if (movie != null) onPlayMovie?.invoke(movie)
                                 else if (lastWatchedEpisode != null) onPlayEpisode?.invoke(lastWatchedEpisode)
                                 else if (currentEpisodes.isNotEmpty()) onPlayEpisode?.invoke(currentEpisodes.first())
                                 else {
                                     val firstSeason = episodesMap.keys.sortedBy { it.toIntOrNull() ?: 999 }.firstOrNull()
                                     val firstEp = episodesMap[firstSeason]?.firstOrNull()
                                     if (firstEp != null) onPlayEpisode?.invoke(firstEp)
                                 }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = com.hasanege.materialtv.ui.theme.ExpressiveShapes.Medium,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = run {
                                    val isTurkish = context.resources.configuration.locales[0].language == "tr"
                                    if (movie != null) {
                                        if (resumePosition > 0) {
                                            if (isTurkish) {
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
                                            if (isTurkish) {
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
                                },
                                style = MaterialTheme.typography.titleMedium
                            )
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
                                    if (isDownloaded) {
                                        // Optional: Play directly if downloaded
                                        onPlayMovie?.invoke(movie)
                                    } else if (!isDownloading) {
                                        onDownloadMovie?.invoke(movie) 
                                    }
                                },
                                modifier = Modifier.height(56.dp),
                                shape = com.hasanege.materialtv.ui.theme.ExpressiveShapes.Medium,
                                enabled = !isDownloading
                            ) {
                                if (isDownloading) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "${download?.progress ?: 0}%",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                        LinearProgressIndicator(
                                            progress = { (download?.progress ?: 0) / 100f },
                                            modifier = Modifier.width(60.dp).height(4.dp).clip(RoundedCornerShape(2.dp)),
                                        )
                                    }
                                } else if (isDownloaded) {
                                    Icon(Icons.Rounded.Check, contentDescription = "Downloaded")
                                } else {
                                    Icon(Icons.Rounded.Download, contentDescription = "Download")
                                }
                            }
                        } else if (series != null) {
                             FilledTonalButton(
                                onClick = { 
                                    val seasonNum = selectedSeasonKey.toIntOrNull() ?: 1
                                    onDownloadSeason?.invoke(seasonNum, currentEpisodes)
                                },
                                modifier = Modifier.height(56.dp),
                                shape = ExpressiveShapes.Medium
                            ) {
                                 Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Download, contentDescription = stringResource(R.string.detail_download_season))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.detail_season_placeholder))
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
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
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
        
        // Back Button
        FilledTonalIconButton(
            onClick = onBack,
            modifier = Modifier
                .padding(top = 48.dp, start = 24.dp)
                .size(48.dp),
            colors = androidx.compose.material3.IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Icon(Icons.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back))
        }
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

    // Debug Matching
    val epSeason = episode.season
    val epNum = episode.episodeNum?.toIntOrNull()
    
    // Find download for this episode
    // Must match Series Name AND (Season/Ep OR ID)
    // DownloadItem stores seriesName.
    val download = activeDownloads.find { downloadItem ->
        val sameSeries = seriesName != null && downloadItem.seriesName == seriesName
        
        // Strategy 1: Match by Season/Episode if Series matches
        val matchBySeasonEp = sameSeries && 
                              downloadItem.seasonNumber == epSeason && 
                              downloadItem.episodeNumber == epNum
                              
        // Strategy 2: Match by ID in URL (Unique ID is safest if available)
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
            // Episode Image or Number Box
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
                        .width(130.dp)
                        .aspectRatio(16f/9f)
                        .clip(ExpressiveShapes.Small)
                )
            } else {
                 Surface(
                    modifier = Modifier
                        .size(48.dp),
                    shape = ExpressiveShapes.Medium,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = episode.episodeNum ?: "?",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                if (!imageUrl.isNullOrEmpty()) {
                     Text(
                        text = stringResource(R.string.detail_episode, episode.episodeNum ?: "?"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Text(
                    text = episode.title ?: "Episode ${episode.episodeNum}",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
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
                Icon(Icons.Rounded.PlayArrow, contentDescription = "Play", tint = MaterialTheme.colorScheme.primary)
            }
            
            if (isDownloaded) {
                 IconButton(onClick = { /* Already downloaded, maybe delete? */ }) {
                     Icon(Icons.Rounded.Check, contentDescription = "Downloaded", tint = MaterialTheme.colorScheme.primary)
                 }
            } else if (isDownloading) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(48.dp)) {
                     CircularProgressIndicator(
                         progress = { (download?.progress ?: 0) / 100f },
                         modifier = Modifier.size(40.dp),
                         strokeWidth = 3.dp,
                         trackColor = MaterialTheme.colorScheme.surfaceVariant,
                     )
                     IconButton(
                        onClick = { download?.id?.let { onCancel?.invoke(it) } },
                        modifier = Modifier.size(32.dp)
                     ) {
                         Icon(
                            Icons.Rounded.Close, 
                            contentDescription = "Cancel", 
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                         )
                     }
                }
            } else {
                IconButton(onClick = onDownload) {
                   Icon(Icons.Rounded.Download, contentDescription = "Download", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
