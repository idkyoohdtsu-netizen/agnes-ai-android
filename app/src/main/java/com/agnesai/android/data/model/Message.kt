package com.agnesai.android.data.model

import java.util.UUID

enum class MessageRole {
    USER,
    ASSISTANT
}

data class Message(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val role: MessageRole,
    val timestamp: Long = System.currentTimeMillis(),
    val attachments: List<String> = emptyList(),
    val isError: Boolean = false
)
