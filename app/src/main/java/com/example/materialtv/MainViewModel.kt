package com.example.materialtv

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.materialtv.model.AuthUserInfo
import com.example.materialtv.network.CredentialsManager
import com.example.materialtv.network.SessionManager
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

class MainViewModel(application: Application) : AndroidViewModel(application) {
    // UI State
    var serverUrl by mutableStateOf("")
    var username by mutableStateOf("")
    var password by mutableStateOf("")

    var isLoading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)

    private val credentialsManager: CredentialsManager

    init {
        SessionManager.clear()
        credentialsManager = (application as MainApplication).credentialsManager
        serverUrl = credentialsManager.getServerUrl() ?: ""
        username = credentialsManager.getUsername() ?: ""
        password = credentialsManager.getPassword() ?: ""
    }

    fun onLoginClick(onLoginSuccess: () -> Unit) {
        error = null
        if (serverUrl.isBlank() || username.isBlank() || password.isBlank()) {
            error = "Please fill in all fields"
            return
        }
        val finalUrl = if (serverUrl.startsWith("http://") || serverUrl.startsWith("https://")) {
            serverUrl
        } else {
            "http://$serverUrl"
        }
        authenticateXtream(finalUrl, username, password, onLoginSuccess)
    }

    fun authenticateXtream(server: String, user: String, pass: String, onLoginSuccess: () -> Unit) {
        viewModelScope.launch {
            isLoading = true
            try {
                SessionManager.initialize(server, username, password)
                val apiService = SessionManager.getApiService()
                val response = apiService.authenticate(user, pass)
                val json = Json { ignoreUnknownKeys = true }

                if (response is JsonObject && "user_info" in response) {
                    val userInfoJson = response["user_info"]
                    if (userInfoJson is JsonObject) {
                        val authUserInfo = json.decodeFromJsonElement(AuthUserInfo.serializer(), userInfoJson)

                        if (authUserInfo.auth == 1) {
                            credentialsManager.saveCredentials(server, user, pass)
                            SessionManager.serverUrl = server
                            SessionManager.username = user
                            SessionManager.password = pass
                            SessionManager.userInfo = authUserInfo
                            onLoginSuccess()
                        } else {
                            error = "Invalid credentials or inactive user."
                            SessionManager.clear()
                            credentialsManager.clearCredentials()
                        }
                    } else {
                        error = "Login failed: Malformed user info in server response."
                        SessionManager.clear()
                        credentialsManager.clearCredentials()
                    }
                } else {
                    error = "Login failed: Unexpected response from server."
                    SessionManager.clear()
                    credentialsManager.clearCredentials()
                }
            } catch (e: Exception) {
                error = "Login failed: ${e.message}"
                SessionManager.clear()
            } finally {
                isLoading = false
            }
        }
    }
}
