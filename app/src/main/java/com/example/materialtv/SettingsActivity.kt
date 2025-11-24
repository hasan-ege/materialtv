package com.example.materialtv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import com.example.materialtv.ui.screens.settings.SettingsScreen
import com.example.materialtv.ui.theme.MaterialTVTheme

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTVTheme {
                SettingsScreen(onBackClick = { finish() })
            }
        }
    }
}
