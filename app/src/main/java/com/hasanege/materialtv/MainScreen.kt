package com.hasanege.materialtv

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

sealed class MainScreen(val route: String, val label: String, val icon: ImageVector) {
    object Home : MainScreen("home", "Home", Icons.Default.Home)
    object Downloads : MainScreen("downloads", "Downloads", Icons.Default.Download)
    object Profile : MainScreen("profile", "Profile", Icons.Default.Person)
}
