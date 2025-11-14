package com.example.materialtv.network

import com.example.materialtv.model.AuthUserInfo

object SessionManager {
    // Xtream Specific
    var serverUrl: String? = null
    var username: String? = null
    var password: String? = null
    var userInfo: AuthUserInfo? = null

    private var apiService: XtreamApiService? = null

    fun initialize(baseUrl: String, user: String, pass: String) {
        val correctedBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        serverUrl = correctedBaseUrl
        username = user
        password = pass
        apiService = RetrofitClient.getClient(correctedBaseUrl)
    }

    fun getApiService(): XtreamApiService {
        return apiService ?: throw IllegalStateException("SessionManager not initialized. Call initialize() first.")
    }

    fun isInitialized(): Boolean {
        return apiService != null
    }

    fun clear() {
        serverUrl = null
        username = null
        password = null
        userInfo = null
        apiService = null
    }
}
