package com.agnesai.android.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.agnesai.android.data.model.GitHubConfig
import com.agnesai.android.data.model.TokenConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "agnes_preferences")

class PreferencesManager(private val context: Context) {

    companion object {
        private val API_KEY = stringPreferencesKey("api_key")
        private val BASE_URL = stringPreferencesKey("base_url")
        private val MODEL = stringPreferencesKey("model")
        private val MAX_TOKENS = intPreferencesKey("max_tokens")
        private val TEMPERATURE = floatPreferencesKey("temperature")

        private val GITHUB_TOKEN = stringPreferencesKey("github_token")
        private val GITHUB_USERNAME = stringPreferencesKey("github_username")
        private val GITHUB_REPO = stringPreferencesKey("github_repo")
        private val GITHUB_BRANCH = stringPreferencesKey("github_branch")
        private val GITHUB_MEMORY_PATH = stringPreferencesKey("github_memory_path")
    }

    val tokenConfigFlow: Flow<TokenConfig> = context.dataStore.data.map { prefs ->
        TokenConfig(
            apiKey = prefs[API_KEY] ?: "",
            baseUrl = prefs[BASE_URL] ?: "https://apihub.agnes-ai.com/v1",
            model = prefs[MODEL] ?: "gpt-4o",
            maxTokens = prefs[MAX_TOKENS] ?: 4096,
            temperature = prefs[TEMPERATURE] ?: 0.7f
        )
    }

    val gitHubConfigFlow: Flow<GitHubConfig> = context.dataStore.data.map { prefs ->
        GitHubConfig(
            token = prefs[GITHUB_TOKEN] ?: "",
            username = prefs[GITHUB_USERNAME] ?: "",
            repo = prefs[GITHUB_REPO] ?: "",
            branch = prefs[GITHUB_BRANCH] ?: "main",
            memoryPath = prefs[GITHUB_MEMORY_PATH] ?: "agnes-memory"
        )
    }

    suspend fun saveTokenConfig(config: TokenConfig) {
        context.dataStore.edit { prefs ->
            prefs[API_KEY] = config.apiKey
            prefs[BASE_URL] = config.baseUrl
            prefs[MODEL] = config.model
            prefs[MAX_TOKENS] = config.maxTokens
            prefs[TEMPERATURE] = config.temperature
        }
    }

    suspend fun saveGitHubConfig(config: GitHubConfig) {
        context.dataStore.edit { prefs ->
            prefs[GITHUB_TOKEN] = config.token
            prefs[GITHUB_USERNAME] = config.username
            prefs[GITHUB_REPO] = config.repo
            prefs[GITHUB_BRANCH] = config.branch
            prefs[GITHUB_MEMORY_PATH] = config.memoryPath
        }
    }

    suspend fun clearAllPreferences() {
        context.dataStore.edit { prefs ->
            prefs.clear()
        }
    }
}
