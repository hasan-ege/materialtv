package com.hasanege.materialtv

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadForOffline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import coil.compose.AsyncImage
import com.hasanege.materialtv.model.ContinueWatchingItem
import com.hasanege.materialtv.model.Episode
import com.hasanege.materialtv.model.SeriesDetailScreenData
import com.hasanege.materialtv.network.SessionManager
import com.hasanege.materialtv.ui.theme.MaterialTVTheme
import com.hasanege.materialtv.R
import kotlinx.coroutines.launch

@UnstableApi
class SeriesDetailActivity : ComponentActivity() {

    private val viewModel: SeriesDetailViewModel by viewModels { SeriesDetailViewModelFactory }
    private val homeViewModel: HomeViewModel by viewModels { HomeViewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val seriesId = intent.getIntExtra("SERIES_ID", -1)
        val username = SessionManager.username ?: ""
        val password = SessionManager.password ?: ""

        if (seriesId != -1) {
            viewModel.loadSeriesInfo(username, password, seriesId)
        }
        homeViewModel.loadContinueWatching()

        setContent {
            MaterialTVTheme {
                val seriesInfoState = viewModel.seriesInfoState
                val continueWatchingState = homeViewModel.continueWatchingState
                val continueWatchingItems = (continueWatchingState as? UiState.Success)?.data ?: emptyList()

                DetailsScreen(
                    state = seriesInfoState,
                    continueWatchingItems = continueWatchingItems,
                    onBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@UnstableApi
@Composable
fun DetailsScreen(state: UiState<SeriesDetailScreenData>, continueWatchingItems: List<ContinueWatchingItem>, onBack: () -> Unit) {
    val context = LocalContext.current
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val showButton by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 0
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            AnimatedVisibility(visible = showButton) {
                FloatingActionButton(onClick = {
                    coroutineScope.launch {
                        lazyListState.animateScrollToItem(0)
                    }
                }) {
                    Icon(Icons.Filled.ArrowUpward, contentDescription = "Scroll to top")
                }
            }
        }
    ) { paddingValues ->
        when (state) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is UiState.Success -> {
                val seriesInfo = state.data.info
                val episodes = state.data.episodes

                val seasons = remember(episodes) { episodes?.keys?.toList() ?: emptyList() }
                var selectedSeason by remember(seasons) { mutableStateOf(seasons.firstOrNull() ?: "") }

                val episodesForSelectedSeason = remember(selectedSeason, episodes) {
                    episodes?.get(selectedSeason) ?: emptyList()
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = seriesInfo?.cover,
                        contentDescription = seriesInfo?.name,
                        modifier = Modifier.height(400.dp).fillMaxWidth(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                                    startY = 600f
                                )
                            )
                    )
                    androidx.compose.animation.AnimatedVisibility(
                        visible = true,
                        enter = androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(500)) + 
                                androidx.compose.animation.slideInVertically(initialOffsetY = { it / 4 }, animationSpec = androidx.compose.animation.core.tween(500))
                    ) {
                        LazyColumn(
                            state = lazyListState,
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Transparent)
                                .padding(paddingValues)
                        ) {
                        item { Spacer(modifier = Modifier.height(250.dp)) }
                        item {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    seriesInfo?.name ?: "",
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    val details = listOf(
                                        seriesInfo?.releaseDate,
                                        "TV-MA",
                                        seriesInfo?.genre,
                                        "${episodes?.size ?: 0} Seasons"
                                    )
                                    details.forEach {
                                        if (it?.isNotEmpty() == true) {
                                            AssistChip(onClick = { /*TODO*/ }, label = { Text(it) })
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(seriesInfo?.plot ?: "", maxLines = 4, overflow = TextOverflow.Ellipsis)
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        val allEpisodes = episodes?.values?.flatten() ?: emptyList()
                                        val lastWatchedEpisode = continueWatchingItems
                                            .filter { item -> allEpisodes.any { it.id == item.streamId.toString() } }
                                            .maxByOrNull { it.position }

                                        val episodeToPlay = if (lastWatchedEpisode != null) {
                                            allEpisodes.find { it.id == lastWatchedEpisode.streamId.toString() }
                                        } else {
                                            allEpisodes.firstOrNull()
                                        }

                                        val position = lastWatchedEpisode?.position ?: 0

                                        if (episodeToPlay != null) {
                                            val intent = Intent(context, PlayerActivity::class.java).apply {
                                                putExtra(
                                                    "url",
                                                    "${SessionManager.serverUrl}/series/${SessionManager.username}/${SessionManager.password}/${episodeToPlay.id}.${episodeToPlay.containerExtension}"
                                                )
                                                putExtra("title", episodeToPlay.title)
                                                putExtra("position", position)
                                            }
                                            context.startActivity(intent)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Play")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Continue Watching")
                                }
                            }
                        }
                        item { ActionButtons() }
                        item { DetailTabs() }
                        item {
                            SeasonSelector(
                                seasons = seasons,
                                selectedSeason = selectedSeason,
                                onSeasonSelected = { selectedSeason = it },
                                onDownloadSeason = {
                                    DownloadHelper.downloadSeason(context, episodesForSelectedSeason, seriesInfo?.name ?: "")
                                }
                            )
                        }
                        itemsIndexed(episodesForSelectedSeason, key = { _, episode -> episode.id }) { index, episode ->
                            val continueWatchingItem = continueWatchingItems.find { it.streamId.toString() == episode.id }
                            EpisodeItem(
                                episode = episode,
                                episodeNumber = index + 1,
                                continueWatchingItem = continueWatchingItem,
                                onDownload = {
                                    DownloadHelper.startDownload(context, episode, seriesInfo?.name ?: "")
                                },
                                onClick = {
                                    val intent = Intent(context, PlayerActivity::class.java).apply {
                                        putExtra(
                                            "url",
                                            "${SessionManager.serverUrl}/series/${SessionManager.username}/${SessionManager.password}/${episode.id}.${episode.containerExtension}"
                                        )
                                        putExtra("title", episode.title)
                                        continueWatchingItem?.let {
                                            putExtra("position", it.position)
                                        }
                                    }
                                    context.startActivity(intent)
                                }
                            )
                        }
                    }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                        Row {
                            IconButton(
                                onClick = { /*TODO*/ },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.5f))
                            ) {
                                Icon(Icons.Default.Cast, contentDescription = "Cast", tint = Color.White)
                            }
                            IconButton(
                                onClick = { context.startActivity(Intent(context, SearchActivity::class.java)) },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.5f))
                            ) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }

            }

            is UiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(state.message)
                }
            }
        }
    }
}

@Composable
fun ActionButtons() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        val actions = listOf("Watchlist", "Download", "Share")
        val icons = listOf(Icons.Default.Add, Icons.Default.Download, Icons.Default.Share)
        actions.forEachIndexed { index, action ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                OutlinedButton(
                    onClick = { /* TODO */ },
                    shape = CircleShape,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(icons[index], contentDescription = action)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(action, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailTabs() {
    val tabs = listOf("Episodes", "Details", "More Like This")
    var selectedTab by remember { mutableStateOf(tabs.first()) }
    PrimaryTabRow(selectedTabIndex = tabs.indexOf(selectedTab)) {
        tabs.forEach { tab ->
            Tab(
                selected = tab == selectedTab,
                onClick = { selectedTab = tab },
                text = { Text(tab) }
            )
        }
    }
}

@Composable
fun SeasonSelector(
    seasons: List<String>,
    selectedSeason: String,
    onSeasonSelected: (String) -> Unit,
    onDownloadSeason: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                OutlinedButton(
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(selectedSeason, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Select season")
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    seasons.forEach { season ->
                        DropdownMenuItem(
                            text = { Text(season) },
                            onClick = {
                                onSeasonSelected(season)
                                expanded = false
                            }
                        )
                    }
                }
            }
            
            OutlinedButton(
                onClick = onDownloadSeason,
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.DownloadForOffline, contentDescription = "Download Season")
            }
        }
    }
}

@Composable
fun EpisodeItem(
    episode: Episode,
    episodeNumber: Int,
    continueWatchingItem: ContinueWatchingItem?,
    onDownload: () -> Unit,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .aspectRatio(16 / 9f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Gray),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = episode.info?.movieImage,
                    contentDescription = episode.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    error = androidx.compose.ui.res.painterResource(R.drawable.ic_placeholder),
                    placeholder = androidx.compose.ui.res.painterResource(R.drawable.ic_placeholder)
                )
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Play episode",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(episode.title ?: "", style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "S${episode.season ?: 1} E${episodeNumber}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            IconButton(onClick = onDownload) {
                Icon(Icons.Default.DownloadForOffline, contentDescription = "Download")
            }
        }
        continueWatchingItem?.let {
            if (it.duration > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = it.position.toFloat() / it.duration.toFloat(),
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                )
            }
        }
    }
}
