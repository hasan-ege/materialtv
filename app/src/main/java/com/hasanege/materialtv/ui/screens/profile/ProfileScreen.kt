package com.hasanege.materialtv.ui.screens.profile

import android.app.Activity
import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.hasanege.materialtv.MainActivity
import com.hasanege.materialtv.MainApplication
import com.hasanege.materialtv.ProfileViewModel
import com.hasanege.materialtv.R
import com.hasanege.materialtv.SettingsActivity
import com.hasanege.materialtv.data.ProfilePreferences
import com.hasanege.materialtv.network.SessionManager
import com.hasanege.materialtv.ui.activities.WatchHistoryActivity
import com.hasanege.materialtv.ui.theme.ExpressiveShapes
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@Composable
fun ProfileScreen(viewModel: ProfileViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    
    val profilePreferences = remember { ProfilePreferences(context) }
    val defaultUser = stringResource(R.string.profile_default_user)
    val profileName by profilePreferences.profileName.collectAsState(initial = defaultUser)
    val profileImageUrl by profilePreferences.profileImageUrl.collectAsState(initial = "")
    
    val totalWatchTime by viewModel.totalWatchTime.collectAsState()
    val totalItemsWatched by viewModel.totalItemsWatched.collectAsState()
    val totalMoviesWatched by viewModel.totalMoviesWatched.collectAsState()
    val totalSeriesWatched by viewModel.totalSeriesWatched.collectAsState()
    
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        viewModel.loadWatchHistory()
        delay(100)
        isVisible = true
    }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Profile Card (read-only, edit from Settings)
        item {
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(300)) + slideInVertically(
                    initialOffsetY = { -40 },
                    animationSpec = spring(dampingRatio = 0.8f)
                )
            ) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = ExpressiveShapes.Large,
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Avatar (read-only)
                        Box(
                            modifier = Modifier.size(110.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(110.dp)
                                    .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            )
                            
                            if (profileImageUrl.isNotBlank()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(profileImageUrl)
                                        .crossfade(300)
                                        .memoryCachePolicy(CachePolicy.DISABLED)
                                        .diskCachePolicy(CachePolicy.DISABLED)
                                        .build(),
                                    contentDescription = stringResource(R.string.profile_title),
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = profileName.firstOrNull()?.uppercase() ?: "U",
                                        style = MaterialTheme.typography.displaySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Name (read-only)
                        Text(
                            text = profileName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Watch time badge
                        Surface(
                            shape = ExpressiveShapes.Full,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.PlayCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = formatWatchTime(totalWatchTime),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // Stats
        item {
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(300, delayMillis = 100)) + slideInVertically(
                    initialOffsetY = { 30 },
                    animationSpec = spring(dampingRatio = 0.8f)
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.Tv,
                        value = totalItemsWatched.toString(),
                        label = stringResource(R.string.profile_stats_watched)
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.Movie,
                        value = totalMoviesWatched.toString(),
                        label = stringResource(R.string.profile_stats_movies)
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.LiveTv,
                        value = totalSeriesWatched.toString(),
                        label = stringResource(R.string.profile_stats_series)
                    )
                }
            }
        }
        
        // Actions header
        item {
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(300, delayMillis = 150))
            ) {
                Text(
                    text = stringResource(R.string.profile_quick_actions),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, start = 4.dp)
                )
            }
        }
        
        // Watch History
        item {
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(300, delayMillis = 200)) + slideInVertically(
                    initialOffsetY = { 30 },
                    animationSpec = spring(dampingRatio = 0.8f)
                )
            ) {
                ActionCard(
                    icon = Icons.Outlined.History,
                    title = stringResource(R.string.profile_watch_history),
                    subtitle = stringResource(R.string.profile_view_history),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        context.startActivity(Intent(context, WatchHistoryActivity::class.java))
                    }
                )
            }
        }
        
        // Settings (profile editing is here)
        item {
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(300, delayMillis = 250)) + slideInVertically(
                    initialOffsetY = { 30 },
                    animationSpec = spring(dampingRatio = 0.8f)
                )
            ) {
                ActionCard(
                    icon = Icons.Outlined.Settings,
                    title = stringResource(R.string.profile_settings),
                    subtitle = stringResource(R.string.profile_settings_subtitle),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        context.startActivity(Intent(context, SettingsActivity::class.java))
                    }
                )
            }
        }
        
        // Logout
        item {
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(300, delayMillis = 300)) + slideInVertically(
                    initialOffsetY = { 30 },
                    animationSpec = spring(dampingRatio = 0.8f)
                )
            ) {
                ActionCard(
                    icon = Icons.AutoMirrored.Filled.Logout,
                    title = stringResource(R.string.profile_logout),
                    subtitle = stringResource(R.string.profile_logout_subtitle),
                    isDestructive = true,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        scope.launch {
                            profilePreferences.setProfileName("")
                            SessionManager.clear()
                            val credentialsManager = (context.applicationContext as MainApplication).credentialsManager
                            credentialsManager.clearCredentials()
                            context.startActivity(Intent(context, MainActivity::class.java))
                            (context as? Activity)?.finish()
                        }
                    }
                )
            }
        }
        
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    value: String,
    label: String
) {
    ElevatedCard(
        modifier = modifier.height(90.dp),
        shape = ExpressiveShapes.Medium,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(ExpressiveShapes.Small)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(16.dp)
                )
            }
            
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = ExpressiveShapes.Medium,
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isDestructive) 
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
            else 
                MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
                        .clip(ExpressiveShapes.Small)
                        .background(
                            if (isDestructive) MaterialTheme.colorScheme.errorContainer
                            else MaterialTheme.colorScheme.primaryContainer
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isDestructive) MaterialTheme.colorScheme.onErrorContainer
                               else MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = if (isDestructive) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private fun formatWatchTime(millis: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(millis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
    
    val hourText = MainApplication.instance.getString(R.string.time_hour_short)
    val minuteText = MainApplication.instance.getString(R.string.time_minute_short)
    
    return when {
        hours > 0 -> "${hours}${hourText} ${minutes}${minuteText}"
        minutes > 0 -> "${minutes}${minuteText}"
        else -> "0${minuteText}"
    }
}
