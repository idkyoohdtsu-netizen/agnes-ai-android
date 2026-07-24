package com.agnesai.android.ui.screens.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.agnesai.android.data.local.PreferencesManager
import com.agnesai.android.data.model.Message
import com.agnesai.android.data.model.MessageRole
import com.agnesai.android.data.model.TokenConfig
import kotlinx.coroutines.delay
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
            content = "Hello! I'm Agnes, your AI assistant. How can I help you today?",
            role = MessageRole.ASSISTANT
        )
    ),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val pendingAttachments: List<String> = emptyList(),
    val error: String? = null
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesManager = PreferencesManager(application)

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    val tokenConfig: StateFlow<TokenConfig> = preferencesManager.tokenConfigFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = TokenConfig()
        )

    fun onInputTextChange(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isEmpty() && _uiState.value.pendingAttachments.isEmpty()) return
        if (_uiState.value.isLoading) return

        val userMessage = Message(
            content = text,
            role = MessageRole.USER,
            attachments = _uiState.value.pendingAttachments.toList()
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

        viewModelScope.launch {
            try {
                // Placeholder AI response - Agent 2 will replace with real API call
                delay(1500)
                val responseText = generatePlaceholderResponse(text)
                val aiMessage = Message(
                    content = responseText,
                    role = MessageRole.ASSISTANT
                )
                _uiState.update { state ->
                    state.copy(
                        messages = state.messages + aiMessage,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        error = "Failed to get response: ${e.message}"
                    )
                }
            }
        }
    }

    fun attachFile(filePath: String) {
        _uiState.update { state ->
            state.copy(
                pendingAttachments = state.pendingAttachments + filePath
            )
        }
    }

    fun removeAttachment(filePath: String) {
        _uiState.update { state ->
            state.copy(
                pendingAttachments = state.pendingAttachments - filePath
            )
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
                        content = "Conversation cleared. How can I help you?",
                        role = MessageRole.ASSISTANT
                    )
                )
            )
        }
    }

    private fun generatePlaceholderResponse(input: String): String {
        return when {
            input.contains("hello", ignoreCase = true) || input.contains("hi", ignoreCase = true) ->
                "Hello! It's great to meet you. I'm Agnes, your AI assistant. I'm ready to help you with questions, coding, analysis, creative writing, and much more. What would you like to explore today?"
            input.contains("help", ignoreCase = true) ->
                "I'm here to help! I can assist you with:\n\n• **Coding** - Write, review, and debug code\n• **Analysis** - Analyze data and documents\n• **Writing** - Draft emails, reports, and creative content\n• **Research** - Answer questions and explain concepts\n• **Files** - Manage and process your files\n\nWhat do you need help with?"
            input.contains("code", ignoreCase = true) || input.contains("kotlin", ignoreCase = true) ->
                "I'd love to help with coding! I'm proficient in Kotlin, Java, Python, JavaScript, and many other languages. Please share the code you're working on or describe what you'd like to build."
            input.contains("file", ignoreCase = true) ->
                "I can help you manage files! You can attach files to our conversation using the attachment button below, or navigate to the **Files** tab to browse and manage files stored in your GitHub repository."
            else ->
                "I understand you're asking about \"$input\". I'm currently in placeholder mode — configure your API key in **Settings** to enable full AI capabilities. Once configured, I'll provide intelligent, contextual responses using your preferred AI model."
        }
    }
}
