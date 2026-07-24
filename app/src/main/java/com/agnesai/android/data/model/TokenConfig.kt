package com.agnesai.android.data.model

data class TokenConfig(
    val apiKey: String = "",
    val baseUrl: String = "https://apihub.agnes-ai.com/v1",
    val model: String = "gpt-4o",
    val maxTokens: Int = 4096,
    val temperature: Float = 0.7f
) {
    val isConfigured: Boolean
        get() = apiKey.isNotBlank() && baseUrl.isNotBlank() && model.isNotBlank()
}

val DEFAULT_MODELS = listOf(
    "gpt-4o",
    "gpt-4o-mini",
    "gpt-4-turbo",
    "gpt-3.5-turbo",
    "claude-3-5-sonnet",
    "claude-3-haiku",
    "gemini-1.5-pro",
    "gemini-1.5-flash",
    "llama-3.1-70b",
    "mixtral-8x7b"
)
