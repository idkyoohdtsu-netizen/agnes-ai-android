package com.agnesai.android.data.model

data class GitHubConfig(
    val token: String = "",
    val username: String = "",
    val repo: String = "",
    val branch: String = "main",
    val memoryPath: String = "agnes-memory"
) {
    val isConfigured: Boolean
        get() = token.isNotBlank() && username.isNotBlank() && repo.isNotBlank()

    val repoUrl: String
        get() = "https://github.com/$username/$repo"

    val apiBaseUrl: String
        get() = "https://api.github.com/repos/$username/$repo"
}
