
package com.hasanege.materialtv.ui.screens.profile

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.hasanege.materialtv.MainActivity
import com.hasanege.materialtv.MainApplication
import com.hasanege.materialtv.ProfileViewModel
import com.hasanege.materialtv.SettingsActivity
import com.hasanege.materialtv.network.SessionManager
import com.hasanege.materialtv.ui.components.ChipSurface

@Composable
fun ProfileScreen(viewModel: ProfileViewModel) {
    val watchHistory by viewModel.watchHistory.collectAsState()
    val totalWatchTime by viewModel.totalWatchTime.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val context = LocalContext.current
    var showAddPlaylistDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadWatchHistory()
        viewModel.loadPlaylists()
    }

    if (showAddPlaylistDialog) {
        AddPlaylistDialog(
            onDismiss = { showAddPlaylistDialog = false },
            onConfirm = { name ->
                viewModel.addPlaylist(name)
                showAddPlaylistDialog = false
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Profile", style = MaterialTheme.typography.headlineLarge)
            Spacer(modifier = Modifier.height(8.dp))
            
            ChipSurface(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Total Watch Time", style = MaterialTheme.typography.labelLarge)
                    Text("${totalWatchTime / 1000 / 60} minutes", style = MaterialTheme.typography.headlineMedium)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChipSurface(
                    modifier = Modifier.weight(1f),
                    onClick = { context.startActivity(Intent(context, SettingsActivity::class.java)) }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null)
                        Spacer(modifier = Modifier.padding(4.dp))
                        Text("Settings")
                    }
                }
                
                ChipSurface(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        SessionManager.clear()
                        val credentialsManager = (context.applicationContext as MainApplication).credentialsManager
                        credentialsManager.clearCredentials()
                        val intent = Intent(context, MainActivity::class.java)
                        context.startActivity(intent)
                        (context as? Activity)?.finish()
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("Logout")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Playlists", style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = { showAddPlaylistDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Playlist")
                }
            }
        }

        items(playlists) { playlist ->
            ChipSurface(modifier = Modifier.fillMaxWidth()) {
                PlaylistRow(playlist = playlist, onDelete = { viewModel.deletePlaylist(it) })
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Text("Watch History", style = MaterialTheme.typography.titleLarge)
        }

        items(watchHistory) { item ->
            ChipSurface(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = item.name,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
fun PlaylistRow(playlist: com.hasanege.materialtv.data.Playlist, onDelete: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(playlist.name, style = MaterialTheme.typography.titleMedium)
            Text(playlist.type.name, style = MaterialTheme.typography.bodySmall)
        }
        IconButton(onClick = { onDelete(playlist.id) }) {
            Icon(Icons.Default.Delete, contentDescription = "Delete")
        }
    }
}

@Composable
fun AddPlaylistDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Playlist") },
        text = {
            TextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Playlist Name") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { if (text.isNotBlank()) onConfirm(text) }) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}


