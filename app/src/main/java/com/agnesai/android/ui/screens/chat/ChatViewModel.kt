package com.agnesai.android.ui.screens.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.agnesai.android.data.local.PreferencesManager
import com.agnesai.android.data.model.GitHubConfig
import com.agnesai.android.data.model.Message
import com.agnesai.android.data.model.MessageRole
import com.agnesai.android.data.model.TokenConfig
import com.agnesai.android.data.repository.AiRepository
import com.agnesai.android.data.repository.GitHubRepository
import com.agnesai.android.data.repository.impl.AiRepositoryImpl
import com.agnesai.android.data.repository.impl.GitHubRepositoryImpl
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class ChatUiState(
    val messages: List<Message> = listOf(
        Message(
            content = "Xin chào! Tôi là Agnes, trợ lý AI của bạn. Hãy cấu hình API key trong Cài đặt để bắt đầu, hoặc hỏi tôi bất cứ điều gì!",
            role = MessageRole.ASSISTANT
        )
    ),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val isStreaming: Boolean = false,
    val pendingAttachments: List<String> = emptyList(),
    val error: String? = null,
    val conversationId: String = UUID.randomUUID().toString(),
    val memorySaved: Boolean = false
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesManager = PreferencesManager(application)
    private val aiRepository: AiRepository = AiRepositoryImpl()
    private val githubRepository: GitHubRepository = GitHubRepositoryImpl()
    private val gson = Gson()

    private var streamingJob: Job? = null

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    val tokenConfig: StateFlow<TokenConfig> = preferencesManager.tokenConfigFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = TokenConfig()
        )

    val githubConfig: StateFlow<GitHubConfig> = preferencesManager.gitHubConfigFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = GitHubConfig()
        )

    fun onInputTextChange(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isEmpty() && _uiState.value.pendingAttachments.isEmpty()) return
        if (_uiState.value.isLoading || _uiState.value.isStreaming) return

        val config = tokenConfig.value
        val attachments = _uiState.value.pendingAttachments.toList()

        val userMessage = Message(
            content = text,
            role = MessageRole.USER,
            attachments = attachments
        )

        _uiState.update { state ->
            state.copy(
                messages = state.messages + userMessage,
                inputText = "",
                pendingAttachments = emptyList(),
                isLoading = true,
                error = null
            )
        }

        if (!config.isConfigured) {
            _uiState.update { state ->
                state.copy(
                    messages = state.messages + Message(
                        content = "⚠️ Chưa cấu hình API key. Vui lòng vào **Cài đặt** và nhập API key để sử dụng AI.",
                        role = MessageRole.ASSISTANT
                    ),
                    isLoading = false
                )
            }
            return
        }

        streamingJob = viewModelScope.launch {
            // Add empty assistant message placeholder for streaming
            val assistantMessageId = UUID.randomUUID().toString()
            _uiState.update { state ->
                state.copy(
                    messages = state.messages + Message(
                        id = assistantMessageId,
                        content = "",
                        role = MessageRole.ASSISTANT
                    ),
                    isLoading = false,
                    isStreaming = true
                )
            }

            try {
                val conversationMessages = _uiState.value.messages
                    .dropLast(1) // exclude empty streaming placeholder
                    .takeLast(20) // keep last 20 messages for context

                val flow = if (attachments.isNotEmpty()) {
                    aiRepository.sendMessageWithAttachments(conversationMessages, config, attachments)
                } else {
                    aiRepository.sendMessage(conversationMessages, config)
                }

                flow.collect { chunk ->
                    _uiState.update { state ->
                        val updatedMessages = state.messages.map { msg ->
                            if (msg.id == assistantMessageId) {
                                msg.copy(content = msg.content + chunk)
                            } else msg
                        }
                        state.copy(messages = updatedMessages)
                    }
                }

                _uiState.update { it.copy(isStreaming = false) }

                // Auto-save memory to GitHub if configured
                val ghConfig = githubConfig.value
                if (ghConfig.isConfigured) {
                    saveMemoryToGitHub(ghConfig)
                }

            } catch (e: Exception) {
                _uiState.update { state ->
                    val updatedMessages = state.messages.map { msg ->
                        if (msg.id == assistantMessageId) {
                            msg.copy(
                                content = "❌ Lỗi: ${e.message}",
                                isError = true
                            )
                        } else msg
                    }
                    state.copy(
                        messages = updatedMessages,
                        isStreaming = false,
                        isLoading = false,
                        error = e.message
                    )
                }
            }
        }
    }

    fun stopStreaming() {
        streamingJob?.cancel()
        _uiState.update { it.copy(isStreaming = false, isLoading = false) }
    }

    fun attachFile(filePath: String) {
        _uiState.update { state ->
            state.copy(pendingAttachments = state.pendingAttachments + filePath)
        }
    }

    fun removeAttachment(filePath: String) {
        _uiState.update { state ->
            state.copy(pendingAttachments = state.pendingAttachments - filePath)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearConversation() {
        _uiState.update {
            ChatUiState(
                messages = listOf(
                    Message(
                        content = "Cuộc trò chuyện đã được xóa. Tôi có thể giúp gì cho bạn?",
                        role = MessageRole.ASSISTANT
                    )
                )
            )
        }
    }

    fun loadMemoryFromGitHub() {
        val ghConfig = githubConfig.value
        if (!ghConfig.isConfigured) return

        viewModelScope.launch {
            val conversationId = _uiState.value.conversationId
            githubRepository.loadMemory(ghConfig, conversationId).onSuccess { json ->
                try {
                    val type = object : TypeToken<List<Message>>() {}.type
                    val messages: List<Message> = gson.fromJson(json, type)
                    if (messages.isNotEmpty()) {
                        _uiState.update { it.copy(messages = messages) }
                    }
                } catch (_: Exception) { }
            }
        }
    }

    private fun saveMemoryToGitHub(ghConfig: GitHubConfig) {
        viewModelScope.launch {
            try {
                val messages = _uiState.value.messages
                val json = gson.toJson(messages)
                val conversationId = _uiState.value.conversationId
                githubRepository.saveMemory(ghConfig, conversationId, json).onSuccess {
                    _uiState.update { it.copy(memorySaved = true) }
                }
            } catch (_: Exception) { }
        }
    }
}
