
package com.hasanege.materialtv.ui.screens.profile

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowRight
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.hasanege.materialtv.MainActivity
import com.hasanege.materialtv.MainApplication
import com.hasanege.materialtv.ProfileViewModel
import com.hasanege.materialtv.SettingsActivity
import com.hasanege.materialtv.network.SessionManager
import com.hasanege.materialtv.ui.components.ChipSurface
import com.hasanege.materialtv.data.ProfilePreferences
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

private fun formatWatchTime(millis: Long): String {
    val days = TimeUnit.MILLISECONDS.toDays(millis)
    val hours = TimeUnit.MILLISECONDS.toHours(millis) % 24
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
    
    return when {
        days > 0 -> "${days}d ${hours}h ${minutes}m ${seconds}s"
        hours > 0 -> "${hours}h ${minutes}m ${seconds}s"
        minutes > 0 -> "${minutes}m ${seconds}s"
        else -> "${seconds}s"
    }
}

@Composable
fun ProfileScreen(viewModel: ProfileViewModel) {
    val totalWatchTime by viewModel.totalWatchTime.collectAsState()
    val context = LocalContext.current
    val profilePreferences = remember { ProfilePreferences.getInstance(context) }
    val scope = rememberCoroutineScope()
    
    var showEditNameDialog by remember { mutableStateOf(false) }
    val profileName by profilePreferences.profileName.collectAsState()
    val profileImageUrl by profilePreferences.profileImageUrl.collectAsState()
    
    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            // Copy image to app storage as pfp.png
            profilePreferences.setProfileImageFromUri(it)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadWatchHistory()
    }

    if (showEditNameDialog) {
        EditNameDialog(
            currentName = profileName,
            onDismiss = { showEditNameDialog = false },
            onConfirm = { newName ->
                profilePreferences.setProfileName(newName)
                showEditNameDialog = false
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
            // Profile Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Profile Image
                Box(
                    modifier = Modifier.size(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (profileImageUrl.isNotBlank()) {
                        Image(
                            painter = rememberAsyncImagePainter(profileImageUrl),
                            contentDescription = "Profile Picture",
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = "Change Profile Picture",
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    // Camera icon overlay
                    IconButton(
                        onClick = { 
                            // Launch image picker to select from gallery
                            imagePickerLauncher.launch("image/*")
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = "Change Profile Picture",
                            modifier = Modifier.size(20.dp),
                            tint = Color.White
                        )
                    }
                }
                
                // Profile Name
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = profileName,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Tap to edit",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { showEditNameDialog = true }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Stats Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Total Watch Time", style = MaterialTheme.typography.labelLarge)
                    Text(formatWatchTime(totalWatchTime), style = MaterialTheme.typography.headlineMedium)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Watch History Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { 
    val intent = android.content.Intent(context, WatchHistoryActivity::class.java)
    context.startActivity(intent)
},
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Watch History",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "View your watching history",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        Icons.Default.ArrowRight,
                        contentDescription = "Open Watch History",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Action Buttons
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Card(
                    modifier = Modifier.weight(1f),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { context.startActivity(Intent(context, SettingsActivity::class.java)) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null)
                        Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                        Text("Settings")
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                scope.launch {
                                    // Clear only profile name on logout, keep profile image permanent
                                    profilePreferences.clearProfile()
                                    
                                    SessionManager.clear()
                                    val credentialsManager = (context.applicationContext as MainApplication).credentialsManager
                                    credentialsManager.clearCredentials()
                                    val intent = Intent(context, MainActivity::class.java)
                                    context.startActivity(intent)
                                    (context as? Activity)?.finish()
                                }
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("Logout")
                    }
                }
            }
        }
    }
}

@Composable
fun EditNameDialog(currentName: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Profile Name") },
        text = {
            TextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Name") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { if (text.isNotBlank()) onConfirm(text) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}


