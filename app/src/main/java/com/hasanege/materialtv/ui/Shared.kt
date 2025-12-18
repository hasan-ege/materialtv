package com.hasanege.materialtv.ui

import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ripple
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.hasanege.materialtv.FavoritesManager
import com.hasanege.materialtv.MainScreen
import com.hasanege.materialtv.PlayerActivity
import com.hasanege.materialtv.R
import com.hasanege.materialtv.model.LiveStream
import com.hasanege.materialtv.model.SeriesItem
import com.hasanege.materialtv.model.VodItem
import com.hasanege.materialtv.network.SessionManager
import com.hasanege.materialtv.ui.utils.ImageConfig
import kotlinx.coroutines.launch


// M3 Expressive Pill-Style Tab Slider with Animated Indicator - Floating Island Style
@Composable

fun ExpressiveTabSlider(
    tabs: List<String>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    onTabLongClick: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    
    // Padding values for calculation
    val horizontalPadding = with(density) { 16.dp.toPx() }
    val verticalPadding = with(density) { 8.dp.toPx() }
    val spacing = with(density) { 6.dp.toPx() }
    
    // Track tab text dimensions
    var tabTextSizes by remember(tabs) { mutableStateOf(listOf<Pair<Float, Float>>()) }
    
    // Calculate full tab dimensions including padding
    val fullTabWidths = tabTextSizes.map { it.first + (horizontalPadding * 2) }
    val fullTabHeights = tabTextSizes.map { it.second + (verticalPadding * 2) }
    
    // Animated indicator offset
    val indicatorOffset by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (fullTabWidths.isNotEmpty() && selectedIndex < fullTabWidths.size) {
            fullTabWidths.take(selectedIndex).sum() + (selectedIndex * spacing)
        } else 0f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
        ),
        label = "indicator_offset"
    )
    
    // Animated indicator width
    val indicatorWidth by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (fullTabWidths.isNotEmpty() && selectedIndex < fullTabWidths.size) {
            fullTabWidths[selectedIndex]
        } else 0f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
        ),
        label = "indicator_width"
    )
    
    // Animated indicator height (use max height for consistency)
    val maxHeight = fullTabHeights.maxOrNull() ?: 0f
    val indicatorHeight by androidx.compose.animation.core.animateFloatAsState(
        targetValue = maxHeight,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
        ),
        label = "indicator_height"
    )
    
    // Outer container
    Box(
        modifier = modifier
            .padding(horizontal = 4.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        // Inner floating pill
        Box(
            modifier = Modifier
                .shadow(
                    elevation = 8.dp,
                    shape = com.hasanege.materialtv.ui.theme.ExpressiveShapes.ExtraLarge,
                    ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                )
                .clip(com.hasanege.materialtv.ui.theme.ExpressiveShapes.ExtraLarge)
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = com.hasanege.materialtv.ui.theme.ExpressiveShapes.ExtraLarge
                )
                .padding(6.dp)
        ) {
            // Scrollable container for tabs
            val scrollState = androidx.compose.foundation.rememberScrollState()
            
            // Auto-scroll to selected tab when it changes
            androidx.compose.runtime.LaunchedEffect(selectedIndex, fullTabWidths) {
                if (fullTabWidths.isNotEmpty() && selectedIndex < fullTabWidths.size) {
                    val scrollTarget = when (selectedIndex) {
                        0 -> 0
                        tabs.size - 1 -> scrollState.maxValue
                        else -> {
                            val targetOffset = fullTabWidths.take(selectedIndex).sum() + (selectedIndex * spacing)
                            val tabWidth = fullTabWidths.getOrNull(selectedIndex) ?: 0f
                            (targetOffset - (scrollState.maxValue - tabWidth) / 2).toInt().coerceIn(0, scrollState.maxValue)
                        }
                    }
                    
                    scrollState.animateScrollTo(
                        value = scrollTarget,
                        animationSpec = androidx.compose.animation.core.spring(
                            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                            stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                        )
                    )
                }
            }
            
            Box {
                // Animated sliding indicator behind tabs
                if (indicatorWidth > 0f && indicatorHeight > 0f) {
                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                translationX = indicatorOffset - scrollState.value
                            }
                            .size(
                                width = with(density) { indicatorWidth.toDp() },
                                height = with(density) { indicatorHeight.toDp() }
                            )
                            .clip(com.hasanege.materialtv.ui.theme.ExpressiveShapes.ExtraLarge)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    )
                }
                
                // Horizontal scrollable row
                Row(
                    modifier = Modifier.horizontalScroll(scrollState),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEachIndexed { index, title ->
                        val isSelected = selectedIndex == index
                        
                        Box(
                            modifier = Modifier
                                .clip(com.hasanege.materialtv.ui.theme.ExpressiveShapes.ExtraLarge)
                                .combinedClickable(
                                    onClick = {
                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                        onTabSelected(index)
                                    },
                                    onLongClick = if (onTabLongClick != null) {
                                        {
                                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                            onTabLongClick(index)
                                        }
                                    } else null
                                )
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                       else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                modifier = Modifier.onGloballyPositioned { coordinates ->
                                    val width = coordinates.size.width.toFloat()
                                    val height = coordinates.size.height.toFloat()
                                    if (tabTextSizes.size != tabs.size) {
                                        tabTextSizes = List(tabs.size) { Pair(0f, 0f) }
                                    }
                                    if (tabTextSizes.getOrNull(index) != Pair(width, height)) {
                                        tabTextSizes = tabTextSizes.toMutableList().also { 
                                            it[index] = Pair(width, height) 
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StreamifyBottomNavBar(items: List<MainScreen>, currentItemRoute: String, onItemClick: (MainScreen) -> Unit, modifier: Modifier = Modifier) {
    // Rounded pill bottom navigation - subtle background
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 16.dp)
            // Consume clicks so they don't pass through to content behind
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null,
                onClick = { /* Consume click */ }
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .clip(com.hasanege.materialtv.ui.theme.ExpressiveShapes.ExtraLarge)
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerLow, // Subtle, lighter tone
                    shape = com.hasanege.materialtv.ui.theme.ExpressiveShapes.ExtraLarge
                )
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(28.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { screen ->
                val isSelected = currentItemRoute == screen.route
                val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
                
                // Spring animation for icon scale
                val iconScale by androidx.compose.animation.core.animateFloatAsState(
                    targetValue = if (isSelected) 1.15f else 1f,
                    animationSpec = androidx.compose.animation.core.spring(
                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                        stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
                    ),
                    label = "icon_scale"
                )
                
                // Simple circular icon indicator
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else androidx.compose.ui.graphics.Color.Transparent
                        )
                        .clickable { 
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            onItemClick(screen) 
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        screen.icon,
                        contentDescription = stringResource(screen.labelRes),
                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                               else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(24.dp)
                            .graphicsLayer {
                                scaleX = iconScale
                                scaleY = iconScale
                            }
                    )
                }
            }
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

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MoviesList(movies: List<VodItem>, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp),
        flingBehavior = ScrollableDefaults.flingBehavior()
    ) {
        items(
            items = movies,
            key = { it.streamId ?: it.hashCode() },
            contentType = { "movie_card" }
        ) { movie ->
            val interactionSource = remember(movie.streamId) { androidx.compose.foundation.interaction.MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            // Spring physics animation like bottom nav
            val scale by androidx.compose.animation.core.animateFloatAsState(
                targetValue = if (isPressed) 0.96f else 1f,
                animationSpec = androidx.compose.animation.core.spring(
                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                    stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
                ),
                label = "scale"
            )

            androidx.compose.material3.ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .combinedClickable(
                        interactionSource = interactionSource,
                        indication = ripple(),
                        onClick = {
                            val intent = Intent(context, com.hasanege.materialtv.DetailActivity::class.java).apply {
                                putExtra("STREAM_ID", movie.streamId)
                            }
                            context.startActivity(intent)

                        },
                        onLongClick = {
                            scope.launch {
                                val added = FavoritesManager.toggleFavorite(
                                    contentId = movie.streamId ?: 0,
                                    contentType = "movie",
                                    name = movie.name ?: "",
                                    thumbnailUrl = movie.streamIcon,
                                    year = movie.year,
                                    categoryId = movie.categoryId
                                )
                                Toast.makeText(
                                    context,
                                    if (added) "Added to favorites" else "Removed from favorites",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    ),
                shape = com.hasanege.materialtv.ui.theme.ExpressiveShapes.ExtraLarge,
                colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                elevation = androidx.compose.material3.CardDefaults.elevatedCardElevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 2.dp
                )
            ) {
                Row(
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(movie.streamIcon)
                            .crossfade(true)
                            .build(),
                        imageLoader = ImageConfig.getImageLoader(context),
                        contentDescription = movie.name ?: "",
                        contentScale = ContentScale.Crop,
                        error = painterResource(R.drawable.ic_placeholder),
                        placeholder = painterResource(R.drawable.ic_placeholder),
                        modifier = Modifier
                            .width(100.dp)
                            .aspectRatio(2f / 3f)
                            .clip(com.hasanege.materialtv.ui.theme.ExpressiveShapes.Small)
                            .shadow(4.dp, com.hasanege.materialtv.ui.theme.ExpressiveShapes.Small)
                    )
                    Column(
                        modifier = Modifier
                            .padding(start = 20.dp)
                            .weight(1f)
                    ) {
                        Text(
                            text = movie.name ?: "",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        movie.rating5Based?.let { rating ->
                            if (rating > 0) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Star,
                                        contentDescription = null,
                                        tint = androidx.compose.ui.graphics.Color(0xFFFFB300),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = " $rating",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                        movie.year?.let { year ->
                             Text(
                                text = year,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun SeriesList(series: List<SeriesItem>, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp),
        flingBehavior = ScrollableDefaults.flingBehavior()
    ) {
        items(
            items = series,
            key = { it.seriesId ?: it.hashCode() },
            contentType = { "series_card" }
        ) { seriesItem ->
            val interactionSource = remember(seriesItem.seriesId) { androidx.compose.foundation.interaction.MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            // Spring physics animation like bottom nav
            val scale by androidx.compose.animation.core.animateFloatAsState(
                targetValue = if (isPressed) 0.96f else 1f,
                animationSpec = androidx.compose.animation.core.spring(
                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                    stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
                ),
                label = "scale"
            )

            androidx.compose.material3.ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .combinedClickable(
                        interactionSource = interactionSource,
                        indication = ripple(),
                        onClick = {
                            val intent = Intent(context, com.hasanege.materialtv.SeriesDetailActivity::class.java).apply {
                                putExtra("SERIES_ID", seriesItem.seriesId)
                            }
                            context.startActivity(intent)

                        },
                        onLongClick = {
                            scope.launch {
                                val added = FavoritesManager.toggleFavorite(
                                    contentId = seriesItem.seriesId ?: 0,
                                    contentType = "series",
                                    name = seriesItem.name ?: "",
                                    thumbnailUrl = seriesItem.cover,
                                    genre = seriesItem.genre,
                                    year = seriesItem.year,
                                    categoryId = seriesItem.categoryId,
                                    seriesId = seriesItem.seriesId,
                                    streamIcon = seriesItem.cover
                                )
                                Toast.makeText(
                                    context,
                                    if (added) "Added to favorites" else "Removed from favorites",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    ),
                shape = com.hasanege.materialtv.ui.theme.ExpressiveShapes.ExtraLarge,
                colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                elevation = androidx.compose.material3.CardDefaults.elevatedCardElevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 2.dp
                )
            ) {
                Row(
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(seriesItem.cover)
                            .crossfade(true)
                            .build(),
                        imageLoader = ImageConfig.getImageLoader(context),
                        contentDescription = seriesItem.name ?: "",
                        contentScale = ContentScale.Crop,
                        error = painterResource(R.drawable.ic_placeholder),
                        placeholder = painterResource(R.drawable.ic_placeholder),
                        modifier = Modifier
                            .width(100.dp)
                            .aspectRatio(2f / 3f)
                            .clip(com.hasanege.materialtv.ui.theme.ExpressiveShapes.Small)
                            .shadow(4.dp, com.hasanege.materialtv.ui.theme.ExpressiveShapes.Small)
                    )
                    Column(
                        modifier = Modifier
                            .padding(start = 20.dp)
                            .weight(1f)
                    ) {
                        Text(
                            text = seriesItem.name ?: "",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (!seriesItem.releaseDate.isNullOrEmpty()) {
                            Text(
                                text = seriesItem.releaseDate ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        if (!seriesItem.plot.isNullOrEmpty()) {
                            Text(
                                text = seriesItem.plot ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 8.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun LiveTVList(liveStreams: List<LiveStream>) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp),
        flingBehavior = ScrollableDefaults.flingBehavior()
    ) {
        items(
            items = liveStreams,
            key = { it.streamId ?: it.hashCode() },
            contentType = { "live_card" }
        ) { liveStream ->
            val interactionSource = remember(liveStream.streamId) { androidx.compose.foundation.interaction.MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            // Spring physics animation like bottom nav
            val scale by androidx.compose.animation.core.animateFloatAsState(
                targetValue = if (isPressed) 0.96f else 1f,
                animationSpec = androidx.compose.animation.core.spring(
                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                    stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
                ),
                label = "scale"
            )

            androidx.compose.material3.ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .combinedClickable(
                        interactionSource = interactionSource,
                        indication = ripple(),
                        onClick = {
                            // For M3U, get URL from repository; for Xtream, construct it
                            val streamUrl = if (SessionManager.loginType == SessionManager.LoginType.M3U) {
                                val url = com.hasanege.materialtv.data.M3uRepository.getStreamUrl(liveStream.streamId ?: 0)
                                Log.d("LiveTVList", "M3U stream URL for ${liveStream.name}: $url")
                                url
                            } else {
                                "${SessionManager.serverUrl}/live/${SessionManager.username}/${SessionManager.password}/${liveStream.streamId}.ts"
                            }
                            
                            if (streamUrl.isNullOrEmpty()) {
                                Toast.makeText(context, "Stream URL not found for ${liveStream.name}", Toast.LENGTH_SHORT).show()
                            } else {
                                val intent = Intent(context, PlayerActivity::class.java).apply {
                                    putExtra("url", streamUrl)
                                    putExtra("TITLE", liveStream.name)
                                }
                                context.startActivity(intent)
                            }
                        },
                        onLongClick = {
                            scope.launch {
                                val added = FavoritesManager.toggleFavorite(
                                    contentId = liveStream.streamId ?: 0,
                                    contentType = "live",
                                    name = liveStream.name ?: "",
                                    thumbnailUrl = liveStream.streamIcon,
                                    categoryId = liveStream.categoryId,
                                    streamIcon = liveStream.streamIcon
                                )
                                Toast.makeText(
                                    context,
                                    if (added) "Added to favorites" else "Removed from favorites",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    ),
                shape = com.hasanege.materialtv.ui.theme.ExpressiveShapes.ExtraLarge,
                colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                elevation = androidx.compose.material3.CardDefaults.elevatedCardElevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 2.dp
                )
            ) {
                Row(
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(liveStream.streamIcon)
                            .crossfade(true)
                            .build(),
                        imageLoader = ImageConfig.getImageLoader(context),
                        contentDescription = liveStream.name ?: "",
                        contentScale = ContentScale.Fit,
                        error = painterResource(R.drawable.ic_placeholder),
                        placeholder = painterResource(R.drawable.ic_placeholder),
                        modifier = Modifier
                            .width(80.dp)
                            .aspectRatio(1f)
                            .clip(com.hasanege.materialtv.ui.theme.ExpressiveShapes.Small)
                            .shadow(4.dp, com.hasanege.materialtv.ui.theme.ExpressiveShapes.Small)
                    )
                    Column(
                        modifier = Modifier
                            .padding(start = 20.dp)
                            .weight(1f)
                    ) {
                        Text(
                            text = liveStream.name ?: "",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.home_live_tv),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
