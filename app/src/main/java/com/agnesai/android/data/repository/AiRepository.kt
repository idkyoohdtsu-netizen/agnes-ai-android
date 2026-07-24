package com.agnesai.android.data.repository

import com.agnesai.android.data.model.Message
import com.agnesai.android.data.model.TokenConfig
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for AI chat operations.
 * Implementation will be provided by Agent 2.
 */
interface AiRepository {
    /**
     * Send a message to the AI and receive a streaming response.
     * @param messages List of conversation messages (history + new message)
     * @param config Token configuration with API key and model settings
     * @return Flow of response text chunks for streaming display
     */
    suspend fun sendMessage(
        messages: List<Message>,
        config: TokenConfig
    ): Flow<String>

    /**
     * Send a message with file attachments.
     * @param messages Conversation history
     * @param config Token configuration
     * @param attachmentPaths Local file paths to attach
     * @return Flow of response text chunks
     */
    suspend fun sendMessageWithAttachments(
        messages: List<Message>,
        config: TokenConfig,
        attachmentPaths: List<String>
    ): Flow<String>

    /**
     * Get available models from the configured endpoint.
     * @param config Token configuration
     * @return List of available model IDs
     */
    suspend fun getAvailableModels(config: TokenConfig): Result<List<String>>
}
