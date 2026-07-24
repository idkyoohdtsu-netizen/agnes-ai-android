package com.agnesai.android.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.agnesai.android.data.local.PreferencesManager
import com.agnesai.android.data.model.DEFAULT_MODELS
import com.agnesai.android.data.model.GitHubConfig
import com.agnesai.android.data.model.TokenConfig
import com.agnesai.android.data.repository.impl.AiRepositoryImpl
import com.agnesai.android.data.repository.impl.GitHubRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val apiKey: String = "",
    val baseUrl: String = "https://apihub.agnes-ai.com/v1",
    val selectedModel: String = "gpt-4o",
    val availableModels: List<String> = DEFAULT_MODELS,
    val githubToken: String = "",
    val githubUsername: String = "",
    val githubRepo: String = "",
    val isSaving: Boolean = false,
    val isTesting: Boolean = false,
    val saveSuccess: Boolean = false,
    val testSuccess: String? = null,
    val error: String? = null,
    val showApiKey: Boolean = false,
    val showGithubToken: Boolean = false
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesManager = PreferencesManager(application)
    private val aiRepository = AiRepositoryImpl()
    private val githubRepository = GitHubRepositoryImpl()

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

    fun testAiConnection() {
        val state = _uiState.value
        if (state.apiKey.isBlank()) {
            _uiState.update { it.copy(error = "Nhập API key trước khi test") }
            return
        }
        _uiState.update { it.copy(isTesting = true, error = null, testSuccess = null) }

        viewModelScope.launch {
            val config = TokenConfig(
                apiKey = state.apiKey,
                baseUrl = state.baseUrl,
                model = state.selectedModel
            )
            aiRepository.getAvailableModels(config)
                .onSuccess { models ->
                    val modelList = if (models.isNotEmpty()) models else DEFAULT_MODELS
                    _uiState.update {
                        it.copy(
                            isTesting = false,
                            availableModels = modelList,
                            testSuccess = "✅ Kết nối AI thành công! Tìm thấy ${models.size} models."
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isTesting = false,
                            error = "❌ Test thất bại: ${e.message}"
                        )
                    }
                }
        }
    }

    fun testGitHubConnection() {
        val state = _uiState.value
        if (state.githubToken.isBlank() || state.githubUsername.isBlank() || state.githubRepo.isBlank()) {
            _uiState.update { it.copy(error = "Nhập đầy đủ GitHub token, username và repo trước khi test") }
            return
        }
        _uiState.update { it.copy(isTesting = true, error = null, testSuccess = null) }

        viewModelScope.launch {
            val config = GitHubConfig(
                token = state.githubToken,
                username = state.githubUsername,
                repo = state.githubRepo
            )
            githubRepository.listFiles(config, "")
                .onSuccess { files ->
                    _uiState.update {
                        it.copy(
                            isTesting = false,
                            testSuccess = "✅ Kết nối GitHub thành công! Repo có ${files.size} items ở thư mục gốc."
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isTesting = false,
                            error = "❌ GitHub test thất bại: ${e.message}"
                        )
                    }
                }
        }
    }

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
                    it.copy(isSaving = false, error = "Lưu thất bại: ${e.message}")
                }
            }
        }
    }

    fun clearSaveSuccess() = _uiState.update { it.copy(saveSuccess = false) }
    fun clearTestSuccess() = _uiState.update { it.copy(testSuccess = null) }
    fun clearError() = _uiState.update { it.copy(error = null) }
}
