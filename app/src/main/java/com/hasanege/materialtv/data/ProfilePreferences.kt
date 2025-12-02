package com.hasanege.materialtv.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

val Context.profileDataStore: DataStore<Preferences> by preferencesDataStore(name = "profile_preferences")

class ProfilePreferences(private val context: Context) {
    companion object {
        val PROFILE_NAME = stringPreferencesKey("profile_name")
        val PROFILE_IMAGE_URL = stringPreferencesKey("profile_image_url")
    }

    val profileName: Flow<String> = context.profileDataStore.data.map { preferences ->
        preferences[PROFILE_NAME] ?: "User"
    }

    val profileImageUrl: Flow<String> = context.profileDataStore.data.map { preferences ->
        val savedPath = preferences[PROFILE_IMAGE_URL]
        if (!savedPath.isNullOrEmpty()) {
            savedPath
        } else {
            val file = java.io.File(context.filesDir, "pfp.png")
            if (file.exists()) file.absolutePath else ""
        }
    }.flowOn(Dispatchers.IO)

    suspend fun setProfileName(name: String) {
        context.profileDataStore.edit { preferences ->
            preferences[PROFILE_NAME] = name
        }
    }

    suspend fun setProfileImageFromUri(uriString: String) {
        withContext(Dispatchers.IO) {
            try {
                val uri = android.net.Uri.parse(uriString)
                val inputStream = context.contentResolver.openInputStream(uri)
                val file = java.io.File(context.filesDir, "pfp.png")
                
                inputStream?.use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                
                context.profileDataStore.edit { preferences ->
                    preferences[PROFILE_IMAGE_URL] = file.absolutePath
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun clearProfile() {
        context.profileDataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
