package com.agnesai.android.ui.screens.files

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.agnesai.android.data.local.PreferencesManager
import com.agnesai.android.data.model.FileItem
import com.agnesai.android.data.model.GitHubConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FilesUiState(
    val files: List<FileItem> = emptyList(),
    val isLoading: Boolean = false,
    val isUploading: Boolean = false,
    val error: String? = null,
    val currentPath: String = "",
    val selectedFile: FileItem? = null
)

class FilesViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesManager = PreferencesManager(application)

    private val _uiState = MutableStateFlow(FilesUiState())
    val uiState: StateFlow<FilesUiState> = _uiState.asStateFlow()

    val gitHubConfig: StateFlow<GitHubConfig> = preferencesManager.gitHubConfigFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = GitHubConfig()
        )

    init {
        loadFiles()
    }

    fun loadFiles(path: String = "") {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                // Placeholder - Agent 2 will implement real GitHub API calls
                delay(800)
                val placeholderFiles = listOf(
                    FileItem(
                        name = "conversation_history.json",
                        path = "agnes-memory/conversation_history.json",
                        size = 4096,
                        sha = "abc123"
                    ),
                    FileItem(
                        name = "notes.md",
                        path = "agnes-memory/notes.md",
                        size = 1024,
                        sha = "def456"
                    ),
                    FileItem(
                        name = "project_context.txt",
                        path = "agnes-memory/project_context.txt",
                        size = 2048,
                        sha = "ghi789"
                    ),
                    FileItem(
                        name = "code_snippet.kt",
                        path = "agnes-memory/code_snippet.kt",
                        size = 512,
                        sha = "jkl012"
                    )
                )
                _uiState.update { it.copy(files = placeholderFiles, isLoading = false, currentPath = path) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to load files: ${e.message}"
                    )
                }
            }
        }
    }

    fun uploadFile(localPath: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUploading = true) }
            try {
                delay(1000)
                // Placeholder upload - Agent 2 will implement
                val newFile = FileItem(
                    name = localPath.substringAfterLast('/'),
                    path = "agnes-memory/${localPath.substringAfterLast('/')}",
                    size = 1024,
                    sha = "new_sha_${System.currentTimeMillis()}"
                )
                _uiState.update { state ->
                    state.copy(
                        files = state.files + newFile,
                        isUploading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isUploading = false,
                        error = "Failed to upload: ${e.message}"
                    )
                }
            }
        }
    }

    fun deleteFile(file: FileItem) {
        viewModelScope.launch {
            try {
                // Placeholder - Agent 2 will implement
                _uiState.update { state ->
                    state.copy(files = state.files.filter { it.sha != file.sha })
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to delete: ${e.message}") }
            }
        }
    }

    fun selectFile(file: FileItem) {
        _uiState.update { it.copy(selectedFile = file) }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedFile = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
