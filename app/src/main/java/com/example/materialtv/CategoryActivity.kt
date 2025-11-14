package com.example.materialtv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.materialtv.ui.CenteredProgressBar
import com.example.materialtv.ui.ErrorMessage
import com.example.materialtv.ui.MoviesList
import com.example.materialtv.ui.SeriesList
import com.example.materialtv.ui.theme.MaterialTVTheme

class CategoryActivity : ComponentActivity() {

    private val viewModel: CategoryViewModel by viewModels { CategoryViewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val categoryId = intent.getStringExtra("category_id")
        val categoryType = intent.getStringExtra("category_type")
        val categoryName = intent.getStringExtra("category_name")

        if (categoryId != null && categoryType != null) {
            viewModel.loadCategoryItems(categoryId, categoryType)
        }

        setContent {
            MaterialTVTheme {
                CategoryScreen(viewModel, categoryName ?: "Category")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(viewModel: CategoryViewModel, categoryName: String) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(categoryName) })
        }
    ) { paddingValues ->
        AnimatedVisibility(
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
                    }
                }
                is UiState.Error -> {
                    ErrorMessage(message = state.message)
                }
            }
        }
    }
}
