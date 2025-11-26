package com.hasanege.materialtv.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import android.content.Intent
import android.provider.Settings
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hasanege.materialtv.BuildConfig
import com.hasanege.materialtv.data.SettingsRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { SettingsRepository.getInstance(context) }
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(repository))

    val maxConcurrentDownloads by viewModel.maxConcurrentDownloads.collectAsState()
    val useVlcForDownloads by viewModel.useVlcForDownloads.collectAsState()
    val downloadAlgorithm by viewModel.downloadAlgorithm.collectAsState()
    val defaultPlayer by viewModel.defaultPlayer.collectAsState()
    val statsForNerds by viewModel.statsForNerds.collectAsState()
    val language by viewModel.language.collectAsState()
    var showDefaultPlayerDialog by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    if (showLanguageDialog) {
        val options = listOf("System default", "English", "Türkçe")
        val currentLabel = when (language) {
            "en" -> "English"
            "tr" -> "Türkçe"
            else -> "System default"
        }
        SelectionDialog(
            title = "Language",
            options = options,
            currentValue = currentLabel,
            onDismiss = { showLanguageDialog = false },
            onSelect = { selected ->
                val code = when (selected) {
                    "English" -> "en"
                    "Türkçe" -> "tr"
                    else -> "system"
                }
                viewModel.setLanguage(code)
                showLanguageDialog = false
            }
        )
    }

    
    if (showDefaultPlayerDialog) {
        SelectionDialog(
            title = "Default Player",
            options = listOf("ExoPlayer", "VLC", "Hybrid (Recommended)"),
            currentValue = when (defaultPlayer) {
                com.hasanege.materialtv.data.PlayerPreference.EXOPLAYER -> "ExoPlayer"
                com.hasanege.materialtv.data.PlayerPreference.VLC -> "VLC"
                com.hasanege.materialtv.data.PlayerPreference.HYBRID -> "Hybrid (Recommended)"
            },
            onDismiss = { showDefaultPlayerDialog = false },
            onSelect = { selected ->
                val preference = when (selected) {
                    "ExoPlayer" -> com.hasanege.materialtv.data.PlayerPreference.EXOPLAYER
                    "VLC" -> com.hasanege.materialtv.data.PlayerPreference.VLC
                    else -> com.hasanege.materialtv.data.PlayerPreference.HYBRID
                }
                viewModel.setDefaultPlayerPreference(preference)
                showDefaultPlayerDialog = false
            }
        )
    }

    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text("Clear Watch History") },
            text = { Text("Are you sure you want to clear your entire watch history? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearWatchHistory()
                    showClearHistoryDialog = false
                    android.widget.Toast.makeText(context, "History cleared", android.widget.Toast.LENGTH_SHORT).show()
                }) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Download Settings", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Max Concurrent Downloads: ${maxConcurrentDownloads.toInt()}", style = MaterialTheme.typography.bodyLarge)
                    Slider(
                        value = maxConcurrentDownloads.toFloat(),
                        onValueChange = { viewModel.setMaxConcurrentDownloads(it.toInt()) },
                        valueRange = 1f..5f,
                        steps = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                SettingSwitchItem(
                    title = "Use VLC for Downloads",
                    checked = useVlcForDownloads,
                    onCheckedChange = { viewModel.setUseVlcForDownloads(it) }
                )
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                        context.startActivity(intent)
                    },
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Battery Optimization",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Bazı cihazlarda uzun indirmelerin arka planda düzgün çalışması için sistem ayarlarından bu uygulama için batarya optimizasyonunu kapatman gerekebilir.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            val downloadAlgorithm by viewModel.downloadAlgorithm.collectAsState()
            var showDownloadAlgorithmDialog by remember { mutableStateOf(false) }

            if (showDownloadAlgorithmDialog) {
                SelectionDialog(
                    title = "Download Algorithm",
                    options = listOf("OkHttp (Default)", "System Download Manager"),
                    currentValue = when (downloadAlgorithm) {
                        com.hasanege.materialtv.data.DownloadAlgorithm.OKHTTP -> "OkHttp (Default)"
                        com.hasanege.materialtv.data.DownloadAlgorithm.SYSTEM_DOWNLOAD_MANAGER -> "System Download Manager"
                    },
                    onDismiss = { showDownloadAlgorithmDialog = false },
                    onSelect = { selected ->
                        val algorithm = when (selected) {
                            "OkHttp (Default)" -> com.hasanege.materialtv.data.DownloadAlgorithm.OKHTTP
                            else -> com.hasanege.materialtv.data.DownloadAlgorithm.SYSTEM_DOWNLOAD_MANAGER
                        }
                        viewModel.setDownloadAlgorithm(algorithm)
                        showDownloadAlgorithmDialog = false
                    }
                )
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDownloadAlgorithmDialog = true },
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                SettingValueItem(
                    title = "Download Algorithm", 
                    value = when(downloadAlgorithm) {
                        com.hasanege.materialtv.data.DownloadAlgorithm.OKHTTP -> "OkHttp"
                        com.hasanege.materialtv.data.DownloadAlgorithm.SYSTEM_DOWNLOAD_MANAGER -> "System Manager"
                    }
                )
            }

            Text("Player Settings", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)

            
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDefaultPlayerDialog = true },
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                SettingValueItem(title = "Default Player", value = when(defaultPlayer) {
                    com.hasanege.materialtv.data.PlayerPreference.EXOPLAYER -> "ExoPlayer"
                    com.hasanege.materialtv.data.PlayerPreference.VLC -> "VLC"
                    com.hasanege.materialtv.data.PlayerPreference.HYBRID -> "Hybrid (Recommended)"
                })
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showLanguageDialog = true },
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                val languageLabel = when (language) {
                    "en" -> "English"
                    "tr" -> "Türkçe"
                    else -> "System default"
                }
                SettingValueItem(title = "Language", value = languageLabel)
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                SettingSwitchItem(
                    title = "Stats for Nerds",
                    checked = statsForNerds,
                    onCheckedChange = { viewModel.setStatsForNerds(it) }
                )
            }

            Text("Privacy", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showClearHistoryDialog = true },
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Clear Watch History", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
                }
            }
            // About Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Version: ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("What's New in v2.0.0:", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("✨ Profile Management:\n• Fixed profile name editing\n• Instant UI updates\n• Persistent profile settings", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("🔄 Continue Watching:\n• Permanent removal system\n• Items stay removed after updates\n• Auto-restore when rewatched", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("⏱️ Watch Time Display:\n• Turkish format (Gün:Sa:DK:SN)\n• Smart time formatting\n• Accurate duration tracking", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("🎚️ Player Improvements:\n• Fixed slider freezing\n• Smooth seeking experience\n• Optimized performance", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("🔧 Technical Updates:\n• Reactive StateFlow architecture\n• Real-time UI updates\n• Enhanced memory management", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
fun SettingSwitchItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun SettingValueItem(title: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun SelectionDialog(
    title: String,
    options: List<String>,
    currentValue: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .toggleable(
                                value = (option == currentValue),
                                onValueChange = { onSelect(option) },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (option == currentValue),
                            onClick = null
                        )
                        Spacer(modifier = Modifier.padding(8.dp))
                        Text(text = option, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
