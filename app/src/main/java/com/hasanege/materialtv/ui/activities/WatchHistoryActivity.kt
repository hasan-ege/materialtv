package com.hasanege.materialtv.ui.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.hasanege.materialtv.PlayerActivity
import com.hasanege.materialtv.WatchHistoryViewModel
import com.hasanege.materialtv.model.ContinueWatchingItem
import com.hasanege.materialtv.ui.theme.MaterialTVTheme
import java.util.concurrent.TimeUnit

class WatchHistoryActivity : ComponentActivity() {
    private val viewModel: WatchHistoryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            MaterialTVTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WatchHistoryScreen(
                        viewModel = viewModel,
                        onBack = { finish() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WatchHistoryScreen(
    viewModel: WatchHistoryViewModel,
    onBack: () -> Unit
) {
    val history by viewModel.history.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Watch History") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (history.isNotEmpty()) {
                        TextButton(onClick = { viewModel.clearHistory() }) {
                            Text("Clear All")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        if (history.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No watch history yet",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(history, key = { "${it.streamId}_${it.type}" }) { item ->
                    HistoryItemCard(
                        item = item,
                        onClick = {
                            // Logic matched with Continue Watching
                            if (item.type == "series") {
                                val seriesIntent = Intent(context, com.hasanege.materialtv.SeriesDetailActivity::class.java).apply {
                                    putExtra("SERIES_ID", item.seriesId)
                                    putExtra("TITLE", item.name)
                                    putExtra("COVER", item.streamIcon)
                                }
                                context.startActivity(seriesIntent)
                            } else {
                                val intent = Intent(context, PlayerActivity::class.java).apply {
                                    putExtra("TITLE", item.name)
                                    putExtra("START_POSITION", item.position)
                                    
                                    if (item.type == "downloaded") {
                                        if (item.episodeId != null && item.episodeId!!.isNotEmpty()) {
                                            putExtra("url", item.episodeId)
                                            putExtra("STREAM_ID", item.streamId)
                                        } else {
                                            putExtra("IS_DOWNLOADED_FILE", true)
                                            putExtra("URI", item.streamIcon)
                                        }
                                    } else if (item.type == "movie") {
                                        putExtra("STREAM_ID", item.streamId)
                                    } else if (item.type == "live") {
                                        if (com.hasanege.materialtv.network.SessionManager.loginType == com.hasanege.materialtv.network.SessionManager.LoginType.M3U) {
                                            val streamUrl = com.hasanege.materialtv.data.M3uRepository.getStreamUrl(item.streamId)
                                            if (streamUrl.isNullOrEmpty()) {
                                                android.widget.Toast.makeText(context, "Stream URL not found for ${item.name}", android.widget.Toast.LENGTH_SHORT).show()
                                                return@HistoryItemCard
                                            }
                                            putExtra("url", streamUrl)
                                        } else {
                                            putExtra("url", "${com.hasanege.materialtv.network.SessionManager.serverUrl}/live/${com.hasanege.materialtv.network.SessionManager.username}/${com.hasanege.materialtv.network.SessionManager.password}/${item.streamId}.ts")
                                        }
                                        putExtra("TITLE", item.name)
                                        putExtra("LIVE_STREAM_ID", item.streamId)
                                        putExtra("STREAM_ICON", item.streamIcon)
                                    } else {
                                        putExtra("STREAM_ID", item.streamId)
                                        putExtra("SERIES_ID", item.seriesId)
                                        putExtra("EPISODE_ID", item.episodeId)
                                    }
                                }
                                context.startActivity(intent)
                            }
                        },
                        onDelete = { viewModel.removeItem(item) }
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryItemCard(
    item: ContinueWatchingItem,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .width(160.dp)
                    .fillMaxHeight()
            ) {
                if (!item.streamIcon.isNullOrEmpty()) {
                    AsyncImage(
                        model = item.streamIcon,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                
                // Progress Bar
                if (item.duration > 0) {
                    val progress = item.position.toFloat() / item.duration.toFloat()
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .align(Alignment.BottomCenter),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    )
                }
            }

            // Info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                val progressPercent = if (item.duration > 0) {
                    (item.position * 100 / item.duration).toInt()
                } else 0
                
                Text(
                    text = "${progressPercent}% Watched",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Delete Button
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remove from history",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
