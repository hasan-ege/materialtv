
package com.hasanege.materialtv

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.hasanege.materialtv.data.M3uRepository
import com.hasanege.materialtv.network.CredentialsManager
import com.hasanege.materialtv.network.SessionManager
import com.hasanege.materialtv.ui.theme.MaterialTVTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WatchHistoryManager.initialize(this)

        val credentialsManager = (application as MainApplication).credentialsManager
        val serverUrl = credentialsManager.getServerUrl()
        val username = credentialsManager.getUsername()
        val password = credentialsManager.getPassword()
        val m3uUrl = credentialsManager.getM3uUrl()
        
        // Check for saved M3U URL first
        if (!m3uUrl.isNullOrBlank()) {
            android.util.Log.d("MainActivity", "=== M3U Auto-Login Started ===")
            android.util.Log.d("MainActivity", "Saved M3U URL found: $m3uUrl")
            
            SessionManager.initializeM3u(m3uUrl)
            // Fetch playlist data before navigating
            lifecycleScope.launch {
                try {
                    android.util.Log.d("MainActivity", "Fetching M3U playlist for auto-login...")
                    M3uRepository.fetchPlaylist(m3uUrl)
                    android.util.Log.d("MainActivity", "Playlist fetched successfully. Navigating to home...")
                    navigateToHome()
                } catch (e: Exception) {
                    // If fetching fails, show login screen
                    android.util.Log.e("MainActivity", "=== M3U Auto-Login Failed ===", e)
                    android.util.Log.e("MainActivity", "Exception: ${e.javaClass.simpleName}: ${e.message}")
                    e.printStackTrace()
                    
                    android.util.Log.d("MainActivity", "Clearing credentials and showing login screen...")
                    credentialsManager.clearCredentials()
                    showLoginScreen()
                }
            }
            return
        }
        
        // Check for saved Xtream credentials
        val hasSavedCredentials = !serverUrl.isNullOrBlank() && !username.isNullOrBlank() && !password.isNullOrBlank()
        if (hasSavedCredentials) {
            SessionManager.initialize(serverUrl!!, username!!, password!!)
            navigateToHome()
            return 
        }

        showLoginScreen()
    }
    
    private fun showLoginScreen() {
        setContent {
            MaterialTVTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    LoginScreen(viewModel) { navigateToHome() }
                }
            }
        }
    }

    private fun navigateToHome() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(viewModel: MainViewModel, onLoginSuccess: () -> Unit) {
    LaunchedEffect(viewModel.error) {
        if (viewModel.error == null && SessionManager.isInitialized()) {
            onLoginSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("MaterialTV Login", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        androidx.compose.material3.TabRow(selectedTabIndex = if (viewModel.isM3uLogin) 1 else 0) {
            androidx.compose.material3.Tab(
                selected = !viewModel.isM3uLogin,
                onClick = { viewModel.isM3uLogin = false },
                text = { Text("Xtream Codes") }
            )
            androidx.compose.material3.Tab(
                selected = viewModel.isM3uLogin,
                onClick = { viewModel.isM3uLogin = true },
                text = { Text("M3U Playlist") }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        if (viewModel.isM3uLogin) {
            OutlinedTextField(
                value = viewModel.m3uUrl,
                onValueChange = { viewModel.m3uUrl = it },
                label = { Text("M3U Playlist URL") },
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            XtreamLoginFields(viewModel)
        }

        Spacer(modifier = Modifier.height(8.dp))
        viewModel.error?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error)
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (viewModel.isLoading) {
            CircularProgressIndicator()
        } else {
            Button(onClick = { viewModel.onLoginClick(onLoginSuccess) }) {
                Text(if (viewModel.isM3uLogin) "Load Playlist" else "Login")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun XtreamLoginFields(viewModel: MainViewModel) {
    OutlinedTextField(value = viewModel.serverUrl, onValueChange = { viewModel.serverUrl = it }, label = { Text("Server URL") }, modifier = Modifier.fillMaxWidth())
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(value = viewModel.username, onValueChange = { viewModel.username = it }, label = { Text("Username") }, modifier = Modifier.fillMaxWidth())
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(value = viewModel.password, onValueChange = { viewModel.password = it }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
}
