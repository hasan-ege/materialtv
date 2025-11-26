package com.hasanege.materialtv.ui.screens.downloads

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hasanege.materialtv.DownloadsViewModel
import com.hasanege.materialtv.PlayerActivity
import com.hasanege.materialtv.data.DownloadEntity
import com.hasanege.materialtv.data.DownloadStatus

@Composable
fun DownloadsScreen(viewModel: DownloadsViewModel) {
    val downloads by viewModel.downloads.collectAsState()
    val settingsRepository = com.hasanege.materialtv.data.SettingsRepository.getInstance(LocalContext.current)
    val statsForNerds by settingsRepository.statsForNerds.collectAsState(initial = false)
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (downloads.isEmpty()) {
            EmptyDownloadsView()
        } else {
            DownloadsList(
                downloads = downloads,
                statsForNerds = statsForNerds,
                onDelete = { viewModel.deleteDownload(it) },
                onRetry = { viewModel.retryDownload(it) },
                onPause = { viewModel.pauseDownload(it) },
                onResume = { viewModel.resumeDownload(it) },
                onCancel = { viewModel.cancelDownload(it) }
            )
        }
    }
}

@Composable
private fun EmptyDownloadsView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "No Downloads Yet",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Download your favorite content to watch offline",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DownloadsList(
    downloads: List<DownloadEntity>,
    statsForNerds: Boolean = false,
    onDelete: (DownloadEntity) -> Unit,
    onRetry: (DownloadEntity) -> Unit,
    onPause: (DownloadEntity) -> Unit,
    onResume: (DownloadEntity) -> Unit,
    onCancel: (DownloadEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "Downloads",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
        items(downloads, key = { it.id }) { download ->
            DownloadItem(
                download = download,
                statsForNerds = statsForNerds,
                onDelete = { onDelete(download) },
                onRetry = { onRetry(download) },
                onPause = { onPause(download) },
                onResume = { onResume(download) },
                onCancel = { onCancel(download) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DownloadItem(
    download: DownloadEntity,
    statsForNerds: Boolean = false,
    onDelete: () -> Unit,
    onRetry: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = download.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        StatusBadge(status = download.status)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${download.progress}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                        if (download.downloadSpeed > 0) {
                            Text(
                                text = " • ${
                                    if (statsForNerds) {
                                        formatSpeed(download.downloadSpeed)
                                    } else if (download.fileSize > 0 && download.downloadedBytes < download.fileSize) {
                                        val remainingBytes = download.fileSize - download.downloadedBytes
                                        val timeRemaining = if (download.downloadSpeed > 0) remainingBytes / download.downloadSpeed else 0
                                        formatTime(timeRemaining)
                                    } else {
                                        formatSpeed(download.downloadSpeed)
                                    }
                                }",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    when (download.status) {
                        DownloadStatus.DOWNLOADING -> {
                            IconButton(onClick = onPause) {
                                Icon(
                                    Icons.Default.Pause,
                                    contentDescription = "Pause",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(onClick = onCancel) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Cancel",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        DownloadStatus.PAUSED -> {
                            IconButton(onClick = onResume) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = "Resume",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(onClick = onCancel) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Cancel",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        DownloadStatus.FAILED -> {
                            IconButton(onClick = onRetry) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = "Retry",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        DownloadStatus.COMPLETED -> {
                            IconButton(onClick = {
                                val intent = Intent(context, PlayerActivity::class.java).apply {
                                    putExtra("URI", download.filePath)
                                    putExtra("TITLE", download.title)
                                }
                                context.startActivity(intent)
                            }) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = "Play",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        else -> {}
                    }
                    
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            
            if (download.status == DownloadStatus.DOWNLOADING) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { download.progress / 100f },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(status: DownloadStatus) {
    val color = when (status) {
        DownloadStatus.COMPLETED -> MaterialTheme.colorScheme.primary
        DownloadStatus.DOWNLOADING -> MaterialTheme.colorScheme.tertiary
        DownloadStatus.QUEUED -> MaterialTheme.colorScheme.secondary
        DownloadStatus.PAUSED -> MaterialTheme.colorScheme.surfaceVariant
        DownloadStatus.FAILED -> MaterialTheme.colorScheme.error
        DownloadStatus.CANCELLED -> MaterialTheme.colorScheme.outline
    }
    
    val icon = when (status) {
        DownloadStatus.DOWNLOADING -> Icons.Default.Download
        DownloadStatus.FAILED -> Icons.Default.Error
        DownloadStatus.PAUSED -> Icons.Default.Pause
        else -> null
    }
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = color
            )
        }
        Text(
            text = status.name,
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

private fun formatSpeed(bytesPerSecond: Long): String {
    return when {
        bytesPerSecond < 1024 -> "${bytesPerSecond} B/s"
        bytesPerSecond < 1024 * 1024 -> "${bytesPerSecond / 1024} KB/s"
        else -> String.format("%.1f MB/s", bytesPerSecond / (1024.0 * 1024.0))
    }
}

private fun formatTime(seconds: Long): String {
    return when {
        seconds < 60 -> "${seconds}s"
        seconds < 3600 -> "${seconds / 60}m ${seconds % 60}s"
        seconds < 86400 -> "${seconds / 3600}h ${(seconds % 3600) / 60}m"
        else -> "${seconds / 86400}d ${(seconds % 86400) / 3600}h"
    }
}

