
package com.hasanege.materialtv

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.util.UnstableApi
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.hasanege.materialtv.model.ContinueWatchingItem
import com.hasanege.materialtv.model.LiveStream
import com.hasanege.materialtv.model.SeriesItem
import com.hasanege.materialtv.model.VodItem
import com.hasanege.materialtv.network.SessionManager
import com.hasanege.materialtv.ui.CenteredProgressBar
import com.hasanege.materialtv.ui.ErrorMessage
import com.hasanege.materialtv.ui.StreamifyBottomNavBar
import com.hasanege.materialtv.ui.screens.downloads.DownloadsScreen
import com.hasanege.materialtv.ui.screens.profile.ProfileScreen
import com.hasanege.materialtv.ui.StreamifyNavRail
import com.hasanege.materialtv.ui.theme.MaterialTVTheme
import com.hasanege.materialtv.ui.theme.ExpressiveAnimations
import com.hasanege.materialtv.ui.utils.ImageConfig
import com.hasanege.materialtv.R
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollableDefaults

class HomeActivity : AppCompatActivity() {
    private val homeViewModel: HomeViewModel by viewModels { HomeViewModelFactory }
    private val downloadsViewModel: DownloadsViewModel by viewModels { DownloadsViewModelFactory }
    private val profileViewModel: ProfileViewModel by viewModels { ProfileViewModelFactory(application as MainApplication) }

    @UnstableApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WatchHistoryManager.initialize(this)
        downloadsViewModel.initialize(this)
        setContent {
            MaterialTVTheme {
                StreamifyApp(homeViewModel, downloadsViewModel, profileViewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@UnstableApi
@Composable
fun StreamifyApp(homeViewModel: HomeViewModel, downloadsViewModel: DownloadsViewModel, profileViewModel: ProfileViewModel) {
    val context = LocalContext.current
    val isOnline = isNetworkAvailable(context)

    val username = SessionManager.username ?: ""
    val password = SessionManager.password ?: ""

    LaunchedEffect(isOnline) {
        if (isOnline) {
            homeViewModel.loadInitialData(context, username, password)
        }
    }

    val navController = remember { mutableStateOf(MainScreen.Home.route) }
    val bottomNavItems = listOf(MainScreen.Home, MainScreen.Downloads, MainScreen.Profile)



// ... (existing imports)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Material",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "TV",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } 
                },
                actions = {
                    IconButton(onClick = { context.startActivity(Intent(Settings.ACTION_CAST_SETTINGS)) }) {
                        Icon(Icons.Default.Cast, contentDescription = "Cast")
                    }
                    IconButton(onClick = { context.startActivity(Intent(context, SearchActivity::class.java)) }) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                }
            )
        }
    ) { paddingValues ->
        androidx.compose.foundation.layout.BoxWithConstraints(
            modifier = Modifier.padding(paddingValues)
        ) {
            val isWideScreen = maxWidth > 600.dp
            
            if (isWideScreen) {
                Row(modifier = Modifier.fillMaxSize()) {
                    StreamifyNavRail(
                        items = bottomNavItems,
                        currentItemRoute = navController.value,
                        onItemClick = { navController.value = it.route }
                    )
                    Box(modifier = Modifier.weight(1f)) {
                        when (navController.value) {
                            MainScreen.Home.route -> {
                                if (isOnline) {
                                    HomeScreen(homeViewModel)
                                } else {
                                    NoConnectionScreen()
                                }
                            }
                            MainScreen.Downloads.route -> DownloadsScreen(downloadsViewModel)
                            MainScreen.Profile.route -> ProfileScreen(profileViewModel)
                        }
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.padding(bottom = 80.dp)) { // Adjust for bottom bar
                        when (navController.value) {
                            MainScreen.Home.route -> {
                                if (isOnline) {
                                    HomeScreen(homeViewModel)
                                } else {
                                    NoConnectionScreen()
                                }
                            }
                            MainScreen.Downloads.route -> DownloadsScreen(downloadsViewModel)
                            MainScreen.Profile.route -> ProfileScreen(profileViewModel)
                        }
                    }
                    StreamifyBottomNavBar(
                        items = bottomNavItems,
                        currentItemRoute = navController.value,
                        onItemClick = { navController.value = it.route },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }
    }
}

@Composable
fun NoConnectionScreen() {
    // TODO: Add a nice animation for no connection. The Lottie file R.raw.no_connection is missing.
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("No connection", style = MaterialTheme.typography.headlineMedium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@UnstableApi
@Composable
fun HomeScreen(homeViewModel: HomeViewModel) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        stringResource(R.string.tab_movies),
        stringResource(R.string.tab_series),
        stringResource(R.string.tab_live_tv)
    )
    
    val pagerState = rememberPagerState(pageCount = { tabs.size })

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                homeViewModel.loadContinueWatching()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
    }
    
    // Sync Tab selection with Pager when tab is clicked
    LaunchedEffect(selectedTabIndex) {
        if (pagerState.currentPage != selectedTabIndex) {
            pagerState.animateScrollToPage(selectedTabIndex)
        }
    }
    
    // Sync Pager scroll with Tab selection when swiping
    LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
        if (!pagerState.isScrollInProgress && selectedTabIndex != pagerState.currentPage) {
            selectedTabIndex = pagerState.currentPage
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title) }
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            CategoryScreen(viewModel = homeViewModel, selectedTab = page)
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@UnstableApi
@Composable
fun CategoryScreen(
    viewModel: HomeViewModel,
    selectedTab: Int
) {
    val context = LocalContext.current
    val isRefreshing = viewModel.isRefreshing
    val continueWatchingState by viewModel.continueWatchingState.collectAsState()
    
    // Set context for ViewModel
    LaunchedEffect(Unit) {
        viewModel.setContext(context)
    }
    val pullRefreshState = rememberPullRefreshState(isRefreshing, { viewModel.loadInitialData(context, SessionManager.username ?: "", SessionManager.password ?: "", true) })

    Box(modifier = Modifier.pullRefresh(pullRefreshState)) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                val state = continueWatchingState
                when (state) {
                    is UiState.Success -> {
                        if (state.data.isNotEmpty()) {
                            ContinueWatchingRow(
                                items = state.data,
                                onItemClick = { item ->
                                    val intent = Intent(context, PlayerActivity::class.java).apply {
                                        putExtra("TITLE", item.name)
                                        putExtra("START_POSITION", item.position)
                                        if (item.type == "downloaded") {
                                            // For regular downloaded files (non-series)
                                            if (item.episodeId != null && item.episodeId!!.isNotEmpty()) {
                                                putExtra("url", item.episodeId)
                                                putExtra("STREAM_ID", item.streamId)
                                            } else {
                                                putExtra("IS_DOWNLOADED_FILE", true)
                                                putExtra("URI", item.streamIcon)
                                            }
                                        } else if (item.type == "series") {
                                            // For series items, go to SeriesDetailActivity
                                            val seriesIntent = Intent(context, SeriesDetailActivity::class.java).apply {
                                                putExtra("SERIES_ID", item.seriesId)
                                                putExtra("TITLE", item.name)
                                                putExtra("COVER", item.streamIcon)
                                            }
                                            context.startActivity(seriesIntent)
                                            return@ContinueWatchingRow
                                        } else if (item.type == "movie") {
                                            putExtra("STREAM_ID", item.streamId)
                                        } else if (item.type == "live") {
                                            // For M3U, get URL from repository; for Xtream, construct it
                                            if (SessionManager.loginType == SessionManager.LoginType.M3U) {
                                                val streamUrl = com.hasanege.materialtv.data.M3uRepository.getStreamUrl(item.streamId)
                                                if (streamUrl.isNullOrEmpty()) {
                                                    android.widget.Toast.makeText(context, "Stream URL not found for ${item.name}", android.widget.Toast.LENGTH_SHORT).show()
                                                    return@ContinueWatchingRow
                                                }
                                                putExtra("url", streamUrl)
                                            } else {
                                                putExtra("url", "${SessionManager.serverUrl}/live/${SessionManager.username}/${SessionManager.password}/${item.streamId}.ts")
                                            }
                                            putExtra("TITLE", item.name)
                                            putExtra("LIVE_STREAM_ID", item.streamId)
                                            putExtra("STREAM_ICON", item.streamIcon)
                                        } else {
                                            putExtra("STREAM_ID", item.streamId)
                                            putExtra("SERIES_ID", item.seriesId)
                                            putExtra("EPISODE_ID", item.episodeId)
                                        }
                                    }
                                    context.startActivity(intent)
                                },
                                onPin = { item ->
                                    // Toggle pin status
                                    val updatedItems = state.data.map {
                                        if (it.streamId == item.streamId) {
                                            it.copy(isPinned = !it.isPinned)
                                        } else {
                                            it
                                        }
                                    }
                                    viewModel.updateContinueWatchingItems(updatedItems)
                                },
                                onRemove = { item ->
                                    // Remove from continue watching (not from full watch history)
                                    viewModel.removeFromContinueWatching(item)
                                }
                            )
                        }
                    }
                    else -> {}
                }
            }
            when (selectedTab) {
                0 -> {
                    when (val moviesByCategoriesState = viewModel.moviesByCategoriesState) {
                        is UiState.Loading -> item { CenteredProgressBar() }
                        is UiState.Success -> {
                            items(
                                items = moviesByCategoriesState.data.entries.filter { it.value.isNotEmpty() }.toList(),
                                key = { (category, _) -> category.categoryId }
                            ) { (category, movies) ->
                                ContentRow(title = category.categoryName, items = movies.take(10), onSeeAllClick = {
                                    val intent = Intent(context, CategoryActivity::class.java).apply {
                                        putExtra("category_id", category.categoryId)
                                        putExtra("category_type", "movie")
                                        putExtra("category_name", category.categoryName)
                                    }
                                    context.startActivity(intent)
                                }) { vodItem ->
                                    val intent = Intent(context, PlayerActivity::class.java).apply {
                                        putExtra("STREAM_ID", vodItem.streamId)
                                        putExtra("TITLE", vodItem.name)
                                    }
                                    context.startActivity(intent)
                                }
                            }
                        }
                        is UiState.Error -> item { ErrorMessage(moviesByCategoriesState.message) }
                    }
                }
                1 -> {
                    when (val seriesByCategoriesState = viewModel.seriesByCategoriesState) {
                        is UiState.Loading -> item { CenteredProgressBar() }
                        is UiState.Success -> {
                            items(
                                items = seriesByCategoriesState.data.entries.filter { it.value.isNotEmpty() }.toList(),
                                key = { (category, _) -> category.categoryId }
                            ) { (category, series) ->
                                SeriesContentRow(title = category.categoryName, items = series.take(10), onSeeAllClick = {
                                    val intent = Intent(context, CategoryActivity::class.java).apply {
                                        putExtra("category_id", category.categoryId)
                                        putExtra("category_type", "series")
                                        putExtra("category_name", category.categoryName)
                                    }
                                    context.startActivity(intent)
                                }) { seriesItem ->
                                    val intent = Intent(context, PlayerActivity::class.java).apply {
                                        putExtra("SERIES_ID", seriesItem.seriesId)
                                        putExtra("TITLE", seriesItem.name)
                                    }
                                    context.startActivity(intent)
                                }
                            }
                        }
                        is UiState.Error -> item { ErrorMessage(seriesByCategoriesState.message) }
                    }
                }
                2 -> {
                    when (val liveByCategoriesState = viewModel.liveByCategoriesState) {
                        is UiState.Loading -> item { CenteredProgressBar() }
                        is UiState.Success -> {
                            items(
                                items = liveByCategoriesState.data.entries.filter { it.value.isNotEmpty() }.toList(),
                                key = { (category, _) -> category.categoryId }
                            ) { (category, liveStreams) ->
                                LiveStreamContentRow(
                                    title = category.categoryName,
                                    items = liveStreams.take(10),
                                    onSeeAllClick = {
                                        val intent = Intent(context, CategoryActivity::class.java).apply {
                                            putExtra("category_id", category.categoryId)
                                            putExtra("category_type", "live")
                                            putExtra("category_name", category.categoryName)
                                        }
                                        context.startActivity(intent)
                                    }
                                ) { liveStream ->
                                    val intent = Intent(context, PlayerActivity::class.java).apply {
                                        // For M3U, get URL from repository; for Xtream, construct it
                                        if (SessionManager.loginType == SessionManager.LoginType.M3U) {
                                            val streamUrl = com.hasanege.materialtv.data.M3uRepository.getStreamUrl(liveStream.streamId ?: 0)
                                            if (streamUrl.isNullOrEmpty()) {
                                                android.widget.Toast.makeText(context, "Stream URL not found for ${liveStream.name}", android.widget.Toast.LENGTH_SHORT).show()
                                                return@LiveStreamContentRow
                                            }
                                            putExtra("url", streamUrl)
                                        } else {
                                            putExtra("url", "${SessionManager.serverUrl}/live/${SessionManager.username}/${SessionManager.password}/${liveStream.streamId}.ts")
                                        }
                                        putExtra("TITLE", liveStream.name ?: "")
                                        putExtra("LIVE_STREAM_ID", liveStream.streamId ?: 0)
                                        putExtra("STREAM_ICON", liveStream.streamIcon)
                                    }
                                    context.startActivity(intent)
                                }
                            }
                        }
                        is UiState.Error -> item { ErrorMessage(liveByCategoriesState.message) }
                    }
                }
            }
        }
        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            backgroundColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun CategoryChips(viewModel: HomeViewModel, selectedTab: Int) {
    val (categories, selectedCategoryId, onCategorySelected) = when (selectedTab) {
        0 -> Triple(viewModel.movieCategories, viewModel.selectedMovieCategoryId, viewModel::onMovieCategorySelected)
        1 -> Triple(viewModel.seriesCategories, viewModel.selectedSeriesCategoryId, viewModel::onSeriesCategorySelected)
        else -> Triple(viewModel.liveCategories, viewModel.selectedLiveCategoryId, viewModel::onLiveCategorySelected)
    }

    LazyRow(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        item {
            FilterChip(
                selected = selectedCategoryId == null,
                onClick = { onCategorySelected(null) },
                label = { Text("All") },
                modifier = Modifier.padding(end = 8.dp)
            )
        }
        items(categories) {
            FilterChip(
                selected = it.categoryId == selectedCategoryId,
                onClick = { onCategorySelected(it.categoryId) },
                label = { Text(it.categoryName) },
                modifier = Modifier.padding(end = 8.dp)
            )
        }
    }
}

@Composable
fun ContentRow(
    title: String,
    items: List<VodItem>,
    onSeeAllClick: () -> Unit,
    onItemClick: (VodItem) -> Unit
) {
    val context = LocalContext.current
    
    AnimatedVisibility(
        visible = true, 
        enter = fadeIn(animationSpec = ExpressiveAnimations.enter()) + slideInVertically(
            initialOffsetY = { it / 3 }, 
            animationSpec = ExpressiveAnimations.enter()
        ),
        exit = fadeOut(animationSpec = ExpressiveAnimations.exit())
    ) {
        Column(modifier = Modifier.padding(vertical = 20.dp)) { 
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp), 
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(onClick = onSeeAllClick) { 
                    Text(
                        "See All",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    ) 
                }
            }
            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                flingBehavior = ScrollableDefaults.flingBehavior()
            ) {
                items(
                    items = items, 
                    key = { it.streamId ?: it.hashCode() }
                ) { item ->
                    var isPressed by remember { mutableStateOf(false) }
                    val scale by animateFloatAsState(
                        targetValue = if (isPressed) 0.92f else 1f,
                        animationSpec = ExpressiveAnimations.fast(),
                        label = "card_scale"
                    )

                    Column(
                        modifier = Modifier
                            .width(150.dp) 
                            .scale(scale)
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        isPressed = event.type == PointerEventType.Press
                                    }
                                }
                            }
                            .clickable { onItemClick(item) }
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(item.streamIcon)
                                .crossfade(200)
                                .build(),
                            imageLoader = ImageConfig.getImageLoader(context),
                            contentDescription = item.name ?: "",
                            contentScale = ContentScale.Crop,
                            error = androidx.compose.ui.res.painterResource(R.drawable.ic_placeholder),
                            placeholder = androidx.compose.ui.res.painterResource(R.drawable.ic_placeholder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(2f / 3f)
                                .clip(MaterialTheme.shapes.medium)
                                .shadow(elevation = 6.dp, shape = MaterialTheme.shapes.medium)
                        )
                        Text(
                            item.name ?: "",
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 12.dp, start = 4.dp, end = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SeriesContentRow(
    title: String,
    items: List<SeriesItem>,
    onSeeAllClick: () -> Unit,
    onItemClick: (SeriesItem) -> Unit
) {
    val context = LocalContext.current
    
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(animationSpec = ExpressiveAnimations.enter()) + slideInVertically(
            initialOffsetY = { it / 3 },
            animationSpec = ExpressiveAnimations.enter()
        ),
        exit = fadeOut(animationSpec = ExpressiveAnimations.exit())
    ) {
        Column(modifier = Modifier.padding(vertical = 20.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(onClick = onSeeAllClick) { 
                    Text(
                        "See All",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    ) 
                }
            }
            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                flingBehavior = ScrollableDefaults.flingBehavior()
            ) {
                items(
                    items = items,
                    key = { it.seriesId ?: it.hashCode() }
                ) { item ->
                    var isPressed by remember { mutableStateOf(false) }
                    val scale by animateFloatAsState(
                        targetValue = if (isPressed) 0.92f else 1f,
                        animationSpec = ExpressiveAnimations.fast(),
                        label = "card_scale"
                    )

                    Column(
                        modifier = Modifier
                            .width(150.dp)
                            .scale(scale)
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        isPressed = event.type == PointerEventType.Press
                                    }
                                }
                            }
                            .clickable { onItemClick(item) }
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(item.cover)
                                .crossfade(200)
                                .build(),
                            imageLoader = ImageConfig.getImageLoader(context),
                            contentDescription = item.name ?: "",
                            contentScale = ContentScale.Crop,
                            error = androidx.compose.ui.res.painterResource(R.drawable.ic_placeholder),
                            placeholder = androidx.compose.ui.res.painterResource(R.drawable.ic_placeholder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(2f / 3f)
                                .clip(MaterialTheme.shapes.medium)
                                .shadow(elevation = 6.dp, shape = MaterialTheme.shapes.medium)
                        )
                        Text(
                            item.name ?: "",
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 12.dp, start = 4.dp, end = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LiveStreamContentRow(
    title: String,
    items: List<LiveStream>,
    onSeeAllClick: () -> Unit,
    onItemClick: (LiveStream) -> Unit
) {
    val context = LocalContext.current
    
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(animationSpec = ExpressiveAnimations.enter()) + slideInVertically(
            initialOffsetY = { it / 3 },
            animationSpec = ExpressiveAnimations.enter()
        ),
        exit = fadeOut(animationSpec = ExpressiveAnimations.exit())
    ) {
        Column(modifier = Modifier.padding(vertical = 20.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(onClick = onSeeAllClick) { 
                    Text(
                        "See All",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    ) 
                }
            }
            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                flingBehavior = ScrollableDefaults.flingBehavior()
            ) {
                items(
                    items = items,
                    key = { it.streamId ?: it.hashCode() }
                ) { item ->
                    var isPressed by remember { mutableStateOf(false) }
                    val scale by animateFloatAsState(
                        targetValue = if (isPressed) 0.92f else 1f,
                        animationSpec = ExpressiveAnimations.fast(),
                        label = "card_scale"
                    )

                    Column(
                        modifier = Modifier
                            .width(150.dp)
                            .scale(scale)
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        isPressed = event.type == PointerEventType.Press
                                    }
                                }
                            }
                            .clickable { onItemClick(item) }
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(item.streamIcon)
                                .crossfade(200)
                                .build(),
                            imageLoader = ImageConfig.getImageLoader(context),
                            contentDescription = item.name ?: "",
                            contentScale = ContentScale.Fit,
                            error = androidx.compose.ui.res.painterResource(R.drawable.ic_placeholder),
                            placeholder = androidx.compose.ui.res.painterResource(R.drawable.ic_placeholder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(MaterialTheme.shapes.medium)
                                .shadow(elevation = 6.dp, shape = MaterialTheme.shapes.medium)
                        )
                        Text(
                            item.name ?: "",
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 12.dp, start = 4.dp, end = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

fun isNetworkAvailable(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork ?: return false
    val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
    return when {
        activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
        activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
        else -> false
    }
}
