package com.hasanege.materialtv

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import com.hasanege.materialtv.ui.ExpressiveTabSlider
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hasanege.materialtv.ui.CenteredProgressBar
import com.hasanege.materialtv.ui.ErrorMessage
import com.hasanege.materialtv.ui.LiveTVList
import com.hasanege.materialtv.ui.MoviesList
import com.hasanege.materialtv.ui.SeriesList
import com.hasanege.materialtv.ui.StreamifyBottomNavBar
import com.hasanege.materialtv.ui.theme.MaterialTVTheme

class SearchActivity : AppCompatActivity() {
    private val searchViewModel: SearchViewModel by viewModels { SearchViewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTVTheme {
                SearchScreen(searchViewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(viewModel: SearchViewModel) {
    val navController = remember { mutableStateOf(MainScreen.Home.route) } // This could be improved to represent the actual search screen state
    val bottomNavItems = listOf(MainScreen.Home, MainScreen.Downloads, MainScreen.Profile)
    val context = LocalContext.current

    Scaffold(
        bottomBar = {
            StreamifyBottomNavBar(
                items = bottomNavItems,
                currentItemRoute = navController.value,
                onItemClick = {
                    if (it.route == MainScreen.Home.route) {
                        context.startActivity(Intent(context, HomeActivity::class.java))
                        (context as? Activity)?.finish()
                    }
                    navController.value = it.route
                }
            )
        }
    ) { paddingValues ->
        var query by remember { mutableStateOf("") }
        var isVisible by remember { mutableStateOf(false) }

        // Entry animation with delay for visibility
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(50) // Small delay to ensure the screen is ready
            isVisible = true
        }

        // Track if keyboard is visible using composition local
        val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
        val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
        var keyboardWasShown by remember { mutableStateOf(false) }

        // Back Handler - handles Android system back button
        // 1st press: Close keyboard
        // 2nd press: Exit search menu
        // 3rd press: Exit app (handled by system after search finishes)
        BackHandler(enabled = true) {
            if (keyboardWasShown) {
                // First: hide keyboard
                keyboardController?.hide()
                focusManager.clearFocus()
                keyboardWasShown = false
            } else {
                // Second: exit search activity
                (context as? Activity)?.finish()
            }
        }

        // Animation from BOTTOM to TOP
        androidx.compose.animation.AnimatedVisibility(
            visible = isVisible,
            enter = androidx.compose.animation.slideInVertically(
                initialOffsetY = { it }, // Positive = from bottom
                animationSpec = androidx.compose.animation.core.spring(
                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                    stiffness = androidx.compose.animation.core.Spring.StiffnessLow
                )
            ) + androidx.compose.animation.fadeIn(
                animationSpec = androidx.compose.animation.core.tween(durationMillis = 400)
            ) + androidx.compose.animation.scaleIn(
                initialScale = 0.95f,
                animationSpec = androidx.compose.animation.core.tween(durationMillis = 300)
            ),
            modifier = Modifier.padding(paddingValues)
        ) {
            Column {
                androidx.compose.material3.SearchBar(
                    query = query,
                    onQueryChange = { 
                        query = it
                        keyboardWasShown = true // Keyboard is shown when typing
                    },
                    onSearch = { 
                        viewModel.search(query)
                        keyboardController?.hide()
                        focusManager.clearFocus()
                        keyboardWasShown = false
                    },
                    active = false, // Keep it as a bar
                    onActiveChange = {},
                    placeholder = { Text(stringResource(R.string.search_field_label)) },
                    leadingIcon = {
                        IconButton(onClick = {
                            (context as? Activity)?.finish()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                        }
                    },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {}

            // Debounce search
            LaunchedEffect(viewModel.isLoading.value, query) {
                if (!viewModel.isLoading.value) {
                    kotlinx.coroutines.delay(300)
                    viewModel.search(query)
                }
            }

            var selectedTab by remember { mutableIntStateOf(0) }
            val tabs = listOf(
                stringResource(R.string.tab_movies),
                stringResource(R.string.tab_series),
                stringResource(R.string.tab_live_tv)
            )
            ExpressiveTabSlider(
                tabs = tabs,
                selectedIndex = selectedTab,
                onTabSelected = { selectedTab = it }
            )

            if (viewModel.isLoading.value && query.isNotBlank()) {
                CenteredProgressBar()
            } else {
                when (selectedTab) {
                    0 -> {
                        when (val moviesState = viewModel.movies.value) {
                            is UiState.Loading -> CenteredProgressBar()
                            is UiState.Success -> MoviesList(moviesState.data)
                            is UiState.Error -> ErrorMessage(moviesState.message)
                        }
                    }
                    1 -> {
                        when (val seriesState = viewModel.series.value) {
                            is UiState.Loading -> CenteredProgressBar()
                            is UiState.Success -> SeriesList(seriesState.data)
                            is UiState.Error -> ErrorMessage(seriesState.message)
                        }
                    }
                    2 -> {
                        when (val liveState = viewModel.liveStreams.value) {
                            is UiState.Loading -> CenteredProgressBar()
                            is UiState.Success -> LiveTVList(liveState.data)
                            is UiState.Error -> ErrorMessage(liveState.message)
                        }
                    }
                }
            }
            }
        }
    }
}
