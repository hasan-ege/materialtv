package com.hasanege.materialtv.ui

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.hasanege.materialtv.MainScreen
import com.hasanege.materialtv.PlayerActivity
import com.hasanege.materialtv.R
import com.hasanege.materialtv.model.LiveStream
import com.hasanege.materialtv.model.SeriesItem
import com.hasanege.materialtv.model.VodItem
import com.hasanege.materialtv.network.SessionManager

@Composable
fun StreamifyBottomNavBar(items: List<MainScreen>, currentItemRoute: String, onItemClick: (MainScreen) -> Unit, modifier: Modifier = Modifier) {
    NavigationBar(modifier = modifier) {
        items.forEach { screen ->
            NavigationBarItem(
                icon = { Icon(screen.icon, contentDescription = screen.label) },
                label = { Text(screen.label) },
                selected = currentItemRoute == screen.route,
                onClick = { onItemClick(screen) }
            )
        }
    }
}

@Composable
fun CenteredProgressBar() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun ErrorMessage(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = message)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoviesList(movies: List<VodItem>, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    LazyColumn(modifier = modifier.fillMaxSize().padding(16.dp)) {
        items(movies) { movie ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), onClick = {
                val intent = Intent(context, PlayerActivity::class.java).apply {
                    putExtra("STREAM_ID", movie.streamId)
                }
                context.startActivity(intent)
            }) {
                Row(modifier = Modifier.padding(16.dp)) {
                    AsyncImage(
                        model = movie.streamIcon,
                        contentDescription = movie.name ?: "",
                        contentScale = ContentScale.Crop,
                        error = painterResource(R.drawable.ic_placeholder),
                        placeholder = painterResource(R.drawable.ic_placeholder),
                        modifier = Modifier.width(80.dp).aspectRatio(2f / 3f).clip(RoundedCornerShape(8.dp))
                    )
                    Column(modifier = Modifier.padding(start = 16.dp)) {
                        Text(movie.name ?: "", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeriesList(series: List<SeriesItem>, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    LazyColumn(modifier = modifier.fillMaxSize().padding(16.dp)) {
        items(series) { seriesItem ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), onClick = {
                val intent = Intent(context, PlayerActivity::class.java).apply {
                    putExtra("SERIES_ID", seriesItem.seriesId)
                }
                context.startActivity(intent)
            }) {
                Row(modifier = Modifier.padding(16.dp)) {
                    AsyncImage(
                        model = seriesItem.cover,
                        contentDescription = seriesItem.name ?: "",
                        contentScale = ContentScale.Crop,
                        error = painterResource(R.drawable.ic_placeholder),
                        placeholder = painterResource(R.drawable.ic_placeholder),
                        modifier = Modifier.width(80.dp).aspectRatio(2f / 3f).clip(RoundedCornerShape(8.dp))
                    )
                    Column(modifier = Modifier.padding(start = 16.dp)) {
                        Text(seriesItem.name ?: "", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(seriesItem.releaseDate ?: "", style = MaterialTheme.typography.bodySmall)
                        Text(seriesItem.plot ?: "", maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveTVList(liveStreams: List<LiveStream>) {
    val context = LocalContext.current
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        items(liveStreams) { liveStream ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), onClick = {
                // For M3U, get URL from repository; for Xtream, construct it
                val streamUrl = if (SessionManager.loginType == SessionManager.LoginType.M3U) {
                    val url = com.hasanege.materialtv.data.M3uRepository.getStreamUrl(liveStream.streamId ?: 0)
                    android.util.Log.d("LiveTVList", "M3U stream URL for ${liveStream.name}: $url")
                    url
                } else {
                    "${SessionManager.serverUrl}/live/${SessionManager.username}/${SessionManager.password}/${liveStream.streamId}.ts"
                }
                
                if (streamUrl.isNullOrEmpty()) {
                    android.widget.Toast.makeText(context, "Stream URL not found for ${liveStream.name}", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    val intent = Intent(context, PlayerActivity::class.java).apply {
                        putExtra("url", streamUrl)
                        putExtra("TITLE", liveStream.name)
                    }
                    context.startActivity(intent)
                }
            }) {
                Row(modifier = Modifier.padding(16.dp)) {
                    AsyncImage(
                        model = liveStream.streamIcon,
                        contentDescription = liveStream.name ?: "",
                        contentScale = ContentScale.Crop,
                        error = painterResource(R.drawable.ic_placeholder),
                        placeholder = painterResource(R.drawable.ic_placeholder),
                        modifier = Modifier.width(80.dp).aspectRatio(16f/9f).clip(RoundedCornerShape(8.dp))
                    )
                    Column(modifier = Modifier.padding(start = 16.dp)) {
                        Text(liveStream.name ?: "", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
