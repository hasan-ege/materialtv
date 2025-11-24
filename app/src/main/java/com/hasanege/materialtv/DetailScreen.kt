
package com.hasanege.materialtv

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.hasanege.materialtv.model.Episode
import com.hasanege.materialtv.model.SeriesInfoResponse
import com.hasanege.materialtv.model.VodItem
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement

private val json = Json {
    ignoreUnknownKeys = true
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    movie: VodItem? = null,
    series: SeriesInfoResponse? = null,
    onBack: () -> Unit = { },
    onPlayMovie: (VodItem) -> Unit = { },
    onPlayEpisode: (Episode) -> Unit = { },
    seriesId: Int? = null
) {
    val episodesMap: Map<String, List<Episode>> = remember(series) {
        if (series?.episodes != null) {
            try {
                json.decodeFromJsonElement<Map<String, List<Episode>>>(series.episodes)
            } catch (_: Exception) {
                emptyMap()
            }
        } else {
            emptyMap()
        }
    }

    val seasonNames = remember(episodesMap) { episodesMap.keys.toList() }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val history = remember { WatchHistoryManager.getHistory() }
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        item {
            Box(contentAlignment = Alignment.TopStart) {
                AsyncImage(
                    model = movie?.streamIcon ?: series?.info?.cover,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(450.dp),
                    contentScale = ContentScale.Crop
                )
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            }
        }

        item {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = movie?.name ?: series?.info?.name ?: "",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${movie?.year ?: series?.info?.releaseDate ?: ""} • ${series?.info?.genre ?: ""} • ",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                    Icon(
                        Icons.Default.Star,
                        contentDescription = "Rating",
                        tint = Color.Yellow,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = " ${movie?.rating5Based?.toString() ?: series?.info?.rating5based ?: ""}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            if (movie != null) {
                                onPlayMovie(movie)
                            } else if (series != null) {
                                val lastWatchedEpisode = history.firstOrNull { it.seriesId == seriesId }
                                if (lastWatchedEpisode != null) {
                                    val allEpisodes = episodesMap.values.flatten()
                                    val episodeToPlay = allEpisodes.find { it.id == lastWatchedEpisode.episodeId } ?: episodesMap.values.first().first()
                                    onPlayEpisode(episodeToPlay)
                                } else {
                                    val firstEpisode = episodesMap.values.firstOrNull()?.firstOrNull()
                                    firstEpisode?.let { onPlayEpisode(it) }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Play")
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(
                        onClick = {
                             if (movie != null) {
                                DownloadHelper.startDownload(context, movie)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = "Download")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Download")
                    }
                }
                series?.info?.plot?.let {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Synopsis",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
                }
                series?.info?.cast?.let {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Cast",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        if (seasonNames.size > 1) {
            item {
                PrimaryTabRow(selectedTabIndex = selectedTabIndex, modifier = Modifier.padding(horizontal = 16.dp)) {
                    seasonNames.forEachIndexed { index, seasonName ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(seasonName) }
                        )
                    }
                }
            }
        }

        if (seasonNames.isNotEmpty()) {
            val selectedSeasonName = seasonNames[selectedTabIndex]
            episodesMap[selectedSeasonName]?.let { episodeList ->
                itemsIndexed(episodeList, key = { _, episode -> episode.id }) { index, episode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPlayEpisode(episode) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(120.dp)
                                .height(80.dp)
                                .clip(RoundedCornerShape(8.dp))
                        ) {
                            AsyncImage(
                                model = episode.info?.movieImage,
                                contentDescription = episode.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )

                            val episodeHistory = history.firstOrNull { it.episodeId == episode.id }

                            if (episodeHistory != null && episodeHistory.duration > 0 && episodeHistory.position > 0) {
                                LinearProgressIndicator(
                                    progress = { episodeHistory.position.toFloat() / episodeHistory.duration.toFloat() },
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp, vertical = 4.dp)
                                        .clip(RoundedCornerShape(50))
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "Episode ${index + 1}",
                                color = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.Bold
                            )
                            episode.title?.let {
                                Text(
                                    text = it,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(onClick = {
                            series?.info?.name?.let {
                                DownloadHelper.startDownload(context, episode, it)
                            }
                        }) {
                            Icon(Icons.Default.Download, contentDescription = "Download")
                        }
                    }
                }
            }
        }
    }
}
