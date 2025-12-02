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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import android.content.Intent
import android.provider.Settings
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hasanege.materialtv.BuildConfig
import com.hasanege.materialtv.R
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
    val experimentalDownloadReconnect by viewModel.experimentalDownloadReconnect.collectAsState()
    val autoRetryFailedDownloads by viewModel.autoRetryFailedDownloads.collectAsState()
    val language by viewModel.language.collectAsState()
    var showDefaultPlayerDialog by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    if (showLanguageDialog) {
        val systemLanguageLabel = stringResource(R.string.settings_language_option_system)
        val englishLanguageLabel = stringResource(R.string.settings_language_option_english)
        val turkishLanguageLabel = stringResource(R.string.settings_language_option_turkish)
        val options = listOf(systemLanguageLabel, englishLanguageLabel, turkishLanguageLabel)
        val currentLabel = when (language) {
            "en" -> englishLanguageLabel
            "tr" -> turkishLanguageLabel
            else -> systemLanguageLabel
        }
        SelectionDialog(
            title = stringResource(R.string.settings_language_dialog_title),
            options = options,
            currentValue = currentLabel,
            onDismiss = { showLanguageDialog = false },
            onSelect = { selected ->
                val code = when (selected) {
                    englishLanguageLabel -> "en"
                    turkishLanguageLabel -> "tr"
                    else -> "system"
                }
                viewModel.setLanguage(code)
                showLanguageDialog = false
            }
        )
    }

    
    if (showDefaultPlayerDialog) {
        val exoplayerLabel = stringResource(R.string.settings_exoplayer)
        val vlcLabel = stringResource(R.string.settings_vlc)
        val hybridLabel = stringResource(R.string.settings_player_hybrid)
        SelectionDialog(
            title = stringResource(R.string.settings_default_player),
            options = listOf(exoplayerLabel, vlcLabel, hybridLabel),
            currentValue = when (defaultPlayer) {
                com.hasanege.materialtv.data.PlayerPreference.EXOPLAYER -> exoplayerLabel
                com.hasanege.materialtv.data.PlayerPreference.VLC -> vlcLabel
                com.hasanege.materialtv.data.PlayerPreference.HYBRID -> hybridLabel
            },
            onDismiss = { showDefaultPlayerDialog = false },
            onSelect = { selected ->
                val preference = when (selected) {
                    exoplayerLabel -> com.hasanege.materialtv.data.PlayerPreference.EXOPLAYER
                    vlcLabel -> com.hasanege.materialtv.data.PlayerPreference.VLC
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
            title = { Text(stringResource(R.string.settings_clear_history)) },
            text = { Text(stringResource(R.string.settings_clear_history_dialog_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearWatchHistory()
                    showClearHistoryDialog = false
                    android.widget.Toast.makeText(context, context.getString(R.string.settings_history_cleared), android.widget.Toast.LENGTH_SHORT).show()
                }) {
                    Text(stringResource(R.string.settings_clear_history_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text(stringResource(R.string.settings_cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.action_back))
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
            Text(stringResource(R.string.settings_download_settings), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        stringResource(R.string.settings_max_concurrent_downloads, maxConcurrentDownloads),
                        style = MaterialTheme.typography.bodyLarge
                    )
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
                    title = stringResource(R.string.settings_use_vlc_downloads),
                    checked = useVlcForDownloads,
                    onCheckedChange = { viewModel.setUseVlcForDownloads(it) }
                )
            }

            // Maksimum eşzamanlı indirme sayısı
            var showMaxConcurrentDialog by remember { mutableStateOf(false) }
            val maxConcurrentOptions = (1..5).map { it.toString() }
            val currentMaxConcurrent = maxConcurrentDownloads.toString()

            if (showMaxConcurrentDialog) {
                SelectionDialog(
                    title = "Maksimum Eşzamanlı İndirme",
                    options = maxConcurrentOptions,
                    currentValue = currentMaxConcurrent,
                    onDismiss = { showMaxConcurrentDialog = false },
                    onSelect = { selected ->
                        viewModel.setMaxConcurrentDownloads(selected.toInt())
                        showMaxConcurrentDialog = false
                    }
                )
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showMaxConcurrentDialog = true },
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                SettingValueItem(
                    title = "Maksimum Eşzamanlı İndirme",
                    value = "$currentMaxConcurrent indirme"
                )
            }

            // Experimental: download reconnect (speed bypass)
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                SettingSwitchItem(
                    title = stringResource(R.string.settings_experimental_reconnect),
                    checked = experimentalDownloadReconnect,
                    onCheckedChange = { viewModel.setExperimentalDownloadReconnect(it) }
                )
            }

            // Auto retry failed downloads
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                SettingSwitchItem(
                    title = "Auto Retry Failed Downloads",
                    checked = autoRetryFailedDownloads,
                    onCheckedChange = { viewModel.setAutoRetryFailedDownloads(it) }
                )
            }

            // FFmpeg Downloader Toggle
            val useFFmpegDownloader by viewModel.useFFmpegDownloader.collectAsState()
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                SettingSwitchItem(
                    title = "Use FFmpeg Downloader (BETA)",
                    checked = useFFmpegDownloader,
                    onCheckedChange = { viewModel.setUseFFmpegDownloader(it) }
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
                        text = stringResource(R.string.settings_battery_optimization),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.settings_battery_optimization),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            var showDownloadAlgorithmDialog by remember { mutableStateOf(false) }
            val okhttpLabel = stringResource(R.string.settings_download_algorithm_option_okhttp)
            val systemManagerLabel = stringResource(R.string.settings_download_algorithm_option_system)

            if (showDownloadAlgorithmDialog) {
                SelectionDialog(
                    title = stringResource(R.string.settings_download_algorithm_dialog_title),
                    options = listOf(okhttpLabel, systemManagerLabel),
                    currentValue = when (downloadAlgorithm) {
                        com.hasanege.materialtv.data.DownloadAlgorithm.OKHTTP -> okhttpLabel
                        com.hasanege.materialtv.data.DownloadAlgorithm.SYSTEM_DOWNLOAD_MANAGER -> systemManagerLabel
                    },
                    onDismiss = { showDownloadAlgorithmDialog = false },
                    onSelect = { selected ->
                        val algorithm = when (selected) {
                            okhttpLabel -> com.hasanege.materialtv.data.DownloadAlgorithm.OKHTTP
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
                    title = stringResource(R.string.settings_download_algorithm), 
                    value = when(downloadAlgorithm) {
                        com.hasanege.materialtv.data.DownloadAlgorithm.OKHTTP -> stringResource(R.string.settings_algo_okhttp)
                        com.hasanege.materialtv.data.DownloadAlgorithm.SYSTEM_DOWNLOAD_MANAGER -> stringResource(R.string.settings_algo_system)
                    }
                )
            }

            Text(stringResource(R.string.settings_player), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)

            
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDefaultPlayerDialog = true },
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                SettingValueItem(title = stringResource(R.string.settings_default_player), value = when(defaultPlayer) {
                    com.hasanege.materialtv.data.PlayerPreference.EXOPLAYER -> stringResource(R.string.settings_exoplayer)
                    com.hasanege.materialtv.data.PlayerPreference.VLC -> stringResource(R.string.settings_vlc)
                    com.hasanege.materialtv.data.PlayerPreference.HYBRID -> stringResource(R.string.settings_player_hybrid)
                })
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showLanguageDialog = true },
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                val languageLabel = when (language) {
                    "en" -> stringResource(R.string.settings_language_option_english)
                    "tr" -> stringResource(R.string.settings_language_option_turkish)
                    else -> stringResource(R.string.settings_language_option_system)
                }
                SettingValueItem(title = stringResource(R.string.settings_language), value = languageLabel)
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                SettingSwitchItem(
                    title = stringResource(R.string.settings_stats_for_nerds),
                    checked = statsForNerds,
                    onCheckedChange = { viewModel.setStatsForNerds(it) }
                )
            }

            Text(stringResource(R.string.settings_privacy), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)

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
                    Text(stringResource(R.string.settings_clear_history), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
                }
            }
            // About Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.settings_about_version, BuildConfig.VERSION_NAME), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(R.string.settings_whats_new_header, "v2.0.0"), style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(stringResource(R.string.settings_whats_new_profile), style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(stringResource(R.string.settings_whats_new_continue), style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(stringResource(R.string.settings_whats_new_watch_time), style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(stringResource(R.string.settings_whats_new_player), style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(stringResource(R.string.settings_whats_new_technical), style = MaterialTheme.typography.bodyMedium)
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
