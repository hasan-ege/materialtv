package com.hasanege.materialtv.ui.screens.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.content.Intent
import android.provider.Settings
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.hasanege.materialtv.BuildConfig
import com.hasanege.materialtv.R
import com.hasanege.materialtv.data.ProfilePreferences
import com.hasanege.materialtv.data.SettingsRepository
import com.hasanege.materialtv.ui.theme.ExpressiveShapes
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { SettingsRepository.getInstance(context) }
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(repository))
    
    // Profile customization
    val profilePreferences = remember { ProfilePreferences(context) }
    val profileName by profilePreferences.profileName.collectAsState(initial = "User")
    val profileImageUrl by profilePreferences.profileImageUrl.collectAsState(initial = "")
    var showEditProfileNameDialog by remember { mutableStateOf(false) }
    
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch { profilePreferences.setProfileImageFromUri(it.toString()) }
        }
    }

    val maxConcurrentDownloads by viewModel.maxConcurrentDownloads.collectAsState()
    val useVlcForDownloads by viewModel.useVlcForDownloads.collectAsState()
    val downloadAlgorithm by viewModel.downloadAlgorithm.collectAsState()
    val defaultPlayer by viewModel.defaultPlayer.collectAsState()
    val statsForNerds by viewModel.statsForNerds.collectAsState()
    val experimentalDownloadReconnect by viewModel.experimentalDownloadReconnect.collectAsState()
    val autoRetryFailedDownloads by viewModel.autoRetryFailedDownloads.collectAsState()
    val language by viewModel.language.collectAsState()
    val autoRestartOnSpeedDrop by viewModel.autoRestartOnSpeedDrop.collectAsState()
    var showDefaultPlayerDialog by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    
    // Animation state
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }
    
    // Profile name edit dialog
    if (showEditProfileNameDialog) {
        ProfileNameEditDialog(
            currentName = profileName,
            onDismiss = { showEditProfileNameDialog = false },
            onSave = { newName ->
                scope.launch {
                    profilePreferences.setProfileName(newName)
                    showEditProfileNameDialog = false
                }
            }
        )
    }

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
        ExpressiveSelectionDialog(
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
        ExpressiveSelectionDialog(
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
                    Text(stringResource(R.string.settings_clear_history_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text(stringResource(R.string.settings_cancel))
                }
            },
            shape = ExpressiveShapes.ExtraLarge
        )
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { 
                    Text(
                        stringResource(R.string.settings_title),
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    )
                )
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile Settings Section
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(delayMillis = 0)) +
                        slideInVertically(initialOffsetY = { it / 4 })
            ) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = ExpressiveShapes.Large,
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = stringResource(R.string.settings_profile),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        // Profile Preview
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Avatar
                            Box(
                                modifier = Modifier.size(64.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                )
                                if (profileImageUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(profileImageUrl)
                                            .crossfade(300)
                                            .memoryCachePolicy(CachePolicy.DISABLED)
                                            .diskCachePolicy(CachePolicy.DISABLED)
                                            .build(),
                                        contentDescription = "Profile",
                                        modifier = Modifier
                                            .size(58.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(58.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = profileName.firstOrNull()?.uppercase() ?: "U",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }
                            }
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = profileName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(R.string.settings_profile_edit_subtitle),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Edit name
                        ExpressiveSettingValueItem(
                            icon = Icons.Default.Edit,
                            title = stringResource(R.string.settings_profile_name),
                            value = profileName,
                            onClick = { showEditProfileNameDialog = true }
                        )
                        
                        // Change photo
                        ExpressiveSettingValueItem(
                            icon = Icons.Default.CameraAlt,
                            title = stringResource(R.string.settings_profile_photo),
                            value = if (profileImageUrl.isNotBlank()) stringResource(R.string.settings_profile_photo_change) else stringResource(R.string.settings_profile_photo_add),
                            onClick = { imagePickerLauncher.launch("image/*") }
                        )
                    }
                }
            }
            
            // Download Settings Section
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(delayMillis = 50)) +
                        slideInVertically(initialOffsetY = { it / 4 })
            ) {
                SettingsSection(
                    title = stringResource(R.string.settings_downloads),
                    icon = Icons.Default.Download
                ) {
                    ExpressiveSettingSwitchItem(
                        icon = Icons.Default.VideoLibrary,
                        title = stringResource(R.string.settings_use_vlc_downloads),
                        checked = useVlcForDownloads,
                        onCheckedChange = { viewModel.setUseVlcForDownloads(it) }
                    )
                    
                    // Concurrent downloads limit slider
                    val maxConcurrentDownloads by viewModel.maxConcurrentDownloads.collectAsState()
                    var sliderValue by remember(maxConcurrentDownloads) { 
                        mutableStateOf(maxConcurrentDownloads.toFloat()) 
                    }
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Eşzamanlı İndirme Limiti",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                            Text(
                                text = "${sliderValue.toInt()}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Slider(
                            value = sliderValue,
                            onValueChange = { sliderValue = it },
                            onValueChangeFinished = {
                                viewModel.setMaxConcurrentDownloads(sliderValue.toInt())
                            },
                            valueRange = 1f..5f,
                            steps = 3, // 1, 2, 3, 4, 5
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Text(
                            text = if (sliderValue.toInt() == 1) 
                                "Tek seferde 1 indirme, diğerleri sıraya alınır" 
                            else 
                                "Aynı anda ${sliderValue.toInt()} indirme aktif olabilir",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    ExpressiveSettingValueItem(
                        icon = Icons.Default.BatteryChargingFull,
                        title = stringResource(R.string.settings_battery_optimization),
                        value = stringResource(R.string.settings_battery_optimization),
                        onClick = {
                            // Open battery optimization dialog for just this app
                            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                // Fallback to general battery settings if app-specific fails
                                context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                            }
                        }
                    )
                    
                    // Auto-restart on speed drop toggle
                    ExpressiveSettingSwitchItem(
                        icon = Icons.Default.Refresh,
                        title = stringResource(R.string.settings_auto_restart_speed_drop),
                        checked = autoRestartOnSpeedDrop,
                        onCheckedChange = { viewModel.setAutoRestartOnSpeedDrop(it) }
                    )
                    
                    // Speed threshold slider (only visible when auto-restart is enabled)
                    AnimatedVisibility(visible = autoRestartOnSpeedDrop) {
                        val minSpeedKbps by viewModel.minDownloadSpeedKbps.collectAsState()
                        // Slider uses log scale for better UX: 50KB/s to 5MB/s
                        // Values: 50, 100, 200, 500, 1000, 2000, 5000 KB/s
                        val speedOptions = listOf(50, 100, 200, 500, 1000, 2000, 5000)
                        val currentIndex = speedOptions.indexOfFirst { it >= minSpeedKbps }.takeIf { it >= 0 } ?: 0
                        var sliderValue by remember(minSpeedKbps) { 
                            mutableStateOf(currentIndex.toFloat()) 
                        }
                        
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Speed,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = stringResource(R.string.settings_min_speed_threshold),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                                // Show speed in KB/s or MB/s
                                val speedValue = speedOptions[sliderValue.toInt()]
                                val displaySpeed = if (speedValue >= 1000) {
                                    "${speedValue / 1000} MB/s"
                                } else {
                                    "$speedValue KB/s"
                                }
                                Text(
                                    text = displaySpeed,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            Slider(
                                value = sliderValue,
                                onValueChange = { sliderValue = it },
                                onValueChangeFinished = {
                                    viewModel.setMinDownloadSpeedKbps(speedOptions[sliderValue.toInt()])
                                },
                                valueRange = 0f..(speedOptions.size - 1).toFloat(),
                                steps = speedOptions.size - 2,
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            Text(
                                text = stringResource(R.string.settings_min_speed_description),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    // Speed restart delay slider (only visible when auto-restart is enabled)
                    AnimatedVisibility(visible = autoRestartOnSpeedDrop) {
                        val restartDelaySeconds by viewModel.speedRestartDelaySeconds.collectAsState()
                        var delaySliderValue by remember(restartDelaySeconds) { 
                            mutableStateOf(restartDelaySeconds.toFloat()) 
                        }
                        
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Timer,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = stringResource(R.string.settings_restart_delay),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                                Text(
                                    text = "${delaySliderValue.toInt()} sn",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            Slider(
                                value = delaySliderValue,
                                onValueChange = { delaySliderValue = it },
                                onValueChangeFinished = {
                                    viewModel.setSpeedRestartDelaySeconds(delaySliderValue.toInt())
                                },
                                valueRange = 5f..120f,
                                steps = 22, // 5s increments: 5, 10, 15, ... 120
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            Text(
                                text = stringResource(R.string.settings_restart_delay_description),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Player Settings Section
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(delayMillis = 100)) +
                        slideInVertically(initialOffsetY = { it / 4 })
            ) {
                SettingsSection(
                    title = stringResource(R.string.settings_player),
                    icon = Icons.Default.PlayCircle
                ) {
                    ExpressiveSettingValueItem(
                        icon = Icons.Default.PlayArrow,
                        title = stringResource(R.string.settings_default_player),
                        value = when(defaultPlayer) {
                            com.hasanege.materialtv.data.PlayerPreference.EXOPLAYER -> stringResource(R.string.settings_exoplayer)
                            com.hasanege.materialtv.data.PlayerPreference.VLC -> stringResource(R.string.settings_vlc)
                            com.hasanege.materialtv.data.PlayerPreference.HYBRID -> stringResource(R.string.settings_player_hybrid)
                        },
                        onClick = { showDefaultPlayerDialog = true }
                    )
                    
                    // Next Episode Threshold Slider
                    val thresholdMinutes by viewModel.nextEpisodeThresholdMinutes.collectAsState()
                    var thresholdSliderValue by remember(thresholdMinutes) { 
                        mutableStateOf(thresholdMinutes.toFloat()) 
                    }
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                         Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipNext,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Sonraki Bölüm Eşiği",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                            Text(
                                text = "${thresholdSliderValue.toInt()} dk",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Slider(
                            value = thresholdSliderValue,
                            onValueChange = { thresholdSliderValue = it },
                            onValueChangeFinished = {
                                viewModel.setNextEpisodeThresholdMinutes(thresholdSliderValue.toInt())
                            },
                            valueRange = 1f..10f,
                            steps = 8, // 1 to 10 (9 steps in between)
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Text(
                            text = "Bölüm sonuna ${thresholdSliderValue.toInt()} dakika kala sonrakine geçme önerisi çıkar",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    ExpressiveSettingSwitchItem(
                        icon = Icons.Default.Analytics,
                        title = stringResource(R.string.settings_stats_for_nerds),
                        checked = statsForNerds,
                        onCheckedChange = { viewModel.setStatsForNerds(it) }
                    )
                }
            }
            
            // General Settings Section
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(delayMillis = 200)) +
                        slideInVertically(initialOffsetY = { it / 4 })
            ) {
                SettingsSection(
                    title = stringResource(R.string.settings_general),
                    icon = Icons.Default.Settings
                ) {
                    val languageLabel = when (language) {
                        "en" -> stringResource(R.string.settings_language_option_english)
                        "tr" -> stringResource(R.string.settings_language_option_turkish)
                        else -> stringResource(R.string.settings_language_option_system)
                    }
                    
                    ExpressiveSettingValueItem(
                        icon = Icons.Default.Language,
                        title = stringResource(R.string.settings_language),
                        value = languageLabel,
                        onClick = { showLanguageDialog = true }
                    )
                    
                    var showStartPageDialog by remember { mutableStateOf(false) }
                    val startPage by viewModel.startPage.collectAsState()
                    
                    // Get localized labels for start page options
                    val homeLabel = stringResource(R.string.nav_home)
                    val favoritesLabel = stringResource(R.string.nav_favorites)
                    val downloadsLabel = stringResource(R.string.nav_downloads)
                    val profileLabel = stringResource(R.string.nav_profile)
                    val startPageTitleLabel = stringResource(R.string.start_page_title)
                    
                    val startPageOptions = listOf(
                        "home" to homeLabel,
                        "favorites" to favoritesLabel,
                        "downloads" to downloadsLabel,
                        "profile" to profileLabel
                    )
                    
                    if (showStartPageDialog) {
                        ExpressiveSelectionDialog(
                            title = startPageTitleLabel,
                            options = startPageOptions.map { it.second },
                            currentValue = startPageOptions.find { it.first == startPage }?.second ?: homeLabel,
                            onDismiss = { showStartPageDialog = false },
                            onSelect = { selected ->
                                val page = startPageOptions.find { it.second == selected }?.first ?: "home"
                                viewModel.setStartPage(page)
                                showStartPageDialog = false
                            }
                        )
                    }
                    
                    ExpressiveSettingValueItem(
                        icon = Icons.Default.Home,
                        title = startPageTitleLabel,
                        value = startPageOptions.find { it.first == startPage }?.second ?: homeLabel,
                        onClick = { showStartPageDialog = true }
                    )
                }
            }

            // Privacy Settings Section
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(delayMillis = 300)) +
                        slideInVertically(initialOffsetY = { it / 4 })
            ) {
                SettingsSection(
                    title = stringResource(R.string.settings_privacy),
                    icon = Icons.Default.Security
                ) {
                    ExpressiveSettingActionItem(
                        icon = Icons.Default.Delete,
                        title = stringResource(R.string.settings_clear_history),
                        isDestructive = true,
                        onClick = { showClearHistoryDialog = true }
                    )
                }
            }

            // About Section
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(delayMillis = 400)) +
                        slideInVertically(initialOffsetY = { it / 4 })
            ) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = ExpressiveShapes.Large,
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(
                                        brush = Brush.linearGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.tertiary
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "MaterialTV",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(R.string.settings_about_version, BuildConfig.VERSION_NAME),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            stringResource(R.string.settings_whats_new_header, "v2.0.0"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            WhatsNewItem(stringResource(R.string.settings_whats_new_profile))
                            WhatsNewItem(stringResource(R.string.settings_whats_new_continue))
                            WhatsNewItem(stringResource(R.string.settings_whats_new_watch_time))
                            WhatsNewItem(stringResource(R.string.settings_whats_new_player))
                            WhatsNewItem(stringResource(R.string.settings_whats_new_technical))
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// Settings Section Card
@Composable
fun SettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = ExpressiveShapes.Large,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            content()
        }
    }
}

// Expressive Switch Setting Item
@Composable
fun ExpressiveSettingSwitchItem(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ExpressiveShapes.Medium)
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(ExpressiveShapes.Medium)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

// Expressive Value Setting Item
@Composable
fun ExpressiveSettingValueItem(
    icon: ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ExpressiveShapes.Medium)
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(ExpressiveShapes.Medium)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// Expressive Action Setting Item
@Composable
fun ExpressiveSettingActionItem(
    icon: ImageVector,
    title: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ExpressiveShapes.Medium)
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(ExpressiveShapes.Medium)
                    .background(
                        if (isDestructive) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isDestructive) MaterialTheme.colorScheme.error
                           else MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isDestructive) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurface
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
    }
}

// What's New Item
@Composable
fun WhatsNewItem(text: String) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "•",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// Expressive Selection Dialog
@Composable
fun ExpressiveSelectionDialog(
    title: String,
    options: List<String>,
    currentValue: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                text = title,
                fontWeight = FontWeight.Bold
            ) 
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(ExpressiveShapes.Medium)
                            .clickable { onSelect(option) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RadioButton(
                            selected = option == currentValue,
                            onClick = null
                        )
                        Text(
                            text = option,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_cancel))
            }
        },
        shape = ExpressiveShapes.ExtraLarge
    )
}

// Profile Name Edit Dialog
@Composable
fun ProfileNameEditDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = ExpressiveShapes.Large,
        title = {
            Text(
                text = stringResource(R.string.settings_profile_name_edit_title),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.settings_profile_name_label)) },
                singleLine = true,
                shape = ExpressiveShapes.Small,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            FilledTonalButton(
                onClick = { if (name.isNotBlank()) onSave(name) },
                shape = ExpressiveShapes.Small
            ) {
                Text(stringResource(R.string.settings_profile_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_profile_cancel))
            }
        }
    )
}
