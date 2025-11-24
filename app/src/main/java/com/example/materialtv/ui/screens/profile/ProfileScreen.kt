
package com.example.materialtv.ui.screens.profile

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.materialtv.MainActivity
import com.example.materialtv.MainApplication
import com.example.materialtv.ProfileViewModel
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.filled.Delete
import com.example.materialtv.SettingsActivity
import com.example.materialtv.network.SessionManager

@Composable
fun ProfileScreen(viewModel: ProfileViewModel) {
    val watchHistory by viewModel.watchHistory.collectAsState()
    val totalWatchTime by viewModel.totalWatchTime.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.loadWatchHistory()
        viewModel.loadPlaylists()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text("Profile", style = MaterialTheme.typography.headlineLarge)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Total Watch Time: ${totalWatchTime / 1000 / 60} minutes")
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    context.startActivity(Intent(context, SettingsActivity::class.java))
                }) {
                    Text("Settings")
                }
                
                Button(onClick = {
                    SessionManager.clear()
                    val credentialsManager = (context.applicationContext as MainApplication).credentialsManager
                    credentialsManager.clearCredentials()
                    val intent = Intent(context, MainActivity::class.java)
                    context.startActivity(intent)
                    (context as? Activity)?.finish()
                }) {
                    Text("Logout")
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            
            Text("Playlists", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(playlists) { playlist ->
            PlaylistRow(playlist = playlist, onDelete = { viewModel.deletePlaylist(it) })
        }
        
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Text("Watch History", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(watchHistory) { item ->
            Text(item.name, modifier = Modifier.padding(vertical = 4.dp))
        }
    }
}

@Composable
fun PlaylistRow(playlist: com.example.materialtv.data.Playlist, onDelete: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(playlist.name, style = MaterialTheme.typography.bodyLarge)
            Text(playlist.type.name, style = MaterialTheme.typography.bodySmall)
        }
        androidx.compose.material3.IconButton(onClick = { onDelete(playlist.id) }) {
            androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Default.Delete, contentDescription = "Delete")
        }
    }
}
