package com.hasanege.materialtv.ui.screens.downloads

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.hasanege.materialtv.DownloadFilter
import com.hasanege.materialtv.DownloadsViewModel
import com.hasanege.materialtv.download.ContentType
import com.hasanege.materialtv.download.DownloadItem
import com.hasanege.materialtv.download.DownloadStatus
import com.hasanege.materialtv.ui.theme.ExpressiveShapes
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.hasanege.materialtv.R
import androidx.compose.ui.res.stringResource

// Define unified display groups for sorting
sealed class DisplayGroup {
    data class Movie(val download: DownloadItem) : DisplayGroup()
    data class Series(val name: String, val episodes: List<DownloadItem>) : DisplayGroup()
    
    val latestTimestamp: Long get() = when(this) {
        is Movie -> download.createdAt
        is Series -> episodes.maxOfOrNull { it.createdAt } ?: 0L
    }
    
    val isActive: Boolean get() = when(this) {
        is Movie -> download.status == DownloadStatus.DOWNLOADING || download.status == DownloadStatus.PENDING
        is Series -> episodes.any { it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.PENDING }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun DownloadsScreen(viewModel: DownloadsViewModel) {
    val downloads by viewModel.downloads.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Pull to refresh state
    var isRefreshing by remember { mutableStateOf(false) }
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = {
            scope.launch {
                isRefreshing = true
                viewModel.rescanDownloads()
                delay(1500) // Tarama için minimum bekleme
                isRefreshing = false
            }
        }
    )
    
    LaunchedEffect(Unit) {
        viewModel.initialize(context)
    }
    
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(selectedFilter) {
        isVisible = false
        delay(100)
        isVisible = true
    }
    
    // Group and sort downloads into a unified timeline
    val displayGroups = remember(downloads) {
        val movies = downloads.filter { it.contentType == ContentType.MOVIE }
            .map { DisplayGroup.Movie(it) }
            
        val series = downloads
            .filter { it.contentType == ContentType.EPISODE && it.seriesName != null }
            .groupBy { it.seriesName!! }
            .map { (name, episodes) -> 
                DisplayGroup.Series(
                    name, 
                    episodes.sortedWith(compareBy({ it.seasonNumber ?: 0 }, { it.episodeNumber ?: 0 }))
                ) 
            }
            
        (movies + series).sortedWith(
            compareByDescending<DisplayGroup> { it.isActive }
                .thenByDescending { it.latestTimestamp }
        )
    }
    
    // Track expanded series
    var expandedSeries by remember { mutableStateOf<Set<String>>(emptySet()) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {
        if (downloads.isEmpty() && !isRefreshing) {
            EmptyDownloadsView()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Stats Card
                item {
                    AnimatedVisibility(
                        visible = isVisible,
                        enter = fadeIn() + slideInVertically(initialOffsetY = { -40 })
                    ) {
                        DownloadStatsCard(downloads)
                    }
                }
                
                // Unified Unified Timeline (Movies and Series intermingled)
                displayGroups.forEach { group ->
                    when (group) {
                        is DisplayGroup.Movie -> {
                            val download = group.download
                            item(key = "movie_${download.id}") {
                                AnimatedVisibility(
                                    visible = isVisible,
                                    enter = fadeIn() + slideInVertically(initialOffsetY = { 100 })
                                ) {
                                    DownloadItemCard(
                                        download = download,
                                        onPause = { viewModel.pauseDownload(download.id) },
                                        onResume = { viewModel.resumeDownload(download.id) },
                                        onCancel = { viewModel.cancelDownload(download.id) },
                                        onDelete = { viewModel.deleteDownload(download.id) },
                                        onPlay = { viewModel.playDownload(context, download) }
                                    )
                                }
                            }
                        }
                        is DisplayGroup.Series -> {
                            val seriesName = group.name
                            val episodes = group.episodes
                            val isExpanded = expandedSeries.contains(seriesName)
                            
                            // Series header
                            item(key = "series_$seriesName") {
                                AnimatedVisibility(
                                    visible = isVisible,
                                    enter = fadeIn() + slideInVertically(initialOffsetY = { 100 })
                                ) {
                                    SeriesGroupHeader(
                                        seriesName = seriesName,
                                        episodeCount = episodes.size,
                                        isExpanded = isExpanded,
                                        downloadingCount = episodes.count { it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.PENDING },
                                        completedCount = episodes.count { it.status == DownloadStatus.COMPLETED },
                                        thumbnailUrl = episodes.firstOrNull()?.seriesCoverUrl 
                                            ?: episodes.firstOrNull()?.thumbnailUrl,
                                        episodeFilePath = episodes.firstOrNull()?.filePath,
                                        onClick = {
                                            expandedSeries = if (isExpanded) {
                                                expandedSeries - seriesName
                                            } else {
                                                expandedSeries + seriesName
                                            }
                                        }
                                    )
                                }
                            }
                            
                            // Episodes (when expanded)
                            items(
                                episodes,
                                key = { it.id }
                            ) { download ->
                                AnimatedVisibility(
                                    visible = isExpanded,
                                    enter = fadeIn(animationSpec = tween(200)) + 
                                            expandVertically(
                                                animationSpec = spring(
                                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                                    stiffness = Spring.StiffnessMedium
                                                )
                                            ),
                                    exit = fadeOut(animationSpec = tween(150)) + 
                                           shrinkVertically(
                                               animationSpec = spring(
                                                   dampingRatio = Spring.DampingRatioNoBouncy,
                                                   stiffness = Spring.StiffnessMedium
                                               )
                                           )
                                ) {
                                    DownloadItemCard(
                                        download = download,
                                        onPause = { viewModel.pauseDownload(download.id) },
                                        onResume = { viewModel.resumeDownload(download.id) },
                                        onCancel = { viewModel.cancelDownload(download.id) },
                                        onDelete = { viewModel.deleteDownload(download.id) },
                                        onPlay = { viewModel.playDownload(context, download) },
                                        isEpisode = true
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Pull to refresh indicator
        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            backgroundColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        )
    }
}

/**
 * Dizi grubu başlık komponenti - kapak fotoğrafı ile
 */
@Composable
fun SeriesGroupHeader(
    seriesName: String,
    episodeCount: Int,
    isExpanded: Boolean,
    downloadingCount: Int,
    completedCount: Int,
    thumbnailUrl: String? = null,
    episodeFilePath: String? = null,
    onClick: () -> Unit
) {
    // Smooth rotation for expand icon
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        )
    )
    
    // Yerel kapak dosyası yolunu hesapla
    val localCoverPath = remember(episodeFilePath) {
        if (episodeFilePath != null) {
            val videoFile = java.io.File(episodeFilePath)
            val seasonDir = videoFile.parentFile
            val seriesDir = seasonDir?.parentFile
            java.io.File(seriesDir, "cover.png")
        } else null
    }
    
    val coverExists = remember(localCoverPath) { localCoverPath?.exists() == true }
    val coverModel = if (coverExists) localCoverPath else thumbnailUrl
    
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = ExpressiveShapes.Large,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Series cover thumbnail
                Box(
                    modifier = Modifier
                        .size(width = 60.dp, height = 85.dp)
                        .clip(ExpressiveShapes.Medium)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    if (coverModel != null) {
                        AsyncImage(
                            model = coverModel,
                            contentDescription = seriesName,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Tv,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    
                    // Episode count badge
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp),
                        shape = ExpressiveShapes.ExtraSmall,
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = "$episodeCount",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = seriesName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Completed badge
                        if (completedCount > 0) {
                            Surface(
                                shape = ExpressiveShapes.Full,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "$completedCount",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                        
                        // Downloading badge
                        if (downloadingCount > 0) {
                            Surface(
                                shape = ExpressiveShapes.Full,
                                color = MaterialTheme.colorScheme.tertiaryContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = MaterialTheme.colorScheme.tertiary
                                    )
                                    Text(
                                        text = "$downloadingCount",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.tertiary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Expand/collapse icon with smooth rotation
            Surface(
                shape = ExpressiveShapes.Full,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    imageVector = Icons.Filled.ExpandMore,
                    contentDescription = if (isExpanded) stringResource(R.string.action_collapse) else stringResource(R.string.action_expand),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .padding(8.dp)
                        .graphicsLayer { rotationZ = rotationAngle }
                )
            }
        }
    }
}

@Composable
private fun DownloadStatsCard(downloads: List<DownloadItem>) {
    val downloadingCount = downloads.count { it.status == DownloadStatus.DOWNLOADING }
    val completedCount = downloads.count { it.status == DownloadStatus.COMPLETED }
    val pausedCount = downloads.count { it.status == DownloadStatus.PAUSED }
    val totalBytes = downloads.filter { it.status == DownloadStatus.COMPLETED }.sumOf { it.totalBytes }
    
    // Animated scale for entrance
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.9f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
    )
    
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale },
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = ExpressiveShapes.Large,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.downloads_status_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                if (totalBytes > 0) {
                    Surface(
                        shape = ExpressiveShapes.Full,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = formatTotalSize(totalBytes),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(count = downloadingCount, label = stringResource(R.string.downloads_status_downloading), icon = Icons.Default.Download, isActive = downloadingCount > 0)
                StatItem(count = completedCount, label = stringResource(R.string.downloads_status_completed), icon = Icons.Default.CheckCircle, isActive = false)
                StatItem(count = pausedCount, label = stringResource(R.string.downloads_status_paused), icon = Icons.Default.Pause, isActive = false)
            }
        }
    }
}

private fun formatTotalSize(bytes: Long): String {
    return when {
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
        else -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
    }
}

@Composable
private fun StatItem(
    count: Int, 
    label: String, 
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean = false
) {
    // Pulse animation for active downloads
    val infiniteTransition = rememberInfiniteTransition()
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isActive) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.graphicsLayer { if (isActive) { scaleX = pulse; scaleY = pulse } }
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(ExpressiveShapes.Full)
                .background(
                    if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isActive) MaterialTheme.colorScheme.primary 
                       else MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun FilterTabs(
    selectedFilter: DownloadFilter,
    onFilterSelected: (DownloadFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val filters = listOf(
        DownloadFilter.ALL to stringResource(R.string.downloads_all),
        DownloadFilter.DOWNLOADING to stringResource(R.string.downloads_status_downloading),
        DownloadFilter.COMPLETED to stringResource(R.string.downloads_status_completed),
        DownloadFilter.PAUSED to stringResource(R.string.downloads_status_paused)
    )
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = ExpressiveShapes.ExtraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                filters.forEach { (filter, label) ->
                    val isSelected = selectedFilter == filter
                    Surface(
                        shape = ExpressiveShapes.ExtraLarge,
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer 
                                else MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.clickable { onFilterSelected(filter) }
                    ) {
                        Text(
                            text = label,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadItemCard(
    download: DownloadItem,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    onPlay: () -> Unit,
    isEpisode: Boolean = false
) {
    val interactionScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )
    
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = interactionScale; scaleY = interactionScale },
        shape = ExpressiveShapes.Medium,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail - önce yerel kapak dosyasını dene (film için), bölüm için video'dan çıkar
            Box(
                modifier = Modifier
                    .size(
                        width = if (download.contentType == ContentType.MOVIE) 90.dp else 80.dp,
                        height = if (download.contentType == ContentType.MOVIE) 135.dp else 60.dp
                    )
                    .clip(ExpressiveShapes.Small)
            ) {
                // Kapak/thumbnail kaynağını belirle
                val coverModel = remember(download.filePath, download.contentType) {
                    when (download.contentType) {
                        ContentType.MOVIE -> {
                            // Film: Yerel kapak dosyası (FilmAdi.png)
                            val videoFile = java.io.File(download.filePath)
                            val parentDir = videoFile.parentFile
                            val coverFile = java.io.File(parentDir, "${videoFile.nameWithoutExtension}.png")
                            if (coverFile.exists()) coverFile else download.thumbnailUrl
                        }
                        ContentType.EPISODE -> {
                            // Bölüm: Yerel thumbnail dosyasını ara
                            // Video: E24_Bolumadi.mp4 -> Thumbnail: E24_thumbnail.png
                            val videoFile = java.io.File(download.filePath)
                            val parentDir = videoFile.parentFile
                            val fileName = videoFile.nameWithoutExtension
                            
                            // Bölüm numarasını çıkar (E01, E24 vb.)
                            // Video dosyası formatı: E24_Bolumadi veya E01_Attack_on_Titan...
                            val episodePrefix = fileName.split("_").firstOrNull() ?: fileName
                            
                            // E24_thumbnail.png formatında ara
                            val thumbnailFile = java.io.File(parentDir, "${episodePrefix}_thumbnail.png")
                            if (thumbnailFile.exists()) {
                                thumbnailFile
                            } else {
                                // NEW: Check for generated video thumbnail (_thumb.jpg)
                                val videoThumb = java.io.File(parentDir, "${fileName}_thumb.jpg")
                                if (videoThumb.exists()) {
                                    videoThumb
                                } else {
                                    // Alternatif: video adı + _thumbnail.png (E24_Bolumadi_thumbnail.png)
                                    val altThumbnail = java.io.File(parentDir, "${fileName}_thumbnail.png")
                                    if (altThumbnail.exists()) {
                                        altThumbnail
                                    } else {
                                        // Alternatif: video adı + .png
                                        val pngFile = java.io.File(parentDir, "${fileName}.png")
                                        if (pngFile.exists()) {
                                            pngFile
                                        } else {
                                            // Yoksa dizi kapağını göster (Series/DiziAdi/cover.png)
                                            val seasonDir = parentDir // S01
                                            val seriesDir = seasonDir?.parentFile // Attack on Titan
                                            val seriesCover = seriesDir?.let { java.io.File(it, "cover.png") }
                                            if (seriesCover?.exists() == true) {
                                                seriesCover
                                            } else {
                                                // Son çare: URL'den yükle
                                                download.thumbnailUrl
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                if (coverModel != null) {
                    AsyncImage(
                        model = coverModel,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (download.contentType == ContentType.MOVIE) 
                                Icons.Default.Movie else Icons.Default.Tv,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Progress overlay
                if (download.status == DownloadStatus.DOWNLOADING && download.progress > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(download.progress / 100f)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = download.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                download.displaySubtitle()?.let { subtitle ->
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusChip(status = download.status)
                    
                    if (download.status == DownloadStatus.DOWNLOADING) {
                        Spacer(modifier = Modifier.width(8.dp))
                        // İlerleme ve hız
                        val speedText = download.formatSpeed()
                        // Kalan süre hesapla
                        val remainingBytes = download.totalBytes - download.downloadedBytes
                        val etaText = download.estimatedTimeRemaining()
                        
                        Text(
                            text = buildString {
                                append("${download.progress}%")
                                append(" • $speedText")
                                if (etaText != null) {
                                    append(" • $etaText")
                                }
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    if (download.status == DownloadStatus.COMPLETED && download.totalBytes > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = download.formatFileSize(download.totalBytes),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Actions
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                when (download.status) {
                    DownloadStatus.DOWNLOADING -> {
                        IconButton(onClick = onPause, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Pause, stringResource(R.string.action_pause), tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = onCancel, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Close, stringResource(R.string.action_cancel), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    DownloadStatus.PAUSED -> {
                        IconButton(onClick = onResume, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.PlayArrow, stringResource(R.string.action_resume), tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = onCancel, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Close, stringResource(R.string.action_cancel), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    DownloadStatus.COMPLETED -> {
                        IconButton(onClick = onPlay, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.PlayArrow, stringResource(R.string.action_play), tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Delete, stringResource(R.string.action_delete), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    DownloadStatus.FAILED -> {
                        IconButton(onClick = onResume, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Refresh, stringResource(R.string.action_retry), tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Delete, stringResource(R.string.action_delete), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    else -> {
                        IconButton(onClick = onCancel, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Close, stringResource(R.string.action_cancel), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusChip(status: DownloadStatus) {
    val (text, color) = when (status) {
        DownloadStatus.PENDING -> stringResource(R.string.downloads_status_pending) to MaterialTheme.colorScheme.tertiary
        DownloadStatus.DOWNLOADING -> stringResource(R.string.downloads_status_downloading) to MaterialTheme.colorScheme.primary
        DownloadStatus.PAUSED -> stringResource(R.string.downloads_status_paused) to MaterialTheme.colorScheme.secondary
        DownloadStatus.COMPLETED -> stringResource(R.string.downloads_status_completed) to MaterialTheme.colorScheme.primary
        DownloadStatus.FAILED -> stringResource(R.string.downloads_status_failed) to MaterialTheme.colorScheme.error
        DownloadStatus.CANCELLED -> stringResource(R.string.downloads_status_cancelled) to MaterialTheme.colorScheme.error
    }
    
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            maxLines = 1
        )
    }
}

@Composable
private fun EmptyDownloadsView() {
    // Subtle pulse animation instead of floating
    val infiniteTransition = rememberInfiniteTransition()
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Use BoxWithConstraints for responsive layout
        BoxWithConstraints(
            modifier = Modifier
                .padding(16.dp)
                .widthIn(max = 500.dp) // Max width for tablets
        ) {
            val isCompact = maxWidth < 360.dp
            val cardFraction = if (isCompact) 1f else 0.9f
            val cardPadding = if (isCompact) 24.dp else 40.dp
            
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(cardFraction),
                shape = ExpressiveShapes.ExtraLarge,
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(cardPadding)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Static icon with subtle pulse
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .graphicsLayer { 
                                scaleX = pulseScale
                                scaleY = pulseScale
                            }
                            .clip(ExpressiveShapes.Full)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    Text(
                        text = stringResource(R.string.downloads_empty_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Text(
                        text = stringResource(R.string.downloads_empty_desc),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Hint chips - responsive with FlowRow
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = ExpressiveShapes.Full,
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Movie,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = stringResource(R.string.tab_movies),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                        Surface(
                            shape = ExpressiveShapes.Full,
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tv,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Text(
                                    text = stringResource(R.string.tab_series),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
