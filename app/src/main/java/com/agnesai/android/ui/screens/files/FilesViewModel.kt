package com.agnesai.android.ui.screens.files

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.agnesai.android.data.local.PreferencesManager
import com.agnesai.android.data.model.FileItem
import com.agnesai.android.data.model.GitHubConfig
import com.agnesai.android.data.repository.GitHubRepository
import com.agnesai.android.data.repository.impl.GitHubRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class FilesUiState(
    val files: List<FileItem> = emptyList(),
    val currentPath: String = "",
    val isLoading: Boolean = false,
    val isUploading: Boolean = false,
    val selectedFile: FileItem? = null,
    val error: String? = null,
    val successMessage: String? = null,
    val fileContent: ByteArray? = null,
    val breadcrumbs: List<String> = emptyList()
)

class FilesViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesManager = PreferencesManager(application)
    private val githubRepository: GitHubRepository = GitHubRepositoryImpl()

    private val _uiState = MutableStateFlow(FilesUiState())
    val uiState: StateFlow<FilesUiState> = _uiState.asStateFlow()

    val githubConfig: StateFlow<GitHubConfig> = preferencesManager.gitHubConfigFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = GitHubConfig()
        )

    init {
        // Observe config changes and reload files
        viewModelScope.launch {
            githubConfig.collect { config ->
                if (config.isConfigured) {
                    loadFiles("")
                } else {
                    _uiState.update { it.copy(files = emptyList(), currentPath = "") }
                }
            }
        }
    }

    fun loadFiles(path: String = _uiState.value.currentPath) {
        val config = githubConfig.value
        if (!config.isConfigured) {
            _uiState.update { it.copy(error = "Chưa cấu hình GitHub. Vào Cài đặt để thiết lập.") }
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            githubRepository.listFiles(config, path)
                .onSuccess { files ->
                    val breadcrumbs = if (path.isBlank()) emptyList()
                    else path.split("/").filter { it.isNotBlank() }

                    _uiState.update {
                        it.copy(
                            files = files,
                            currentPath = path,
                            isLoading = false,
                            breadcrumbs = breadcrumbs
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Không tải được files: ${error.message}"
                        )
                    }
                }
        }
    }

    fun navigateTo(file: FileItem) {
        if (file.type == "dir") {
            loadFiles(file.path)
        } else {
            _uiState.update { it.copy(selectedFile = file) }
        }
    }

    fun navigateUp() {
        val currentPath = _uiState.value.currentPath
        val parentPath = if (currentPath.contains("/")) {
            currentPath.substringBeforeLast("/")
        } else {
            ""
        }
        loadFiles(parentPath)
    }

    fun navigateToBreadcrumb(index: Int) {
        val breadcrumbs = _uiState.value.breadcrumbs
        val path = breadcrumbs.take(index + 1).joinToString("/")
        loadFiles(path)
    }

    fun uploadFile(uri: Uri, displayName: String) {
        val config = githubConfig.value
        if (!config.isConfigured) {
            _uiState.update { it.copy(error = "Chưa cấu hình GitHub.") }
            return
        }

        _uiState.update { it.copy(isUploading = true, error = null) }

        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: throw Exception("Không thể đọc file")

                val bytes = inputStream.readBytes()
                inputStream.close()

                val currentPath = _uiState.value.currentPath
                val targetPath = if (currentPath.isBlank()) displayName
                else "$currentPath/$displayName"

                githubRepository.uploadFile(
                    config,
                    targetPath,
                    bytes,
                    "Upload $displayName via Agnes AI"
                ).onSuccess {
                    _uiState.update {
                        it.copy(
                            isUploading = false,
                            successMessage = "✅ Đã upload $displayName thành công!"
                        )
                    }
                    loadFiles()
                }.onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isUploading = false,
                            error = "Upload thất bại: ${error.message}"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isUploading = false,
                        error = "Lỗi: ${e.message}"
                    )
                }
            }
        }
    }

    fun uploadTextFile(fileName: String, content: String) {
        val config = githubConfig.value
        if (!config.isConfigured) {
            _uiState.update { it.copy(error = "Chưa cấu hình GitHub.") }
            return
        }

        _uiState.update { it.copy(isUploading = true, error = null) }

        viewModelScope.launch {
            val currentPath = _uiState.value.currentPath
            val targetPath = if (currentPath.isBlank()) fileName else "$currentPath/$fileName"

            githubRepository.uploadFile(
                config,
                targetPath,
                content.toByteArray(Charsets.UTF_8),
                "Create $fileName via Agnes AI"
            ).onSuccess {
                _uiState.update {
                    it.copy(
                        isUploading = false,
                        successMessage = "✅ Đã tạo $fileName thành công!"
                    )
                }
                loadFiles()
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isUploading = false,
                        error = "Tạo file thất bại: ${error.message}"
                    )
                }
            }
        }
    }

    fun downloadFile(file: FileItem) {
        val config = githubConfig.value
        if (!config.isConfigured) return

        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            githubRepository.downloadFile(config, file.path)
                .onSuccess { bytes ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            fileContent = bytes,
                            selectedFile = file
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Download thất bại: ${error.message}"
                        )
                    }
                }
        }
    }

    fun deleteFile(file: FileItem) {
        val config = githubConfig.value
        if (!config.isConfigured) return
        if (file.sha.isBlank()) {
            _uiState.update { it.copy(error = "Không có SHA để xóa file.") }
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            githubRepository.deleteFile(config, file.path, file.sha)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            selectedFile = null,
                            successMessage = "✅ Đã xóa ${file.name}"
                        )
                    }
                    loadFiles()
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Xóa thất bại: ${error.message}"
                        )
                    }
                }
        }
    }

    fun selectFile(file: FileItem) {
        _uiState.update { it.copy(selectedFile = file) }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedFile = null, fileContent = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearSuccess() {
        _uiState.update { it.copy(successMessage = null) }
    }

    fun refresh() {
        loadFiles(_uiState.value.currentPath)
    }
}
