package com.hasanege.materialtv

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.hasanege.materialtv.ui.screens.favorites.FavoritesScreen
import com.hasanege.materialtv.ui.theme.MaterialTVTheme

class FavoritesActivity : AppCompatActivity() {
    private val viewModel: FavoritesViewModel by viewModels { FavoritesViewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTVTheme {
                FavoritesScreen(viewModel)
            }
        }
    }
}
