package com.hasanege.materialtv

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.hasanege.materialtv.model.LiveStream
import com.hasanege.materialtv.network.SessionManager
import com.hasanege.materialtv.ui.*
import com.hasanege.materialtv.ui.theme.MaterialTVTheme

class CategoryActivity : ComponentActivity() {

    private val viewModel: CategoryViewModel by viewModels { CategoryViewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val categoryId = intent.getStringExtra("category_id")
        val categoryType = intent.getStringExtra("category_type")
        val categoryName = intent.getStringExtra("category_name")

        if (categoryId != null && categoryType != null) {
            viewModel.loadCategoryItems(categoryId, categoryType)
        } else {
            android.widget.Toast.makeText(this, "Error: Missing category info", android.widget.Toast.LENGTH_SHORT).show()
            finish()
        }

        setContent {
            MaterialTVTheme {
                CategoryScreen(viewModel, categoryName ?: "Category")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun CategoryScreen(viewModel: CategoryViewModel, categoryName: String) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(categoryName) })
        }
    ) { paddingValues ->
        androidx.compose.animation.AnimatedVisibility(
            visible = true, // Always visible, but triggers the animation
            enter = fadeIn(animationSpec = tween(durationMillis = 500)),
            modifier = Modifier.padding(paddingValues)
        ) {
            when (val state = viewModel.uiState.value) {
                is UiState.Loading -> {
                    CenteredProgressBar()
                }
                is UiState.Success -> {
                    when (state.data) {
                        is CategoryData.Movies -> MoviesList(
                            movies = state.data.items
                        )
                        is CategoryData.Series -> SeriesList(
                            series = state.data.items
                        )
                        is CategoryData.LiveStreams -> LiveTVList(
                            liveStreams = state.data.items
                        )
                    }
                }
                is UiState.Error -> {
                    ErrorMessage(message = state.message)
                }
            }
        }
    }
}
