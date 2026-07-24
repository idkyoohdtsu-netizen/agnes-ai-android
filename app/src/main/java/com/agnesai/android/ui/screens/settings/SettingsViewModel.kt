package com.agnesai.android.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.agnesai.android.data.local.PreferencesManager
import com.agnesai.android.data.model.DEFAULT_MODELS
import com.agnesai.android.data.model.GitHubConfig
import com.agnesai.android.data.model.TokenConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    // AI config
    val apiKey: String = "",
    val baseUrl: String = "https://apihub.agnes-ai.com/v1",
    val selectedModel: String = "gpt-4o",
    val availableModels: List<String> = DEFAULT_MODELS,

    // GitHub config
    val githubToken: String = "",
    val githubUsername: String = "",
    val githubRepo: String = "",

    // UI state
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: String? = null,

    // Visibility
    val showApiKey: Boolean = false,
    val showGithubToken: Boolean = false
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesManager = PreferencesManager(application)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val tokenConfig = preferencesManager.tokenConfigFlow.first()
            val githubConfig = preferencesManager.gitHubConfigFlow.first()
            _uiState.update { state ->
                state.copy(
                    apiKey = tokenConfig.apiKey,
                    baseUrl = tokenConfig.baseUrl,
                    selectedModel = tokenConfig.model,
                    githubToken = githubConfig.token,
                    githubUsername = githubConfig.username,
                    githubRepo = githubConfig.repo
                )
            }
        }
    }

    fun onApiKeyChange(value: String) = _uiState.update { it.copy(apiKey = value) }
    fun onBaseUrlChange(value: String) = _uiState.update { it.copy(baseUrl = value) }
    fun onModelSelect(model: String) = _uiState.update { it.copy(selectedModel = model) }
    fun onGithubTokenChange(value: String) = _uiState.update { it.copy(githubToken = value) }
    fun onGithubUsernameChange(value: String) = _uiState.update { it.copy(githubUsername = value) }
    fun onGithubRepoChange(value: String) = _uiState.update { it.copy(githubRepo = value) }
    fun toggleApiKeyVisibility() = _uiState.update { it.copy(showApiKey = !it.showApiKey) }
    fun toggleGithubTokenVisibility() = _uiState.update { it.copy(showGithubToken = !it.showGithubToken) }

    fun saveSettings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            try {
                val state = _uiState.value
                preferencesManager.saveTokenConfig(
                    TokenConfig(
                        apiKey = state.apiKey,
                        baseUrl = state.baseUrl,
                        model = state.selectedModel
                    )
                )
                preferencesManager.saveGitHubConfig(
                    GitHubConfig(
                        token = state.githubToken,
                        username = state.githubUsername,
                        repo = state.githubRepo
                    )
                )
                _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        error = "Failed to save settings: ${e.message}"
                    )
                }
            }
        }
    }

    fun clearSaveSuccess() {
        _uiState.update { it.copy(saveSuccess = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
